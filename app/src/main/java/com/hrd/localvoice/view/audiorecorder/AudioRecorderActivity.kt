package com.hrd.localvoice.view.audiorecorder

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityAudioRecorderBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.WaveRecorder
import com.hrd.localvoice.view.MainActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity.Companion.currentParticipantData
import com.hrd.localvoice.view.participants.ParticipantCompensationDetailsActivity

class AudioRecorderActivity : AppCompatActivity() {
    private val tag = "AudioRecorderActivity"
    private lateinit var viewModel: RecorderActivityViewModel
    private var availableImages: MutableList<Image> = mutableListOf()
    private lateinit var binding: ActivityAudioRecorderBinding
    private lateinit var recorder: WaveRecorder
    private var currentImageIndex = 0
    private val REQUEST_PERMISSIONS_CODE = 4
    private var currentParticipant: Participant? = null
    private var currentUser: User? = null
    private var environment: String? = null

    // List of images described by the current participant
    private var describedImages: MutableList<Image> = mutableListOf()
    private val deviceId = Build.MANUFACTURER + " " + Build.MODEL

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecorderBinding.inflate(layoutInflater);
        setContentView(binding.root)
        title = "Recorder"
        setup()
        recorder = WaveRecorder(this)

        AppRoomDatabase.databaseWriteExecutor.execute {
            // Retrieve current participant
            val prefsEditor: SharedPreferences =
                getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
            val participantId = prefsEditor.getLong(currentParticipantData, -1)

            currentParticipant = viewModel.getParticipantById(participantId)

            if (currentParticipant != null) {
                environment = currentParticipant!!.environment
            }
        }

        //Check Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE)
        }

        binding.startStopButton.setOnClickListener {
            if (!recorder.isRecording()) {
                recorder.startRecording()
                binding.startStopButton.text = getString(R.string.stop)
                binding.playStopButton.isEnabled = false
                binding.saveButton.isEnabled = false
                Thread(updateProgressRunnable).start()
                Thread(monitorAudioAmplitude).start()
            } else {
                showRecordingCompletedDialog()
                recorder.stopRecording()
                binding.startStopButton.text = getString(R.string.start)
                binding.playStopButton.isEnabled = true
            }
        }

        binding.playStopButton.setOnClickListener {
            if (!recorder.isAudioPlaying()) {
                recorder.playBackRecording()
                binding.playStopButton.text = getString(R.string.stop)
                binding.startStopButton.isEnabled = false
            } else {
                recorder.stopPlayback()
                binding.playStopButton.text = getString(R.string.play)
                binding.startStopButton.isEnabled = true
            }
        }

        binding.imageView.setOnClickListener {
            Log.d(tag, "imageView clicked: ")
        }

        binding.previousImageButton.setOnClickListener {
            showImageAtIndex(--currentImageIndex)
        }

        binding.nextImageButton.setOnClickListener {
            showImageAtIndex(++currentImageIndex)
        }

        viewModel.user?.observe(this) {
            currentUser = it
            if (environment == null && currentUser != null) {
                environment = currentUser?.environment
            }
        }

        // Fetch maximum description count from db
        viewModel.getConfiguration()?.observe(this) { configuration ->
            val maxImageDescription =
                if (configuration?.maxImageDescriptionCount != null) configuration.maxImageDescriptionCount else 5

            // Get images without required number of descriptions
            viewModel.getImages(maxImageDescription!!)?.observe(this) { images ->
                availableImages = images as MutableList<Image>

                // Remove already described images
                availableImages.removeAll(describedImages)

                if (availableImages.isNotEmpty()) showImageAtIndex(0)

                binding.previousImageButton.isEnabled = availableImages.isNotEmpty()
                binding.nextImageButton.isEnabled = availableImages.isNotEmpty()
                binding.startStopButton.isEnabled = availableImages.isNotEmpty()
                binding.playStopButton.isEnabled = availableImages.isNotEmpty()
            }
        }

        binding.saveButton.setOnClickListener {
            val currentImage = availableImages[currentImageIndex.mod(availableImages.size)]
            val fileName = "image_" + currentImage.remoteId.toString().padStart(
                6, '0'
            ) + "_description_${currentImage.descriptionCount + 1}_${System.currentTimeMillis()}.wav"
            val result = recorder.saveAudioIntoFile(fileName)
            if (result != null && currentUser != null) {
                val description = currentImage.name
                val audio = Audio(
                    userId = currentUser!!.id,
                    timestamp = System.currentTimeMillis(),
                    remoteImageID = currentImage.remoteId,
                    localFileURl = result,
                    description = description,
                    duration = recorder.audioDuration(),
                    deviceId = deviceId,
                    environment = environment!!
                )
                if (currentParticipant != null) {
                    audio.participantId = currentParticipant!!.id
                }

                viewModel.insertAudio(audio)

                // Increase image description count
                currentImage.descriptionCount += 1
                viewModel.updateImage(currentImage)
                describedImages.add(currentImage)

                // Reset Recording box control
                binding.timerLabel.text = "00:00"
                binding.saveButton.isEnabled = false

                Toast.makeText(this, "Audio saved.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Audio save failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setup() {
        viewModel = ViewModelProvider(this)[RecorderActivityViewModel::class.java]
    }

    private fun showRecordingCompletedDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("Recording completed").setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }

        var message = ""
        if (recorder.silentDuration() >= 3) {
            message = "Please the recording contains too many lapses."
        } else if (recorder.audioDuration() < 17) {
            message = "Please record for at least 17 seconds."
        } else if (recorder.audioDuration() > 30) {
            message = "Please audio should be less than 30 seconds."
        } else {
            binding.saveButton.isEnabled = true
            binding.saveButton.visibility = View.VISIBLE
        }

        // Show error dialog box
        if (recorder.audioDuration() < 17 || recorder.audioDuration() > 30 || recorder.silentDuration() >= 3) {
            dialog.setMessage(message)
            dialog.create()
            dialog.show()
        }
    }

    private val updateProgressRunnable = Runnable {
        while (true) {
            runOnUiThread {
                // Update timer text
                val duration = recorder.audioDuration()
                val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                val seconds = (duration % 60).toString().padStart(2, '0')
                binding.timerLabel.text = "${minutes}:${seconds}"

                // Check audio duration
                if (duration >= 30 || recorder.silentDuration() >= 3) {
                    binding.timerLabel.setTextColor(Color.rgb(200, 50, 50))
                } else if (duration >= 17 && recorder.silentDuration() <= 3) {
                    binding.timerLabel.setTextColor(Color.rgb(50, 200, 50))
                } else {
                    binding.timerLabel.setTextColor(Color.rgb(50, 50, 50))
                }
            }
            Thread.sleep(1000)
            if (!recorder.isRecording()) {
                break
            }
        }
    }

    private val monitorAudioAmplitude = Runnable {
        while (true) {
            runOnUiThread {
                // Update audio amplitude display
                val percentage = (recorder.averageAmplitude / 1500 * 100).toInt()
                binding.progressBar.progress = percentage
            }
            Thread.sleep(100)
            if (!recorder.isRecording()) {
                runOnUiThread {
                    binding.progressBar.progress = 0
                }
                break
            }
        }
    }

    private fun showImageAtIndex(index: Int) {
        binding.timerLabel.text = "00:00"

        val currentImage = availableImages[index.mod(availableImages.size)]
        val options: RequestOptions =
            RequestOptions().fitCenter().placeholder(R.mipmap.loading).error(R.mipmap.loading)
        val imageUrl =
            if (currentImage.localURl != null) currentImage.localURl else currentImage.remoteURL
        Glide.with(this).load(imageUrl).apply(options)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imageView)

        // Update image details
        binding.imageName.text = currentImage.name
        binding.imageCategory.text = currentImage.category
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun permissionsDenied(): Boolean {
        for (permission in PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDenied()) {
            Toast.makeText(this, "Perm Denied", Toast.LENGTH_LONG).show()
            (this.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            finish()
        } else {
            recreate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.recorder_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_next) {
            if (currentParticipant != null) {
                startActivity(Intent(this, ParticipantCompensationDetailsActivity::class.java))
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.saved_recording),
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        recorder.release()
    }
}