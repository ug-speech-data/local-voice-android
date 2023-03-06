package com.hrd.localvoice.view.configurations

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityConfigurationBinding
import com.hrd.localvoice.workers.UpdateConfigurationWorker

class ConfigurationActivity : AppCompatActivity() {
    lateinit var binding: ActivityConfigurationBinding
    private lateinit var viewModel: ConfigurationActivityViewModel

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ConfigurationActivityViewModel::class.java]

        viewModel.getConfiguration()?.observe(this) { configuration ->
            if (configuration == null) return@observe

            binding.demoVideo.text = configuration.demoVideoRemoteUrl
            binding.privacyStatementAudio.text = configuration.privacyPolicyStatementAudioRemoteUrl
            binding.maximumNoiseLevel.text = configuration.maximumBackgroundNoiseLevel.toString()

            if (configuration.demoVideoLocalUrl.isEmpty()) {
                binding.demoVideo.setTextColor(getColor(android.R.color.holo_red_light))
            }

            if (configuration.privacyPolicyStatementAudioLocalUrl?.isEmpty() == true) {
                binding.privacyStatementAudio.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.configuration_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_update_configuration -> scheduleConfigurationUpdate()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scheduleConfigurationUpdate() {
        // Background work manager
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()
        val workManager = WorkManager.getInstance(application)
        // Update configurations and user profile
        val updateConfigurationRequest =
            OneTimeWorkRequestBuilder<UpdateConfigurationWorker>().setConstraints(constraints)
                .build()
        workManager.enqueue(updateConfigurationRequest)
        Toast.makeText(this, "Scheduled update", Toast.LENGTH_SHORT).show()
    }
}