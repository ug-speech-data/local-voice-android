package com.hrd.localvoice.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.BuildConfig
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityMainBinding
import com.hrd.localvoice.databinding.LayoutSkipWarningBinding
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.view.authentication.LoginActivity
import com.hrd.localvoice.view.authentication.ProfileActivity
import com.hrd.localvoice.view.configurations.ConfigurationActivity
import com.hrd.localvoice.view.local_files.MyAudiosActivity
import com.hrd.localvoice.view.local_files.MyImagesActivity
import com.hrd.localvoice.view.participants.ParticipantBioActivity
import com.hrd.localvoice.view.validations.AssignedAudiosActivity
import com.hrd.localvoice.view.videoplayer.VideoPlayerActivity
import com.hrd.localvoice.workers.UpdateAssignedImagesWorker
import com.hrd.localvoice.workers.UpdateConfigurationWorker
import com.hrd.localvoice.workers.UploadWorker
import java.io.File
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainActivityViewModel
    private var user: User? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        val preferences: SharedPreferences =
            getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)

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
                    title = "Hi ${user?.emailAddress}"

                    if ((it?.network == null || it.age == null || it.gender == null || it.environment == null)) {
                        Toast.makeText(this, "Please update your profile", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        startActivity(intent)
                    } else {
                        binding.balanceView.text = getString(R.string.balance, user?.balance);
                        binding.audiosSubmittedView.text = user?.audiosSubmitted.toString()
                        binding.audiosValidatedView.text = user?.audiosValidated.toString()

                        binding.audiosPendingView.text = user?.audiosPending.toString()

                        binding.audiosRejectedView.text = user?.audiosRejected.toString()

                        binding.audiosAcceptedView.text = user?.audiosAccepted.toString()

                        binding.balanceDeductionView.text = getString(
                            R.string.balance_deduction, user?.estimatedDeductionAmount.toString()
                        );
                    }

                    // Hide/Show Audio validation button
                    if (user?.permissions?.contains("validate_audio") != true) {
                        binding.audioValidationCard.visibility = View.GONE
                    } else {
                        binding.audioValidationCard.visibility = View.VISIBLE
                    }

                    if (user?.permissions?.contains("record_self") != true) {
                        binding.balanceView.visibility = View.GONE
                    }
                }
            }
        }

        // Initialise the database
        AppRoomDatabase.getDatabase(application)

        // Schedule configuration update
        scheduleConfigurationUpdate(constraints, workManager)

        // Attach listener to update configurations
        binding.appStatusInfo.setOnClickListener {
            startActivity(Intent(this, ConfigurationActivity::class.java))
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

            // Show/Hide record depending on user's permission
            if (user?.permissions?.contains("record_others") == true || user?.permissions?.contains(
                    "record_self"
                ) == true
            ) {
                binding.recordingCard.visibility = View.VISIBLE
            } else {
                binding.recordingCard.visibility = View.GONE
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
            startActivity(Intent(this, AssignedAudiosActivity::class.java))
        }

        // Fetch maximum description count from db
        viewModel.getConfiguration()?.observe(this) { configuration ->
            if (configuration == null || !File(configuration.demoVideoLocalUrl).exists() || !File(
                    configuration.privacyPolicyStatementAudioLocalUrl
                ).exists()
            ) {
                binding.appStatusInfo.text = getString(R.string.outdated_config_info)
                binding.appStatusInfo.setTextColor(Color.rgb(200, 50, 50))
            } else {
                binding.appStatusInfo.setTextColor(Color.rgb(50, 200, 50))
                binding.appStatusInfo.text = getString(R.string.configurations_set)
            }

            // Alert if current server apk doesn't match
            if (configuration?.currentAPKVersion?.isNotEmpty() == true && BuildConfig.VERSION_NAME != configuration.currentAPKVersion && preferences.getString(
                    Constants.IGNORED_UPDATE_VERSION, ""
                ) != configuration.currentAPKVersion
            ) {
                showUpdateAlert(configuration)
            }
        }

        // Get images without required number of descriptions
        viewModel.getImages()?.observe(this) { images ->
            binding.assignedImagesInfo.text = "${images.size} Assigned Images"
        }

        // Update recorded descriptions
        viewModel.getAudios()?.observe(this) { audios ->
            binding.recordedAudioLabel.text = getString(R.string.recorded_audios, audios.size)
        }

        // Downloaded validations
        viewModel.getValidationAudios()?.observe(this) { audios ->
            binding.audioValidationTextLabel.text =
                getString(R.string.audios_to_validate, audios.size)
        }

        // Attach listener to update local images button
        binding.updateLocalImages.setOnClickListener {
            val workRequest =
                OneTimeWorkRequestBuilder<UpdateAssignedImagesWorker>().setConstraints(
                    constraints
                ).setInputData(createInputDataForUri()).build()
            workManager.enqueue(workRequest)

            Toast.makeText(
                this@MainActivity,
                getString(R.string.scheduled_local_image_update),
                Toast.LENGTH_LONG
            ).show()
        }

        // Enqueue upload workers
        val uploadWorker =
            PeriodicWorkRequest.Builder(UploadWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).setInputData(createInputDataForUri()).build()
        workManager.enqueueUniquePeriodicWork(
            "AudioUploadWorker", ExistingPeriodicWorkPolicy.UPDATE, uploadWorker
        )

        val updateConfiguration =
            PeriodicWorkRequest.Builder(UpdateConfigurationWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(
            "UpdateConfiguration", ExistingPeriodicWorkPolicy.UPDATE, updateConfiguration
        )

        binding.refreshButton.setOnClickListener {
            workManager.enqueueUniquePeriodicWork(
                "UpdateConfiguration", ExistingPeriodicWorkPolicy.UPDATE, updateConfiguration
            )
            Toast.makeText(this, "Update requested.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpdateAlert(configuration: com.hrd.localvoice.models.Configuration) {
        val ignoreCheckBox = LayoutSkipWarningBinding.inflate(layoutInflater)
        var doDoNotShow = false
        ignoreCheckBox.checkbox.setOnCheckedChangeListener { _, isChecked ->
            doDoNotShow = isChecked
        }
        ignoreCheckBox.checkbox.text = getString(R.string.do_not_show_again)
        val dialog = AlertDialog.Builder(this).setTitle("NEW UPDATE").setCancelable(true)
            .setView(ignoreCheckBox.root).setNegativeButton("CANCEL") { _, _ ->
                if (doDoNotShow) {
                    val editPref = getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE).edit()
                    editPref.putString(
                        Constants.IGNORED_UPDATE_VERSION,
                        configuration.currentAPKVersion
                    ).apply()
                }
            }.setPositiveButton("DOWNLOAD") { _, _ ->
                val uri: Uri = Uri.parse(configuration.apkLink)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            .setMessage("App version ${configuration.currentAPKVersion} is available. Download to update.")

        dialog.create()
        dialog.show()
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
            }.setPositiveButton("OTHERS") { _, _ ->
                startActivity(Intent(this, ParticipantBioActivity::class.java))
            }.setMessage("Who is the speaker?")

        dialog.create()
        dialog.show()
    }

    private fun checkForUpdate() {
        val intent = Intent(Intent.ACTION_VIEW)
        var pathToApk = "/data/user/0/com.hrd.localvoice/files/localvoice-v15-release.apk"
        pathToApk = "/storage/emulated/0/Download/localvoice-v15-release.apk"

        val uri = FileProvider.getUriForFile(
            this@MainActivity, BuildConfig.APPLICATION_ID + ".provider", File(pathToApk)
        )

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent)
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
}