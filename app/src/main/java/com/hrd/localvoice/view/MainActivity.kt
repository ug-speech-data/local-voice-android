package com.hrd.localvoice.view

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityMainBinding
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.utils.Functions.Companion.getPathFromUri
import com.hrd.localvoice.view.authentication.LoginActivity
import com.hrd.localvoice.view.local_files.MyAudiosActivity
import com.hrd.localvoice.view.local_files.MyImagesActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity
import com.hrd.localvoice.workers.UpdateAssignedImagesWorker
import com.hrd.localvoice.workers.UpdateConfigurationWorker
import com.hrd.localvoice.workers.UploadWorker
import java.io.File
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_CODE = 4
    private val PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )
    private val tag = "TESTMAIN"
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        // If new user, redirect to login
        val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        val isNew = prefs.getBoolean(Constants.IS_NEW_USER, true)
        val token = prefs.getString(Constants.USER_TOKEN, "")
        if (isNew || token == null || token.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Initialise the database
        AppRoomDatabase.getDatabase(application)

        // Request storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE)
        }

        // Attach listener to download manager
        registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        );

        // Open Audio Recorder activity
        binding.recordingCard.setOnClickListener {
            startActivity(Intent(this, ParticipantBioActivity::class.java))
        }

        // Open recorded audios activity
        binding.audiosCard.setOnClickListener {
            startActivity(Intent(this, MyAudiosActivity::class.java))
        }

        // Open downloaded images acivity
        binding.imagesCard.setOnClickListener {
            startActivity(Intent(this, MyImagesActivity::class.java))
        }

        //Update tasks load info
        // Fetch maximum description count from db
        viewModel.getConfiguration()?.observe(this) { configuration ->
            if (configuration == null || !File(configuration.demoVideoLocalUrl).exists()) {
                binding.appStatusInfo.text = getString(R.string.outdated_config_info)
                binding.appStatusInfo.setTextColor(Color.rgb(200, 50, 50))
            } else {
                binding.appStatusInfo.setTextColor(Color.rgb(50, 200, 50))
                binding.appStatusInfo.text = getString(R.string.configurations_set)
            }

            val maxImageDescription =
                if (configuration?.maxImageDescriptionCount != null) configuration.maxImageDescriptionCount else 3

            // Get images without required number of descriptions
            viewModel.getImages(maxImageDescription!!)?.observe(this) { images ->
                binding.assignedImagesInfo.text = "${images.size} Pending Images"
                binding.expectedDescriptionCount.text =
                    "About ${images.size * maxImageDescription} Descriptions"
            }
        }

        // Schedule audio upload.
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresBatteryNotLow(true)
        }.build()

        val workManager = WorkManager.getInstance(application)
        val updateConfigurationRequest = OneTimeWorkRequestBuilder<UpdateConfigurationWorker>()
            .setConstraints(constraints).setInputData(createInputDataForUri()).build()
        workManager.enqueue(updateConfigurationRequest)

        // Attach listener to update local images button
        binding.updateLocalImages.setOnClickListener {
            val workRequest = OneTimeWorkRequestBuilder<UpdateAssignedImagesWorker>()
                .setConstraints(constraints).setInputData(createInputDataForUri()).build()
            workManager.enqueue(workRequest)
            binding.updateLocalImages.isEnabled = false

            Toast.makeText(
                this@MainActivity,
                getString(R.string.scheduled_local_image_update),
                Toast.LENGTH_LONG
            ).show()
        }

        // Enqueue upload worker
        val uploadWorker =
            PeriodicWorkRequest.Builder(UploadWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(createInputDataForUri())
                .build()
        workManager.enqueueUniquePeriodicWork(
            "AudioUploadWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            uploadWorker
        )

        // Tests

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

    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        builder.putString("KEY_IMAGE_URI", "imageUri.toString()")
        return builder.build()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE).edit()
            prefs.remove(Constants.IS_NEW_USER)
            prefs.remove(Constants.USER_TOKEN)
            prefs.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
            val downloadedUri = downloadManager?.getUriForDownloadedFile(id)

            if (context != null && downloadedUri != null) {
                val downloadedFilePath = getPathFromUri(context, downloadedUri)
                if (downloadedFilePath != null) {
                    var fileNumber =
                        downloadedFilePath.split("-")[downloadedFilePath.split("-").size - 1]
                    fileNumber = fileNumber.split(".")[0]
                    if (fileNumber.toIntOrNull() != null) {
                        // Duplicate file name, delete old file.
                        val newFileName = downloadedFilePath.replace("-$fileNumber", "")
                        val newFile = File(newFileName)
                        if (newFile.exists()) {
                            newFile.delete()
                        }
                        val file = File(downloadedFilePath)
                        file.renameTo(newFile)
                        Log.d(tag, "Renamed ${file.absolutePath} to $newFile")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }
}