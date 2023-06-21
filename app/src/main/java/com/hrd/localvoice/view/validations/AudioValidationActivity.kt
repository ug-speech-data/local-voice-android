package com.hrd.localvoice.view.validations

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityAudioValidationBinding
import com.hrd.localvoice.databinding.LayoutSkipWarningBinding
import com.hrd.localvoice.models.ValidationAudio
import com.hrd.localvoice.utils.AudioStatus
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.Constants.DO_NOT_SHOW_SKIP_WARNING
import com.hrd.localvoice.view.MainActivity
import java.io.File


class AudioValidationActivity : AppCompatActivity() {
    val tag = "AudioValidationActivity"
    lateinit var binding: ActivityAudioValidationBinding
    private lateinit var viewModel: ValidationActivityViewModel
    private var player: ExoPlayer? = null
    private var isAudioPlaying = false
    private var playedFullAudio = false
    private var progressBarThread: Thread? = null
    private var allAudios: MutableList<ValidationAudio> = mutableListOf()
    private var currentIndex = 0
    private var currentAudio: ValidationAudio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioValidationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.audio_validation)
        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var currentIndex = intent.getIntExtra("position", 0)

        val preferences: SharedPreferences =
            getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)

        viewModel = ViewModelProvider(this)[ValidationActivityViewModel::class.java]
        viewModel.isLoading.observe(this) { value ->
            if (value) {
                binding.loadingProcessBar.visibility = View.VISIBLE
                binding.skipButton.isEnabled = false
                binding.acceptButton.isEnabled = false
                binding.rejectButton.isEnabled = false
            } else {
                binding.loadingProcessBar.visibility = View.GONE
                binding.skipButton.isEnabled = true
            }
        }

        // If user does not have permission, close the activity.
        viewModel.user?.observe(this) { user ->
            if (user?.permissions?.contains("validate_audio") != true) {
                Toast.makeText(this, "Unauthorised", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        AppRoomDatabase.databaseWriteExecutor.execute {
            val audios =
                AppRoomDatabase.INSTANCE?.ValidationAudioDao()?.getSyncPendingAudioValidations()
            allAudios = audios as MutableList<ValidationAudio>
            currentIndex = allAudios.size - currentIndex - 1
            runOnUiThread {
                showAudioAtIndex(currentIndex)
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            binding.messageTextView.text = message
        }

        // Image navigation
        binding.skipButton.setOnClickListener {
            if (!preferences.getBoolean(DO_NOT_SHOW_SKIP_WARNING, false)) {
                showSkipWarning()
            } else {
                deleteAudio(currentAudio!!)
            }
        }

        binding.acceptButton.setOnClickListener {
            currentAudio?.let { audio ->
                AppRoomDatabase.databaseWriteExecutor.execute {
                    audio.validatedStatus = AudioStatus.ACCEPTED
                    audio.updatedAt = System.currentTimeMillis()
                    AppRoomDatabase.INSTANCE?.ValidationAudioDao()
                        ?.updateAudioValidation(audio)
                    runOnUiThread {
                        allAudios.remove(audio)
                        showAudioAtIndex(currentIndex)
                    }
                }
            }
        }

        binding.rejectButton.setOnClickListener {
            currentAudio?.let { audio ->
                AppRoomDatabase.databaseWriteExecutor.execute {
                    audio.validatedStatus = AudioStatus.REJECTED
                    audio.updatedAt = System.currentTimeMillis()
                    AppRoomDatabase.INSTANCE?.ValidationAudioDao()
                        ?.updateAudioValidation(audio)
                    runOnUiThread {
                        allAudios.remove(audio)
                        showAudioAtIndex(currentIndex)
                    }
                }
            }
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

        binding.playerProgressBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
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
                            this@AudioValidationActivity,
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
            .setView(skipWarningBinding.root)
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                val editPref =
                    getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE).edit()
                editPref.putBoolean(DO_NOT_SHOW_SKIP_WARNING, doDoNotShow).apply()
                deleteAudio(currentAudio!!)
            }
            .setNegativeButton(
                "No"
            ) { dialog, id -> dialog.cancel() }.show()
    }

    private fun deleteAudio(audio: ValidationAudio) {
        player?.stop()

        if (audio.localImageUrl?.let { it1 -> File(it1).exists() } == true) audio.localImageUrl?.let { it1 ->
            File(
                it1
            ).delete()
        }
        if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
            File(
                it1
            ).delete()
        }
        AppRoomDatabase.databaseWriteExecutor.execute {
            AppRoomDatabase.INSTANCE?.ValidationAudioDao()?.delete(audio)
            runOnUiThread {
                Toast.makeText(
                    this@AudioValidationActivity,
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
        dialog.setMessage("Please refresh your validation list.")
        dialog.create()
        dialog.show()
    }

    private fun showAudioAtIndex(index: Int) {
        playedFullAudio = false
        if (allAudios.isEmpty()) {
            showNoAudiosDialog()
            return
        }
        currentAudio = allAudios[index.mod(allAudios.size)]
        if (currentAudio == null) return

        binding.audioLocale.text = currentAudio!!.locale
        binding.audioEnvironment.text = currentAudio!!.environment
        binding.audioDuration.text = currentAudio!!.duration.toString()
        binding.audioName.text = currentAudio!!.id.toString()

        // Load images
        val imageUrl = currentAudio!!.localImageUrl
        val options: RequestOptions =
            RequestOptions().fitCenter().placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
        Glide.with(this).load(imageUrl).apply(options)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imageView)

        initializePlayer()
    }

    private fun initializePlayer() {
        if (currentAudio == null) return
        binding.rejectButton.isEnabled = false
        binding.acceptButton.isEnabled = false

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
                        binding.rejectButton.isEnabled = true
                        binding.acceptButton.isEnabled = true
                        binding.playPauseButton.isEnabled = true
                        playedFullAudio = true
                        player?.pause()
                        player?.seekTo(0)
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
                    this@AudioValidationActivity,
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
                    player?.let { player ->
                        val progress = (player.currentPosition * 100) / playerDuration
                        val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                        val seconds = (duration % 60).toString().padStart(2, '0')
                        binding.timerLabel.text = "${minutes}:${seconds}"
                        binding.playerProgressBar.progress = progress.toInt()
                        isAudioPlaying = player.isPlaying == true
                        if (player.currentPosition.div(1000) >= 5 && !binding.rejectButton.isEnabled) {
                            binding.rejectButton.isEnabled = true
                        }
                    }
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