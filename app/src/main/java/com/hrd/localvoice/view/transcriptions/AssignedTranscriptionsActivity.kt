package com.hrd.localvoice.view.transcriptions

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.adapters.TranscriptionAudioAdapter
import com.hrd.localvoice.databinding.ActivityAssingedTranscriptionsBinding
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.workers.TranscriptionUploadWorker
import com.hrd.localvoice.workers.UpdateAssignedTranscriptionAudiosWorker
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AssignedTranscriptionsActivity : AppCompatActivity() {
    lateinit var binding: ActivityAssingedTranscriptionsBinding
    private lateinit var viewModel: TranscriptionActivityViewModel
    private lateinit var adapter: TranscriptionAudioAdapter
    private var audios: List<TranscriptionAudio>? = null

    // Background work manager
    private val constraints = Constraints.Builder().apply {
        setRequiredNetworkType(NetworkType.CONNECTED)
    }.build()
    private val workManager = WorkManager.getInstance(application)
    private val transcriptionUploadWorker =
        PeriodicWorkRequest.Builder(TranscriptionUploadWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssingedTranscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[TranscriptionActivityViewModel::class.java]
        title = "Transcriptions"

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        deleteExpiredAudios()

        adapter = TranscriptionAudioAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadAudios()

        // Swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            loadAudios()
        }

        // Attach listener to update local images button
        binding.buttonGetAssignedAudios.setOnClickListener {
            scheduleTranscriptionDownloadWorker(constraints, workManager)
        }

        binding.statsCards.setOnClickListener {
            showStatsExplanation()
        }

        binding.startButton.setOnClickListener {
            startActivity(Intent(this, TranscriptionActivity::class.java))
        }

        // Schedule auto transcription upload.
        workManager.enqueueUniquePeriodicWork(
            "TranscriptionUploadWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            transcriptionUploadWorker
        )
    }

    private fun scheduleTranscriptionDownloadWorker(
        constraints: Constraints,
        workManager: WorkManager
    ) {
        val updateAssignedTranscriptionAudiosWorker = PeriodicWorkRequest.Builder(
            UpdateAssignedTranscriptionAudiosWorker::class.java, 12, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            "UpdateAssignedTranscriptionAudiosWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateAssignedTranscriptionAudiosWorker
        )
        Toast.makeText(
            this, "Requested new audios for transcription.", Toast.LENGTH_LONG
        ).show()
    }

    private fun loadAudios() {
        viewModel.getTranscriptionAudios()?.observe(this) {
            audios = it
            adapter.setData(it)
            binding.swiperefresh.isRefreshing = false
            binding.pendingCountLabel.text = "${audios?.size} Pending"
            binding.startButton.isEnabled = it.isNotEmpty()

            AppRoomDatabase.databaseWriteExecutor.execute {
                val hoursToKeepAudiosForTranscription = AppRoomDatabase.INSTANCE?.ConfigurationDao()
                    ?.getConfiguration()?.hoursToKeepAudiosForTranscription
                hoursToKeepAudiosForTranscription?.let { hours ->
                    if (audios.isNullOrEmpty()) return@execute

                    val lastAudio = audios?.let { aus -> aus[aus.size - 1].createdAt }
                    lastAudio?.let {
                        val timeLeft =
                            max(
                                (-System.currentTimeMillis() + hours * 3600 * 1000 + lastAudio) / (3600 * 1000),
                                0
                            )
                        runOnUiThread {
                            binding.timeLeftLabel.text = "${timeLeft}hrs left"
                        }
                    }
                }
            }
            if (it.isEmpty()) {
                binding.emptyContainer.visibility = View.VISIBLE
            } else {
                binding.emptyContainer.visibility = View.GONE
            }
        }
    }

    private fun deleteExpiredAudios() {
        AppRoomDatabase.databaseWriteExecutor.execute {
            val hoursToKeepAudiosForTranscription = AppRoomDatabase.INSTANCE?.ConfigurationDao()
                ?.getConfiguration()?.hoursToKeepAudiosForTranscription
            hoursToKeepAudiosForTranscription?.let { hours ->
                val total = System.currentTimeMillis() - (hours * 3600 * 1000)
                val audios =
                    AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()?.getExpiredAudios(total)
                audios?.forEach { audio ->
                    if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
                        File(it1).delete()
                    }
                }
                if (audios != null) {
                    AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()?.delete(audios)
                }
            }
        }
    }

    private fun showStatsExplanation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("INFO")
        builder.setMessage(getString(R.string.transcription_audios_availability))
            .setPositiveButton("OK") { _, _ ->
            }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.transcription_audios_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_get_new_audios -> {
                scheduleTranscriptionDownloadWorker(constraints, workManager)
            }
            R.id.action_clear_all_pending -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("DELETE PENDING AUDIOS")
                builder.setMessage("Are you sure you want to delete all audios awaiting your transcription?")
                    .setNegativeButton("CANCEL") { _, _ -> }.setPositiveButton("YES") { _, _ ->
                        AppRoomDatabase.databaseWriteExecutor.execute {
                            val pendingAudios = AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()
                                ?.getSyncPendingAudioTranscriptions()
                            pendingAudios?.forEach { audio ->
                                if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
                                    File(it1).delete()
                                }
                            }
                            if (pendingAudios?.isNotEmpty() == true) {
                                AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()
                                    ?.delete(pendingAudios)
                            }
                        }
                    }.show()
            }
            R.id.action_upload_transcriptions -> {
                workManager.enqueueUniquePeriodicWork(
                    "TranscriptionUploadWorker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    transcriptionUploadWorker
                )
                Toast.makeText(this, "Scheduled upload for transcribed audios.", Toast.LENGTH_SHORT)
                    .show()
            }
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}