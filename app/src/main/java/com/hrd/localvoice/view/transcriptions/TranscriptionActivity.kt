package com.hrd.localvoice.view.transcriptions

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityTranscriptionBinding
import com.hrd.localvoice.databinding.LayoutSkipWarningBinding
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.TranscriptionStatus
import com.hrd.localvoice.view.MainActivity
import java.io.File


class TranscriptionActivity : AppCompatActivity() {
    val tag = "TranscriptionActivity"
    lateinit var binding: ActivityTranscriptionBinding
    private lateinit var viewModel: TranscriptionActivityViewModel
    private var player: ExoPlayer? = null
    private var isAudioPlaying = false
    private var playedFullAudio = false
    private var progressBarThread: Thread? = null
    private var allAudios: MutableList<TranscriptionAudio> = mutableListOf()
    private var currentIndex = 0
    private var currentAudio: TranscriptionAudio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Transcriptions"
        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this)[TranscriptionActivityViewModel::class.java]

        var currentIndex = intent.getIntExtra("position", 0)
        val preferences: SharedPreferences =
            getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)

        // If user does not have permission, close the activity.
        viewModel.user?.observe(this) { user ->
            if (user?.permissions?.contains("transcribe_audio") != true) {
                Toast.makeText(this, "Unauthorised", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        AppRoomDatabase.databaseWriteExecutor.execute {
            val audios = AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()
                ?.getSyncPendingAudioTranscriptions()
            allAudios = audios as MutableList<TranscriptionAudio>
            currentIndex = allAudios.size - currentIndex - 1
            runOnUiThread {
                showAudioAtIndex(currentIndex)
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                binding.messageTextView.visibility = View.VISIBLE
                binding.messageTextView.text = message
            } else {
                binding.messageTextView.visibility = View.GONE
            }
        }

        // Image navigation
        binding.skipButton.setOnClickListener {
            if (!preferences.getBoolean(Constants.DO_NOT_SHOW_TRANSCRIPTION_SKIP_WARNING, false)) {
                showSkipWarning()
            } else {
                deleteAudio(currentAudio!!)
            }
        }

        binding.saveButton.setOnClickListener {
            showSaveConfirmationDialog()
        }

        binding.playPauseButton.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                binding.playPauseButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                player?.play()
                binding.playPauseButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
        }

        binding.playerProgressBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (playedFullAudio) player?.let { player ->
                        player.seekTo(player.duration * progress / 100)
                    } else {
                        Toast.makeText(
                            this@TranscriptionActivity,
                            "Cannot seek around until full audio is played.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun showSkipWarning() {
        val skipWarningBinding = LayoutSkipWarningBinding.inflate(layoutInflater)
        var doDoNotShow = false
        skipWarningBinding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            doDoNotShow = isChecked
        }
        skipWarningBinding.checkbox.text = getString(R.string.do_not_show_again)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("SKIP AUDIO")
        builder.setMessage("Audios skipped will be deleted from your device. Continue to skip?")
            .setView(skipWarningBinding.root).setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                val editPref =
                    getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE).edit()
                editPref.putBoolean(Constants.DO_NOT_SHOW_TRANSCRIPTION_SKIP_WARNING, doDoNotShow)
                    .apply()
                deleteAudio(currentAudio!!)
            }.setNegativeButton(
                "No"
            ) { dialog, id -> dialog.cancel() }.show()
    }

    private fun showAudioAtIndex(index: Int) {
        playedFullAudio = false
        binding.textArea.text?.clear()
        if (allAudios.isEmpty()) {
            showNoAudiosDialog()
            return
        }
        currentAudio = allAudios[index.mod(allAudios.size)]
        currentAudio?.let { audio ->
            binding.audioLocale.text = audio.locale
            audio.text?.let { text -> binding.textArea.setText(text, TextView.BufferType.EDITABLE) }
            binding.audioEnvironment.text = audio.environment
            binding.audioDuration.text = audio.duration.toString()
            binding.audioName.text = audio.id.toString()
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        if (currentAudio == null) return
        binding.saveButton.isEnabled = false

        if (player == null) {
            player = ExoPlayer.Builder(this).build()
        }
        player?.playWhenReady = false
        player?.seekTo(0)
        currentAudio?.localAudioUrl?.let { MediaItem.fromUri(it) }?.let { player?.setMediaItem(it) }
        player?.prepare()
        binding.playerLoading.visibility = View.VISIBLE

        player?.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                progressBarThread = Thread(updateProgressRunnable)
                progressBarThread?.start()
                binding.playPauseButton.isEnabled = false
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        binding.saveButton.isEnabled = binding.textArea.text.toString().isNotEmpty()
                        binding.playPauseButton.isEnabled = true
                        player?.pause()
                        player?.seekTo(0)
                        playedFullAudio = true
                    }
                    Player.STATE_IDLE, Player.STATE_READY -> {
                        binding.playerLoading.visibility = View.GONE
                        binding.playPauseButton.isEnabled = true
                    }
                    Player.STATE_BUFFERING -> binding.playerLoading.visibility = View.VISIBLE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                binding.playerLoading.visibility = View.GONE
                Toast.makeText(
                    this@TranscriptionActivity,
                    "Couldn't play audio: ${error.errorCodeName}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(tag, "onPlayerError: ${error.message}")
            }
        })
    }

    private val updateProgressRunnable = Runnable {
        while (true) {
            runOnUiThread {
                // Update timer text
                var duration = player?.duration?.div(1000)
                if (player?.duration == C.TIME_UNSET) {
                    duration = 0
                }
                val playerDuration = player?.duration
                if (playerDuration != 0L && duration != null && playerDuration != null) {
                    val progress = (player!!.currentPosition * 100) / playerDuration
                    val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                    val seconds = (duration % 60).toString().padStart(2, '0')
                    binding.timerLabel.text = "${minutes}:${seconds}"
                    binding.playerProgressBar.progress = progress.toInt()
                    isAudioPlaying = player?.isPlaying == true

                    if (isAudioPlaying) binding.playPauseButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                }
            }
            Thread.sleep(1000)
            if (!isAudioPlaying) {
                runOnUiThread {
                    binding.playPauseButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                }
                break
            }
        }
    }

    private fun deleteAudio(audio: TranscriptionAudio) {
        player?.stop()
        if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
            File(
                it1
            ).delete()
        }
        AppRoomDatabase.databaseWriteExecutor.execute {
            AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()?.delete(audio)
            runOnUiThread {
                Toast.makeText(
                    this@TranscriptionActivity,
                    "Removed audio ${audio.id} from device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        allAudios.remove(audio)
        showAudioAtIndex(currentIndex)
    }

    private fun showNoAudiosDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("No audios found").setCancelable(false)
            .setPositiveButton("GO HOME") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        dialog.setMessage("Please refresh your transcription list.")
        dialog.create()
        dialog.show()
    }

    private fun showSaveConfirmationDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("Save").setCancelable(false)
            .setNegativeButton("No") { _, _ -> }
            .setPositiveButton("Yes") { _, _ ->
                currentAudio?.let { audio ->
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        audio.transcriptionStatus = TranscriptionStatus.TRANSCRIBED
                        audio.text = binding.textArea.text.toString()
                        audio.updatedAt = System.currentTimeMillis()
                        AppRoomDatabase.INSTANCE?.TranscriptionAudioDao()
                            ?.updateAudioTranscription(audio)
                        runOnUiThread {
                            allAudios.remove(audio)
                            showAudioAtIndex(currentIndex)
                        }
                    }
                }
            }
        dialog.setMessage("Are you sure you want to save your transcription?")
        dialog.create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun releasePlayer() {
        if (player != null) {
            player?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}