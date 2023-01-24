package com.hrd.localvoice.view.videoplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityVideoPlayerBinding
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.view.BackgroundAudioCheckActivity
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var fullscreenContent: ConstraintLayout
    private var player: ExoPlayer? = null
    private val hideHandler = Handler(Looper.myLooper()!!)
    private var fullscreenButton: ImageView? = null
    private var isFullscreen: Boolean = false
    private lateinit var viewModel: VideoPlayerActivityViewModel
    private var configuration: Configuration? = null

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[VideoPlayerActivityViewModel::class.java]

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Demo Video"

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        fullscreenButton = findViewById(R.id.exo_fullscreen_icon);
        fullscreenButton?.setOnClickListener { toggle() }

        viewModel.getConfiguration()?.observe(this) { it ->
            configuration = it
            initializePlayer()
        }

        showPrivacyPolicyBottomSheetDialog()
    }

    private fun showPrivacyPolicyBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.demo_video_info_bottom_sheet_dialog_layout)

        val actionButton = bottomSheetDialog.findViewById<Button>(R.id.action_button)
        actionButton?.setOnClickListener {
            bottomSheetDialog.dismiss();
            player?.playWhenReady = true
            if (player?.isPlaying == false) {
                player?.play()
            }
        }
        bottomSheetDialog.show()
    }

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
    }

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            binding.videoView.player = player
        }

        player!!.playWhenReady = false
        player!!.seekTo(0)
        val file = configuration?.demoVideoLocalUrl?.let { File(it) }
        if (file?.exists() == true) {
            player!!.setMediaItem(MediaItem.fromUri(file.absolutePath))
            player!!.prepare()
            binding.progressBar.visibility = View.GONE
        }

        player!!.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                when (playbackState) {
                    Player.STATE_IDLE, Player.STATE_READY -> binding.progressBar.visibility =
                        View.GONE
                    Player.STATE_BUFFERING -> binding.progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@VideoPlayerActivity,
                    "Couldn't play video: " + error.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.video_player_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_continue -> {
                val intent = Intent(this, BackgroundAudioCheckActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
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

    override fun onResume() {
        super.onResume()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}