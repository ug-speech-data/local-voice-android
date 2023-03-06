package com.hrd.localvoice.view.local_files

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.adapters.AudioAdapter
import com.hrd.localvoice.databinding.ActivityMyAudiosBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.AUDIO_STATUS_UPLOADED
import com.hrd.localvoice.workers.UploadWorker
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MyAudiosActivity : AppCompatActivity() {
    private lateinit var adapter: AudioAdapter
    private lateinit var binding: ActivityMyAudiosBinding
    private lateinit var viewModel: MyAudiosActivityViewModel
    private var mediaPlayer: MediaPlayer? = MediaPlayer()
    private val tag = "MyAudiosActivity"
    private var currentAudio: Audio? = null
    private var audios: List<Audio>? = null
    private lateinit var playerDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAudiosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "My Audios"

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setup()
        loadAudios()

        // Swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            loadAudios()
        }

        adapter.setOnPlayStopAudioListener(object : AudioAdapter.OnPlayStopButtonClickListener {
            override fun playStopAudioListener(audio: Audio) {
                playerDialog = BottomSheetDialog(this@MyAudiosActivity)
                playerDialog.setContentView(R.layout.play_back_bottom_sheet_dialog_layout)
                val playStopButton = playerDialog.findViewById<Button>(R.id.play_stop_button)
                val audioEnvironmentLabel =
                    playerDialog.findViewById<TextView>(R.id.audio_environment_label)
                val audioNameLabel = playerDialog.findViewById<TextView>(R.id.audio_name_label)

                val participantLabelText =
                    playerDialog.findViewById<TextView>(R.id.audio_participant_momo_label_text)
                val participantLabel =
                    playerDialog.findViewById<TextView>(R.id.audio_participant_momo_label)
                val progressBar = playerDialog.findViewById<ProgressBar>(R.id.progress_bar)

                audioEnvironmentLabel?.text = audio.environment
                audioNameLabel?.text = audio.description

                if (audio.participantId != null) {
                    viewModel.getParticipant(audio.participantId!!)
                        ?.observe(this@MyAudiosActivity) { participant ->
                            if (participant != null) {
                                participantLabelText?.visibility = View.VISIBLE
                                participantLabel?.visibility = View.VISIBLE
                                participantLabel?.text = participant.momoNumber
                                participantLabel?.text =
                                    if (participant.momoNumber?.isNotEmpty() == true) participant.momoNumber else "null"
                            } else {
                                participantLabelText?.visibility = View.VISIBLE
                            }
                        }
                }

                playStopButton?.setOnClickListener {
                    if (mediaPlayer?.isPlaying == true && currentAudio?.localFileURl == audio.localFileURl) {
                        mediaPlayer?.stop()
                        playStopButton.text = getString(R.string.play)
                        progressBar?.progress = 0
                    } else {
                        playFile(File(audio.localFileURl))
                        playStopButton.text = getString(R.string.stop)

                        // Trigger progress bar update
                        Thread(updateProgressRunnable).start()
                    }
                    currentAudio = audio
                }

                playerDialog.setOnCancelListener {
                    mediaPlayer?.stop()
                }

                playerDialog.show()
            }
        })
    }

    private val updateProgressRunnable = Runnable {
        while (true) {
            if (mediaPlayer != null) {
                // Update timer text
                val totalDuration = mediaPlayer!!.duration
                val duration = mediaPlayer!!.currentPosition

                val progressBar = playerDialog.findViewById<ProgressBar>(R.id.progress_bar)
                if (progressBar != null && totalDuration > 0)
                    runOnUiThread {
                        progressBar.progress = ((duration.toFloat() / totalDuration.toFloat()) * 100).toInt()
                    }
            }
            Thread.sleep(1000)
            if (mediaPlayer?.isPlaying != true) {
                break
            }
        }
    }

    private fun setup() {
        viewModel = ViewModelProvider(this)[MyAudiosActivityViewModel::class.java]
        adapter = AudioAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadAudios() {
        viewModel.getAudios()?.observe(this) {
            audios = it
            adapter.setData(it)
            binding.swiperefresh.isRefreshing = false

            if (it.isEmpty()) {
                binding.infoText.visibility = View.VISIBLE
            } else {
                binding.infoText.visibility = View.GONE
            }
        }
    }

    fun playFile(file: File) {
        if (!file.exists()) return
        val fileName = file.absolutePath

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(fileName)
            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
            }
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            Log.d(tag, "onCreate: Can't open file: $fileName")
        }
    }

    private fun showAlertDialogBox() {
        val dialog =
            AlertDialog.Builder(this).setTitle("DELETE UPLOADED AUDIOS").setCancelable(false)
                .setNegativeButton("Cancel") { _, _ -> }.setPositiveButton("Delete") { _, _ ->
                    audios?.forEach { audio ->
                        if (audio.status == AUDIO_STATUS_UPLOADED) {
                            val file = File(audio.localFileURl)
                            file.delete()
                            viewModel.deleteAudio(audio)
                        }
                    }
                }.setMessage(getString(R.string.audio_deletion_info))
        dialog.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.my_audios_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_uploaded -> showAlertDialogBox()
            R.id.action_upload_audios -> {
                // Schedule on-time work
                val constraints = Constraints.Builder().apply {
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }.build()
                val workManager = WorkManager.getInstance(application)
                val uploadWorker =
                    PeriodicWorkRequest.Builder(UploadWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints).build()
                workManager.enqueueUniquePeriodicWork(
                    "AudioUploadWorker", ExistingPeriodicWorkPolicy.REPLACE, uploadWorker
                )
            }
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}