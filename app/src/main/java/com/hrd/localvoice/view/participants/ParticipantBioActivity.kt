package com.hrd.localvoice.view.participants

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityParticipantBioBinding
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.VideoPlayerActivity


class ParticipantBioActivity : AppCompatActivity() {
    private lateinit var viewModel: ParticipantBioActivityViewModel

    companion object {
        const val currentParticipantData = "com.hrd.localvoice.currentParticipant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityParticipantBioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.participant_bio)

        viewModel = ViewModelProvider(this)[ParticipantBioActivityViewModel::class.java]
        var environment = ""
        val deviceId = Build.MANUFACTURER + " " + Build.MODEL

        binding.nextButton.setOnClickListener {
            // Age
            val age = binding.ageInput.text.toString()
            if (age.isEmpty()) {
                binding.ageErrorLabel.visibility = View.VISIBLE
            } else {
                binding.ageErrorLabel.visibility = View.GONE
            }

            // Gender
            var gender = ""
            val selectedId: Int = binding.genderGroup.checkedRadioButtonId
            if (selectedId == -1) {
                binding.genderErrorLabel.visibility = View.VISIBLE
            } else {
                val radioButton: RadioButton = binding.genderGroup.findViewById(selectedId)
                gender = radioButton.text.toString()
                binding.genderErrorLabel.visibility = View.GONE
            }

            // Locale
            var locale = ""
            val selectedLocaleId: Int = binding.localeGroup.checkedRadioButtonId
            if (selectedLocaleId == -1) {
                binding.localeErrorLabel.visibility = View.VISIBLE
            } else {
                val radioButton: RadioButton = binding.localeGroup.findViewById(selectedLocaleId)
                locale = radioButton.text.toString()
                binding.localeErrorLabel.visibility = View.GONE
            }

            if (environment.isEmpty()) {
                binding.environmentErrorLabel.visibility = View.VISIBLE
            } else {
                binding.environmentErrorLabel.visibility = View.GONE
            }

            if (age.isNotEmpty() && gender.isNotEmpty() && locale.isNotEmpty() && environment.isNotEmpty()) {
                val participant = Participant(
                    age.toInt(), gender, null, null, environment = environment,
                    locale = locale,
                    deviceId = deviceId
                )
                viewModel.createParticipant(participant)
                Thread(saveLastParticipantInSharedPreferences).start()

                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        }

        // Populate the environment spinner
        val environments = arrayOf("Outdoor", "Office", "In a car")
        val adapter: ArrayAdapter<*> =
            ArrayAdapter<Any?>(this, android.R.layout.simple_spinner_item, environments)
        binding.environmentSpinner.adapter = adapter
        binding.environmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    environment = environments[position]
                }
            }
    }

    private val saveLastParticipantInSharedPreferences = Runnable {
        val participant = AppRoomDatabase.INSTANCE?.ParticipantDao()?.getLastParticipant()
        if (participant != null) {
            // Store this id store shared preference in retrieval in recording activity
            val prefsEditor: SharedPreferences =
                getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
            prefsEditor.edit().putLong(currentParticipantData, participant.id).apply()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}