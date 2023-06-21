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
import android.widget.*
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
import com.hrd.localvoice.databinding.RecorderBottomSheetDialogLayoutBinding
import com.hrd.localvoice.models.*
import com.hrd.localvoice.utils.AudioUtil
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.WaveRecorder2
import com.hrd.localvoice.view.ImageViewActivity
import com.hrd.localvoice.view.MainActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity.Companion.currentParticipantData
import com.hrd.localvoice.view.participants.ParticipantCompensationDetailsActivity


class AudioRecorderActivity : AppCompatActivity() {
    private val tag = "AudioRecorderActivity"
    private lateinit var viewModel: RecorderActivityViewModel
    private lateinit var availableImages: MutableList<Image>
    private lateinit var binding: ActivityAudioRecorderBinding
    private lateinit var waveRecorder2: WaveRecorder2
    private var currentImageIndex = 0
    private val requestPermissionCode = 4
    private var currentParticipant: Participant? = null
    private var currentUser: User? = null
    private var environment: String? = null
    private var totalExpectedDescription = 120
    private val maxAudioDuration = 29
    private var totalDescriptionCount = 0
    private var requiredAudioDuration = 15
    private var allowedPauseDuration = 3
    private var configuration: Configuration? = null
    private var amplitudeLevelThread: Thread? = null

    private var backgroundNoiseCheckDurationInSec = 3
    private var maxBackgroundNoiseLevel = 350

    // List of images described by the current participant
    private val deviceId = Build.MANUFACTURER + " " + Build.MODEL

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecorderBinding.inflate(layoutInflater);
        setContentView(binding.root)

        try {
            viewModel = ViewModelProvider(this)[RecorderActivityViewModel::class.java]
            waveRecorder2 = WaveRecorder2(this)
        } catch (e: java.lang.Exception) {
            e.localizedMessage?.let { showErrorDialog(it) }
        }

        viewModel.getConfiguration()?.observe(this) { conf ->
            configuration = conf
            configuration?.numberOfAudiosPerParticipant?.let { count ->
                totalExpectedDescription = count
            }
        }

        // App bar actions
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.buttonDone.setOnClickListener {
            val dialog = AlertDialog.Builder(this).setTitle("Done?")
                .setNegativeButton(getString(R.string.no)) { _, _ -> }
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    done()
                }
            val message = "Do you want to end the current session?"
            dialog.setMessage(message)
            dialog.create()
            dialog.show()
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
        if (permissionsDenied()) {
            requestPermissions(permissions, requestPermissionCode)
        }

        binding.startStopButton.setOnClickListener {
            if (!waveRecorder2.isRecording()) {
                waveRecorder2.startRecording()
                binding.startStopButton.text = getString(R.string.stop)
                Thread(updateProgressRunnable).start()
            } else {
                showRecordingCompletedDialog()
                waveRecorder2.stopRecording()
                binding.startStopButton.text = getString(R.string.start)
            }
        }

        binding.imageView.setOnClickListener {
            if (!waveRecorder2.isRecording()) {
                val intent = Intent(this, ImageViewActivity::class.java)
                intent.putExtra(
                    "IMAGE_PATH",
                    availableImages[currentImageIndex.mod(availableImages.size)].localURl
                )
                startActivity(intent)
            }
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

        AppRoomDatabase.databaseWriteExecutor.execute {
            val value = AppRoomDatabase.INSTANCE?.ConfigurationDao()
                ?.getConfiguration()?.maximumBackgroundNoiseLevel
            if (value != null) {
                maxBackgroundNoiseLevel = value
            }
        }
    }

    private val amplitudeLeveRunnable = Runnable {
        var recordedSilentDuration = 0
        while (!Thread.currentThread().isInterrupted) {
            runOnUiThread {
                val percentage = (waveRecorder2.averageAmplitude / 1500 * 100).toInt()
                binding.progressBar.progress = percentage
            }

            if (waveRecorder2.averageAmplitude < maxBackgroundNoiseLevel) {
                recordedSilentDuration++
            } else {
                recordedSilentDuration = 0
            }

            // If the background is silent for 3 seconds, enable continue button.
            runOnUiThread {
                if (!waveRecorder2.isRecording()) {
                    binding.startStopButton.isEnabled =
                        recordedSilentDuration >= 10 * backgroundNoiseCheckDurationInSec

                    if (recordedSilentDuration >= 10 * backgroundNoiseCheckDurationInSec) {
                        binding.startStopButton.text = "Start"
                    } else {
                        binding.startStopButton.text = "Too Noisy"
                    }
                }
            }
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun loadImages() {
        val currentParticipantId = currentParticipant?.id
        AppRoomDatabase.databaseWriteExecutor.execute {
            val audios: List<Audio>? = if (currentParticipantId != null) {
                AppRoomDatabase.INSTANCE?.AudioDao()?.getAudiosByParticipant(currentParticipantId)
            } else {
                AppRoomDatabase.INSTANCE?.AudioDao()?.getAudiosByUser()
            }

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

            runOnUiThread {
                binding.previousImageButton.isEnabled = availableImages.isNotEmpty()
                binding.nextImageButton.isEnabled = availableImages.isNotEmpty()
                binding.startStopButton.isEnabled = availableImages.isNotEmpty()
            }
        }
    }

    private fun saveAudioIntoFile() {
        val currentImage = availableImages[currentImageIndex.mod(availableImages.size)]
        val fileName = currentUser?.locale + "_image_" + currentImage.remoteId.toString().padStart(
            4, '0'
        ) + "_u${currentUser?.id}_${currentImage.descriptionCount + 1}_${System.currentTimeMillis()}.wav"

        val duration = waveRecorder2.audioDuration()
        val result = waveRecorder2.saveAudioIntoFile(fileName)
        if (result != null && currentUser != null && duration >= 0) {
            val description = currentImage.name
            val audio = Audio(
                userId = currentUser!!.id,
                timestamp = System.currentTimeMillis(),
                remoteImageID = currentImage.remoteId,
                localFileURl = result,
                localImageURl = currentImage.localURl,
                description = description,
                duration = duration,
                deviceId = deviceId,
                environment = environment!!
            )
            if (currentParticipant != null) {
                audio.participantId = currentParticipant!!.id
            }

            // Insert audio and convert to mp3
            AppRoomDatabase.databaseWriteExecutor.execute {
                val id = AppRoomDatabase.getDatabase(application)?.AudioDao()?.insertAudio(audio)
                if (id != null) {
                    audio.id = id
                    // Convert to mp3
                    AudioUtil.convert(audio, application)
                }
            }

            availableImages.remove(currentImage)

            // Increase image description count
            currentImage.descriptionCount += 1
            viewModel.updateImage(currentImage)

            totalDescriptionCount += 1
            viewModel.descriptionCount.value = totalDescriptionCount

            // Reset Recording box control
            resetTimerLabel()

            if (availableImages.isEmpty()) {
                done()
            }

            showImageAtIndex(++currentImageIndex)

            Toast.makeText(this, "Audio saved.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio save failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun done() {
        if (currentParticipant != null && totalDescriptionCount > 0) {
            startActivity(Intent(this, ParticipantCompensationDetailsActivity::class.java))
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
        val dialogBinding = RecorderBottomSheetDialogLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)

        // Playback
        dialogBinding.playStopButton?.setOnClickListener {
            if (!waveRecorder2.isAudioPlaying()) {
                waveRecorder2.playBackRecording()
                dialogBinding.playStopButton.text = getString(R.string.stop)
            } else {
                waveRecorder2.stopPlayback()
                dialogBinding.playStopButton.text = getString(R.string.play)
            }
        }

        // Save
        dialogBinding.saveButton.setOnClickListener {
            // Stop play in case the user left it playing before exiting
            waveRecorder2.stopPlayback()

            saveAudioIntoFile()
            dialog.dismiss()
            resetTimerLabel()
            waveRecorder2.reset()
        }

        // delete
        dialogBinding.deleteButton.setOnClickListener {
            // Audio is written to only a temporary location.
            // Next recording will overwrite, no need to perform an actual deletion.

            // Stop play in case the user left it playing before exiting
            waveRecorder2.stopPlayback()

            dialog.dismiss()
            resetTimerLabel()
        }
        dialog.show()
    }

    private fun resetTimerLabel() {
        binding.timerLabel.text = "00:00"
        binding.timerLabel.setTextColor(getColor(R.color.text_color))
    }

    private fun showRecordingCompletedDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("Recording completed").setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                resetTimerLabel()
                waveRecorder2.stopRecording()
                waveRecorder2.reset()
            }

        var message = ""
        if (waveRecorder2.silentDuration() >= allowedPauseDuration) {
            message = "Please the recording contains too many lapses."
        } else if (waveRecorder2.audioDuration() < requiredAudioDuration) {
            message = "Please record for at least $requiredAudioDuration seconds."
        } else if (waveRecorder2.audioDuration() > maxAudioDuration) {
            message = "Please audio should be less than or equal to $maxAudioDuration seconds."
        } else {
            showSaveDeleteBottomSheetDialog()
        }

        // Show error dialog box
        if (waveRecorder2.audioDuration() < requiredAudioDuration || waveRecorder2.audioDuration() > maxAudioDuration || waveRecorder2.silentDuration() >= allowedPauseDuration) {
            dialog.setMessage(message)
            dialog.create()
            dialog.show()
        }
    }

    private fun showErrorDialog(message: String) {
        val dialog = AlertDialog.Builder(this).setTitle("Error")
            .setPositiveButton("Exit") { _, _ -> finish() }
        dialog.setMessage(message)
        dialog.create()
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private val updateProgressRunnable = Runnable {
        while (true) {
            runOnUiThread {
                // Update timer text
                val duration = waveRecorder2.audioDuration()
                val minutes = (duration / 60).toInt().toString().padStart(2, '0')
                val seconds = (duration % 60).toString().padStart(2, '0')
                binding.timerLabel.text = "${minutes}:${seconds}"

                if (duration > maxAudioDuration) {
                    waveRecorder2.stopRecording()
                    binding.startStopButton.text = getString(R.string.start)
                    showRecordingCompletedDialog()
                }

                // Check audio duration
                if (duration >= maxAudioDuration || waveRecorder2.silentDuration() >= allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(200, 50, 50))
                } else if (duration >= requiredAudioDuration && waveRecorder2.silentDuration() <= allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(50, 200, 50))
                } else {
                    binding.timerLabel.setTextColor(getColor(R.color.text_color))
                }

                // Warn user to stop
                if (duration >= (maxAudioDuration - 5) && waveRecorder2.silentDuration() < allowedPauseDuration) {
                    binding.timerLabel.setTextColor(Color.rgb(200, 150, 50))
                }
            }
            Thread.sleep(100)
            if (!waveRecorder2.isRecording()) {
                break
            }
        }
    }

    private fun showImageAtIndex(index: Int) {
        if (waveRecorder2.isRecording() || availableImages.size < 1) return

        resetTimerLabel()

        val currentImage = availableImages[index.mod(availableImages.size)]
        val options: RequestOptions =
            RequestOptions().fitCenter().placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
        val imageUrl =
            if (currentImage.localURl != null) currentImage.localURl else currentImage.remoteURL
        Glide.with(this).load(imageUrl).apply(options)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imageView)

        // Update image details
        binding.imageName.text = currentImage.name
        binding.imageCategory.text = currentImage.category
    }

    private fun permissionsDenied(): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

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

    override fun onResume() {
        super.onResume()
        amplitudeLevelThread?.interrupt()

        amplitudeLevelThread = Thread(amplitudeLeveRunnable)
        amplitudeLevelThread?.start()
    }

    override fun onPause() {
        super.onPause()
        amplitudeLevelThread?.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        waveRecorder2.release()
    }
}