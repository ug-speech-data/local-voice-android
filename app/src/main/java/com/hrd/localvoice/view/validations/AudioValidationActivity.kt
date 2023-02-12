package com.hrd.localvoice.view.validations

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.*
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityAudioValidationBinding
import com.hrd.localvoice.models.UploadedAudio
import com.hrd.localvoice.utils.AudioStatus

class AudioValidationActivity : AppCompatActivity() {
    lateinit var binding: ActivityAudioValidationBinding
    private lateinit var viewModel: ValidationActivityViewModel
    private var currentAudio: UploadedAudio? = null
    private var player: ExoPlayer? = null
    private var isAudioPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioValidationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.audio_validation)
        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        // Get first audio
        viewModel.getAssignedAudios(-1)

        viewModel.errorMessage.observe(this) { message ->
            binding.messageTextView.text = message
        }

        // Image navigation
        binding.skipButton.setOnClickListener {
            if (currentAudio != null) {
                viewModel.getAssignedAudios(currentAudio!!.id)
            }
        }
        binding.acceptButton.setOnClickListener {
            viewModel.validateDate(currentAudio!!.id, AudioStatus.ACCEPTED)
        }
        binding.rejectButton.setOnClickListener {
            if (currentAudio != null) viewModel.validateDate(
                currentAudio!!.id,
                AudioStatus.REJECTED
            )
        }

        binding.playPauseButton.setOnClickListener {
            if (player != null) {
                if (player?.isPlaying == true) {
                    player?.pause()
                    binding.playPauseButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                } else {
                    player?.play()
                    binding.playPauseButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                }
            }
        }

        // Audio move to next image
        viewModel.validationSuccess.observe(this) { success ->
            if (success && currentAudio != null) {
                Toast.makeText(this, getString(R.string.success), Toast.LENGTH_SHORT).show()
                viewModel.getAssignedAudios(currentAudio!!.id)
            }
        }

        // If No more images
        viewModel.noMoreImages.observe(this) { done ->
            if (done) {
                showNoMoreImagesDialog()
            }
        }

        // Listen for response
        viewModel.audio.observe(this) { audio ->
            currentAudio = audio
            binding.audioLocale.text = audio.locale
            binding.audioEnvironment.text = audio.environment
            binding.audioDuration.text = audio.duration.toString()

            // Load images
            val imageUrl = audio.imageURL
            val options: RequestOptions =
                RequestOptions().fitCenter().placeholder(R.mipmap.loading).error(R.mipmap.loading)
            Glide.with(this).load(imageUrl).apply(options)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imageView)

            initializePlayer()
        }
    }

    private fun showNoMoreImagesDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("No more images").setCancelable(false)
            .setNegativeButton(getString(R.string.no)) { _, _ -> finish() }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.getAssignedAudios(-1)
            }.setMessage("There are no more images available. Would you like to restart?")

        dialog.create()
        dialog.show()
    }

    private fun initializePlayer() {
        if (currentAudio == null) return
        binding.rejectButton.isEnabled = false
        binding.acceptButton.isEnabled = false

        if (player == null) {
            player = ExoPlayer.Builder(this).build()
        }
        player!!.playWhenReady = false
        player!!.seekTo(0)
        player!!.setMediaItem(MediaItem.fromUri(currentAudio!!.audioURL))
        player!!.prepare()
        binding.playerLoading.visibility = View.VISIBLE

        player!!.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                Thread(updateProgressRunnable).start()
                binding.playPauseButton.isEnabled = false
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        binding.rejectButton.isEnabled = true
                        binding.acceptButton.isEnabled = true
                        binding.playPauseButton.isEnabled = true
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
                    "Couldn't play video: " + error.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private val updateProgressRunnable = Runnable {
        while (true) {
            runOnUiThread {
                // Update timer text
                var duration = player!!.duration / 1000
                if (player!!.duration == C.TIME_UNSET) {
                    duration = 0
                }
                val progress = (player!!.currentPosition * 100) / player!!.duration
                val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                val seconds = (duration % 60).toString().padStart(2, '0')
                binding.timerLabel.text = "${minutes}:${seconds}"
                binding.playerProgressBar.progress = progress.toInt()
                isAudioPlaying = player?.isPlaying == true

                if (isAudioPlaying)
                    binding.playPauseButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
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
            player!!.release()
            player = null
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

}