package com.hrd.localvoice.view.validations

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
import com.hrd.localvoice.adapters.ValidationAudioAdapter
import com.hrd.localvoice.databinding.ActivityAssignedAudiosBinding
import com.hrd.localvoice.models.ValidationAudio
import com.hrd.localvoice.workers.UpdateAssignedAudiosWorker
import com.hrd.localvoice.workers.ValidationUploadWorker
import java.util.concurrent.TimeUnit

class AssignedAudiosActivity : AppCompatActivity() {
    lateinit var binding: ActivityAssignedAudiosBinding
    private lateinit var viewModel: ValidationActivityViewModel
    private lateinit var adapter: ValidationAudioAdapter
    private var audios: List<ValidationAudio>? = null

    // Background work manager
    private val constraints = Constraints.Builder().apply {
        setRequiredNetworkType(NetworkType.CONNECTED)
    }.build()
    private val workManager = WorkManager.getInstance(application)
    private val validationUploadWorker =
        PeriodicWorkRequest.Builder(ValidationUploadWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignedAudiosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[ValidationActivityViewModel::class.java]

        title = "Validations"

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ValidationAudioAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadAudios()

        // Swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            loadAudios()
        }

        // Attach listener to update local images button
        binding.buttonGetAssignedAudios.setOnClickListener {
            scheduleDownloadWorker(constraints, workManager)
        }

        binding.statsCards.setOnClickListener {
            showStatsExplanation()
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AudioValidationActivity::class.java))
        }

        // Schedule auto validation upload.
        workManager.enqueueUniquePeriodicWork(
            "ValidationUploadWorker", ExistingPeriodicWorkPolicy.UPDATE, validationUploadWorker
        )
    }

    private fun scheduleDownloadWorker(
        constraints: Constraints, workManager: WorkManager
    ) {
        val updateAssignedAudiosWorker = PeriodicWorkRequest.Builder(
            UpdateAssignedAudiosWorker::class.java, 30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(
            "UpdateAssignedAudiosWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateAssignedAudiosWorker
        )
        Toast.makeText(
            this, "Requested new audios for validation.", Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.validation_audios_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_get_new_audios -> {
                scheduleDownloadWorker(constraints, workManager)
            }
            R.id.action_clear_all_pending -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("DELETE PENDING AUDIOS")
                builder.setMessage("Are you sure you want to delete all audios awaiting your validation?")
                    .setNegativeButton("CANCEL") { _, _ -> }
                    .setPositiveButton("YES") { _, _ ->
                        AppRoomDatabase.databaseWriteExecutor.execute {
                            val pendingAudios = AppRoomDatabase.INSTANCE?.ValidationAudioDao()
                                ?.getSyncPendingAudioValidations()
                            if (pendingAudios?.isNotEmpty() == true) {
                                AppRoomDatabase.INSTANCE?.ValidationAudioDao()
                                    ?.delete(pendingAudios)
                            }
                        }
                    }.show()
            }
            R.id.action_upload_validations -> {
                workManager.enqueueUniquePeriodicWork(
                    "ValidationUploadWorker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    validationUploadWorker
                )
                Toast.makeText(this, "Scheduled upload for validated audios.", Toast.LENGTH_LONG)
                    .show()
            }
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showStatsExplanation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("INFO")
        builder.setMessage(getString(R.string.validation_audios_availability))
            .setPositiveButton("OK") { _, _ ->
            }.show()
    }

    private fun loadAudios() {
        viewModel.getValidationAudios()?.observe(this) {
            audios = it
            adapter.setData(it)
            binding.swiperefresh.isRefreshing = false
            binding.pendingCountLabel.text = "${audios?.size} Pending"

            AppRoomDatabase.databaseWriteExecutor.execute {
                val hoursToKeepAudiosForValidation = AppRoomDatabase.INSTANCE?.ConfigurationDao()
                    ?.getConfiguration()?.hoursToKeepAudiosForValidation
                hoursToKeepAudiosForValidation?.let { hours ->
                    if (audios.isNullOrEmpty()) return@execute

                    val lastAudio = audios?.let { aus -> aus[aus.size - 1].createdAt }
                    lastAudio?.let {
                        val timeLeft =
                            (-System.currentTimeMillis() + hours * 3600 * 1000 + lastAudio) / (3600 * 1000)
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
}