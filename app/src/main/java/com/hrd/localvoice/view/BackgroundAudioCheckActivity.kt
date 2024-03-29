package com.hrd.localvoice.view

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityBackgroundAudioCheckBinding
import com.hrd.localvoice.utils.WaveRecorder
import com.hrd.localvoice.view.audiorecorder.AudioRecorderActivity

class BackgroundAudioCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackgroundAudioCheckBinding
    private val permissionRequestCode = 4
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
    )
    private lateinit var recorder: WaveRecorder
    private var backgroundNoiseCheckDurationInSec = 3
    private var maxBackgroundNoiseLevel = 350

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackgroundAudioCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.background_check)

        // Check for audio recording permission
        if (permissionsDenied()) {
            requestPermissions(permissions, permissionRequestCode)
        }

        recorder = WaveRecorder(this, false)
        recorder.startRecording()

        AppRoomDatabase.databaseWriteExecutor.execute {
            val value = AppRoomDatabase.INSTANCE?.ConfigurationDao()
                ?.getConfiguration()?.maximumBackgroundNoiseLevel
            if (value != null) {
                maxBackgroundNoiseLevel = value
            }
            Thread(monitorBackgroundNoise).start()
        }

        binding.continueButton.setOnClickListener {
            val intent = Intent(this, AudioRecorderActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private val monitorBackgroundNoise = Runnable {
        var duration = 0
        while (true) {
            runOnUiThread {
                if (duration >= 10 * backgroundNoiseCheckDurationInSec) {
                    binding.statusInfo.text = getString(R.string.silent_environment_message)
                } else {
                    binding.statusInfo.text = getString(R.string.noisy_environment_message)
                }
                val percentage = (recorder.averageAmplitude / 1500 * 100).toInt()
                binding.progressBar.progress = percentage
            }

            if (recorder.averageAmplitude < maxBackgroundNoiseLevel) {
                duration++
            } else {
                duration = 0
            }

            // If the background is silent for 5 seconds, enable continue button.
            runOnUiThread {
                binding.continueButton.isEnabled =
                    duration >= 10 * backgroundNoiseCheckDurationInSec
            }

            Thread.sleep(100)
            if (!recorder.isRecording()) {
                break
            }
        }
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
            Toast.makeText(this, "Perm Denied", Toast.LENGTH_LONG).show()
            (this.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            finish()
        } else {
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        recorder = WaveRecorder(this, false)
        recorder.startRecording()
    }

    override fun onPause() {
        super.onPause()
        recorder.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}