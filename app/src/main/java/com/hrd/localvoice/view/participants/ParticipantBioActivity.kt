package com.hrd.localvoice.view.participants

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityParticipantBioBinding
import com.hrd.localvoice.fragments.PrivacyPolicyBottomSheet
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.videoplayer.VideoPlayerActivity


class ParticipantBioActivity : AppCompatActivity() {
    private lateinit var viewModel: ParticipantBioActivityViewModel
    private var configuration: Configuration? = null

    companion object {
        const val currentParticipantData = "com.hrd.localvoice.currentParticipant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityParticipantBioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[ParticipantBioActivityViewModel::class.java]

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.participant_bio)

        viewModel.configuration?.observe(this) {
            configuration = it
        }

        var environment = ""

        // Privacy policy
        binding.privacyPolicyLabel.setOnClickListener {
            showPrivacyPolicyBottomSheetDialog()
        }

        binding.nextButton.setOnClickListener {
            // Age
            val age = binding.ageInput.text.toString().toIntOrNull()
            val ageIsValid: Boolean;
            if ((age == null) || (age.toString().length != 2)) {
                binding.ageErrorLabel.visibility = View.VISIBLE
                ageIsValid = false
            } else {
                binding.ageErrorLabel.visibility = View.GONE
                ageIsValid = true
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

            if (environment.isEmpty()) {
                binding.environmentErrorLabel.visibility = View.VISIBLE
            } else {
                binding.environmentErrorLabel.visibility = View.GONE
            }

            // Privacy Policy
            val checkedPrivacyPolicy = binding.privacyPolicyCheckBox.isChecked
            if (!checkedPrivacyPolicy) {
                binding.privacyPolicyErrorLabel.visibility = View.VISIBLE
            } else {
                binding.privacyPolicyErrorLabel.visibility = View.GONE
            }

            if (ageIsValid && gender.isNotEmpty() && environment.isNotEmpty() && checkedPrivacyPolicy) {
                val participant = Participant(
                    age!!, gender, null, null, environment = environment,
                    acceptedPrivacyPolicy = checkedPrivacyPolicy
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

    private fun showPrivacyPolicyBottomSheetDialog() {
        val modalBottomSheet = PrivacyPolicyBottomSheet()
        modalBottomSheet.show(supportFragmentManager, PrivacyPolicyBottomSheet.TAG)
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