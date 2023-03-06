package com.hrd.localvoice.view.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityAudioRecorderBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.WaveRecorder
import com.hrd.localvoice.view.ImageViewActivity
import com.hrd.localvoice.view.MainActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity.Companion.currentParticipantData
import com.hrd.localvoice.view.participants.ParticipantCompensationDetailsActivity


class AudioRecorderActivity : AppCompatActivity() {
    private val tag = "AudioRecorderActivity"
    private lateinit var viewModel: RecorderActivityViewModel
    private lateinit var availableImages: MutableList<Image>
    private lateinit var binding: ActivityAudioRecorderBinding
    private lateinit var recorder: WaveRecorder
    private var currentImageIndex = 0
    private val requestPermissionCode = 4
    private var currentParticipant: Participant? = null
    private var currentUser: User? = null
    private var environment: String? = null
    private val totalExpectedDescription = 120
    private val maxAudioDuration = 29
    private var totalDescriptionCount = 0
    private var requiredAudioDuration = 15
    private var allowedPauseDuration = 3

    // List of images described by the current participant
    private val deviceId = Build.MANUFACTURER + " " + Build.MODEL

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecorderBinding.inflate(layoutInflater);
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[RecorderActivityViewModel::class.java]
        recorder = WaveRecorder(this)

        // App bar actions
        binding.buttonDone.setOnClickListener {
            if (totalDescriptionCount < totalExpectedDescription) {
                showMaximumImageCountReachedDialog()
            } else {
                done()
            }
        }

        AppRoomDatabase.databaseWriteExecutor.execute {
            // Retrieve current participant
            val prefsEditor: SharedPreferences =
                getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
            val participantId = prefsEditor.getLong(currentParticipantData, -1)

            currentParticipant = viewModel.getParticipantById(participantId)
            runOnUiThread { loadImages() }

            if (currentParticipant != null) {
                environment = currentParticipant!!.environment
            }
        }

        viewModel.descriptionCount.observe(this) {
            binding.imageCountLabel.text = "${
                it.toString().padStart(totalExpectedDescription.toString().length, '0')
            }/$totalExpectedDescription"
        }

        //Check Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
            requestPermissions(permissions, requestPermissionCode)
        }

        binding.startStopButton.setOnClickListener {
            if (!recorder.isRecording()) {
                recorder.startRecording()
                binding.startStopButton.text = getString(R.string.stop)
                Thread(updateProgressRunnable).start()
                Thread(monitorAudioAmplitude).start()
            } else {
                showRecordingCompletedDialog()
                recorder.stopRecording()
                binding.startStopButton.text = getString(R.string.start)
            }
        }

        binding.imageView.setOnClickListener {
            val intent = Intent(this, ImageViewActivity::class.java)
            intent.putExtra(
                "IMAGE_PATH", availableImages[currentImageIndex.mod(availableImages.size)].localURl
            )
            startActivity(intent)
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
    }

    private fun loadImages() {
        val currentParticipantId = currentParticipant?.id ?: -1 // If recording oneself.

        AppRoomDatabase.databaseWriteExecutor.execute {
            val audios =
                AppRoomDatabase.INSTANCE?.AudioDao()?.getAudiosByParticipant(currentParticipantId)
            // Prevent double description of same image
            val excludedImageIds = mutableListOf<Long>()

            runOnUiThread {
                viewModel.descriptionCount.value = audios?.size
                audios?.size.also {
                    if (it != null) {
                        totalDescriptionCount = it
                    }
                }
            }

            audios?.forEach { audio ->
                excludedImageIds.add(audio.remoteImageID)
            }

            // Get images, excluding already described by current participant
            val images = AppRoomDatabase.INSTANCE?.ImageDao()?.getImages(excludedImageIds)
            Log.d("TEST", "images: ${images?.size}")

            availableImages = images as MutableList<Image>
            currentImageIndex = 0
            if (availableImages.isEmpty()) {
                runOnUiThread {
                    showNoImagesDialog()
                }
            } else {
                runOnUiThread {
                    showImageAtIndex(currentImageIndex)
                }
            }
            binding.previousImageButton.isEnabled = availableImages.isNotEmpty()
            binding.nextImageButton.isEnabled = availableImages.isNotEmpty()
            binding.startStopButton.isEnabled = availableImages.isNotEmpty()
        }
    }

    private fun saveAudioIntoFile() {
        val currentImage = availableImages[currentImageIndex.mod(availableImages.size)]
        val fileName = currentUser?.locale + "_image_" + currentImage.remoteId.toString().padStart(
            4, '0'
        ) + "_u${currentImage.remoteId}_${currentImage.descriptionCount + 1}_${System.currentTimeMillis()}.wav"

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

            availableImages.remove(currentImage)

            // Increase image description count
            currentImage.descriptionCount += 1
            viewModel.updateImage(currentImage)

            totalDescriptionCount += 1
            viewModel.descriptionCount.value = totalDescriptionCount

            // Reset Recording box control
            binding.timerLabel.text = "00:00"

            showImageAtIndex(++currentImageIndex)

            Toast.makeText(this, "Audio saved.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio save failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun done() {
        if (currentParticipant != null && totalDescriptionCount > 0) {
            startActivity(Intent(this, ParticipantCompensationDetailsActivity::class.java))
            finish()
        } else if (totalDescriptionCount > 0) {
            Toast.makeText(
                this, getString(R.string.saved_recording), Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showNoImagesDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("No images found").setCancelable(false)
            .setPositiveButton("GO HOME") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        dialog.setMessage("Please download the images by tapping on \"Update Local Images\" on the home screen.")
        dialog.create()
        dialog.show()
    }

    private fun showSaveDeleteBottomSheetDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.recorder_bottom_sheet_dialog_layout)
        dialog.setCancelable(false)
        val playStopButton = dialog.findViewById<Button>(R.id.play_stop_button)
        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        val deleteButton = dialog.findViewById<TextView>(R.id.delete_button)

        // Playback
        playStopButton?.setOnClickListener {
            if (!recorder.isAudioPlaying()) {
                recorder.playBackRecording()
                playStopButton.text = getString(R.string.stop)
            } else {
                recorder.stopPlayback()
                playStopButton.text = getString(R.string.play)
            }
        }

        // Save
        saveButton?.setOnClickListener {
            // Stop play in case the user left it playing before exiting
            recorder.stopPlayback()

            saveAudioIntoFile()
            dialog.dismiss()
            binding.timerLabel.text = "00:00"
            recorder.reset()
        }

        // delete
        deleteButton?.setOnClickListener {
            // Audio is written to only a temporary location.
            // Next recording will overwrite, no need to perform an actual deletion.

            // Stop play in case the user left it playing before exiting
            recorder.stopPlayback()

            dialog.dismiss()
            binding.timerLabel.text = "00:00"
        }
        dialog.show()
    }

    private fun showRecordingCompletedDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("Recording completed").setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                binding.timerLabel.text = "00:00"
                recorder.stopRecording()
                recorder.reset()
            }

        var message = ""
        if (recorder.silentDuration() >= allowedPauseDuration) {
            message = "Please the recording contains too many lapses."
        } else if (recorder.audioDuration() < requiredAudioDuration) {
            message = "Please record for at least $requiredAudioDuration seconds."
        } else if (recorder.audioDuration() > maxAudioDuration) {
            message = "Please audio should be less than or equal to $maxAudioDuration seconds."
        } else {
            showSaveDeleteBottomSheetDialog()
        }

        // Show error dialog box
        if (recorder.audioDuration() < requiredAudioDuration || recorder.audioDuration() > maxAudioDuration || recorder.silentDuration() >= allowedPauseDuration) {
            dialog.setMessage(message)
            dialog.create()
            dialog.show()
        }
    }

    private fun showMaximumImageCountReachedDialog() {
        val dialog =
            AlertDialog.Builder(this).setTitle("Expected $totalExpectedDescription descriptions")
                .setCancelable(false).setNegativeButton(getString(R.string.no)) { _, _ ->
                    done()
                }.setPositiveButton(getString(R.string.yes)) { _, _ -> }

        val message =
            "You have recorded only $totalDescriptionCount descriptions. You will only be paid if you record $totalExpectedDescription descriptions. Continue to record?"
        dialog.setMessage(message)
        dialog.create()
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private val updateProgressRunnable = Runnable {
        while (true) {
            runOnUiThread {
                // Update timer text
                val duration = recorder.audioDuration()
                val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                val seconds = (duration % 60).toString().padStart(2, '0')
                binding.timerLabel.text = "${minutes}:${seconds}"

                if (duration > maxAudioDuration) {
                    recorder.stopRecording()
                    binding.startStopButton.text = getString(R.string.start)
                    showRecordingCompletedDialog()
                }

                // Check audio duration
                if (duration >= maxAudioDuration || recorder.silentDuration() >= allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(200, 50, 50))
                } else if (duration >= requiredAudioDuration && recorder.silentDuration() <= allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(50, 200, 50))
                } else {
                    binding.timerLabel.setTextColor(Color.rgb(50, 50, 50))
                }

                // Warn user to stop
                if (duration >= (maxAudioDuration - 5) && recorder.silentDuration() < allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(200, 150, 50))
                }

                // Auto stop when the desired audio length is reached and the
                // participant pauses.
                if (duration >= requiredAudioDuration && recorder.silentDuration() >= (allowedPauseDuration - 1)) {
                    recorder.stopRecording()
                    binding.startStopButton.text = getString(R.string.start)
                    showRecordingCompletedDialog()
                }
            }
            Thread.sleep(100)
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
        if (recorder.isRecording() || availableImages.size < 1) return

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
        for (permission in permissions) {
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
            Toast.makeText(this, "Perm Denied", Toast.LENGTH_SHORT).show()
            (this.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            finish()
        } else {
            recreate()
        }
    }

    override fun onStop() {
        super.onStop()
        recorder.release()
    }
}