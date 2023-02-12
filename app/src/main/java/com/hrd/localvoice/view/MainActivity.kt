package com.hrd.localvoice.view

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityMainBinding
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.utils.Functions.Companion.getPathFromUri
import com.hrd.localvoice.view.authentication.LoginActivity
import com.hrd.localvoice.view.authentication.ProfileActivity
import com.hrd.localvoice.view.local_files.MyAudiosActivity
import com.hrd.localvoice.view.local_files.MyImagesActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity
import com.hrd.localvoice.view.validations.AudioValidationActivity
import com.hrd.localvoice.view.videoplayer.VideoPlayerActivity
import com.hrd.localvoice.workers.UpdateAssignedImagesWorker
import com.hrd.localvoice.workers.UpdateConfigurationWorker
import com.hrd.localvoice.workers.UploadWorker
import java.io.File
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val tag = "TESTMAIN"
    private lateinit var viewModel: MainActivityViewModel
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        // Background work manager
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()
        val workManager = WorkManager.getInstance(application)


        // If new user, redirect to login
        val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        val isNew = prefs.getBoolean(Constants.IS_NEW_USER, true)
        val token = prefs.getString(Constants.USER_TOKEN, "")
        if (isNew || token == null || token.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            viewModel.user?.observe(this) {
                user = it
                if (user != null) {
                    if ((it?.network == null || it.age == null || it.gender == null || it.environment == null)) {
                        Toast.makeText(this, "Please update your profile", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        startActivity(intent)
                    } else {
                        binding.balanceView.text = getString(R.string.balance, user?.balance);
                        binding.audiosSubmittedView.text =
                            getString(R.string.audios_submitted, user?.audiosSubmitted);
                        binding.audiosValidatedView.text =
                            getString(R.string.audios_validated, user?.audiosValidated);
                    }
                }
            }
        }

        // Initialise the database
        AppRoomDatabase.getDatabase(application)

        // Schedule configuration update
        scheduleConfigurationUpdate(constraints, workManager)

        // Attach listener to download manager
        registerReceiver(
            onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        );

        // Attach listener to update configurations
        binding.appStatusInfo.setOnClickListener {
            scheduleConfigurationUpdate(constraints, workManager)
        }

        // Open Audio Recorder activity
        binding.recordingCard.setOnClickListener {
            if (user?.permissions?.contains("record_others") == true && user?.permissions?.contains(
                    "record_self"
                ) == true
            ) {
                showActionAlertDialogBox()
            } else if (user?.permissions?.contains("record_others") == true) {
                startActivity(Intent(this, ParticipantBioActivity::class.java))
            } else if (user?.permissions?.contains("record_self") == true) {
                // Remove previous participant cache.
                val pref = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE).edit()
                pref.remove(ParticipantBioActivity.currentParticipantData)
                pref.apply()

                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
        }

        // Open recorded audios activity
        binding.audiosCard.setOnClickListener {
            startActivity(Intent(this, MyAudiosActivity::class.java))
        }

        // Open downloaded images activity
        binding.imagesCard.setOnClickListener {
            startActivity(Intent(this, MyImagesActivity::class.java))
        }

        // Open audio validation activity
        binding.audioValidationCard.setOnClickListener {
            startActivity(Intent(this, AudioValidationActivity::class.java))
        }

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

        // Attach listener to update local images button
        binding.updateLocalImages.setOnClickListener {
            val workRequest =
                OneTimeWorkRequestBuilder<UpdateAssignedImagesWorker>().setConstraints(constraints)
                    .setInputData(createInputDataForUri()).build()
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
                .setConstraints(constraints).setInputData(createInputDataForUri()).build()
        workManager.enqueueUniquePeriodicWork(
            "AudioUploadWorker", ExistingPeriodicWorkPolicy.REPLACE, uploadWorker
        )
    }

    private fun scheduleConfigurationUpdate(
        constraints: Constraints, workManager: WorkManager
    ) {
        // Update configurations and user profile
        val updateConfigurationRequest =
            OneTimeWorkRequestBuilder<UpdateConfigurationWorker>().setConstraints(constraints)
                .setInputData(createInputDataForUri()).build()
        workManager.enqueue(updateConfigurationRequest)
    }

    private fun showActionAlertDialogBox() {
        val dialog = AlertDialog.Builder(this).setTitle("SPEAKER").setCancelable(true)
            .setNegativeButton("MY SELF") { _, _ ->
                // Remove previous participant cache.
                val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE).edit()
                prefs.remove(ParticipantBioActivity.currentParticipantData)
                prefs.apply()

                // Launch demo activity.
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }.setPositiveButton("OTHERS") { _, _ ->
                startActivity(Intent(this, ParticipantBioActivity::class.java))
            }.setMessage("Who is the speaker?")

        dialog.create()
        dialog.show()
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
        if (item.itemId == R.id.action_profile) {
            startActivity(Intent(this, ProfileActivity::class.java))
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