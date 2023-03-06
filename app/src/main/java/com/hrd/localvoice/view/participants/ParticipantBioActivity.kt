package com.hrd.localvoice.view.participants

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityParticipantBioBinding
import com.hrd.localvoice.fragments.PrivacyPolicyBottomSheet
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.BackgroundAudioCheckActivity
import com.hrd.localvoice.view.videoplayer.VideoPlayerActivity


class ParticipantBioActivity : AppCompatActivity() {
    private lateinit var viewModel: ParticipantBioActivityViewModel
    private var configuration: Configuration? = null
    private lateinit var binding: ActivityParticipantBioBinding
    private var previousParticipant: Participant? = null
    private var createdNewParticipant = false

    companion object {
        const val currentParticipantData = "com.hrd.localvoice.currentParticipant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticipantBioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[ParticipantBioActivityViewModel::class.java]

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.participant_bio)

        viewModel.configuration?.observe(this) {
            configuration = it
        }

        viewModel.getPendingParticipant()?.observe(this) { participant ->
            if (participant != null && !createdNewParticipant) {
                previousParticipant = participant
                showPendingParticipantDialog()
            }
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
                    age!!,
                    gender,
                    null,
                    null,
                    environment = environment,
                    acceptedPrivacyPolicy = true
                )
                AppRoomDatabase.databaseWriteExecutor.execute {
                    createdNewParticipant = true

                    val id =
                        AppRoomDatabase.INSTANCE?.ParticipantDao()?.insertParticipant(participant)
                    if (id != null) {
                        val prefsEditor: SharedPreferences =
                            getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
                        prefsEditor.edit().putLong(currentParticipantData, id).apply()
                        val intent = Intent(this, VideoPlayerActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Can't save participant's bio", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        // Populate the environment spinner
        val environments = Constants.ENVIRONMENTS
        val adapter: ArrayAdapter<*> =
            ArrayAdapter<Any?>(this, android.R.layout.simple_spinner_item, environments)
        binding.environmentSpinner.adapter = adapter
        binding.environmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    environment = environments[position]
                }
            }
    }

    private fun showPendingParticipantDialog() {
        val message =
            "You did not finish the session with the previous participant." + " \nWould you like to continue or delete the previous participant's audios?"
        val dialog =
            AlertDialog.Builder(this).setTitle("Continue?").setMessage(message).setCancelable(false)
                .setNegativeButton(getString(R.string.delete)) { _, _ ->
                    showDeletionSnackBar()
                }.setPositiveButton(getString(R.string.string_continue)) { _, _ ->

                    // Store this id store shared preference in retrieval in recording activity
                    val prefsEditor: SharedPreferences =
                        getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
                    prefsEditor.edit().putLong(currentParticipantData, previousParticipant!!.id)
                        .apply()

                    // Launch background check
                    val intent = Intent(this, BackgroundAudioCheckActivity::class.java)
                    startActivity(intent)
                    finish()
                }
        dialog.create()
        dialog.show()
    }

    private fun showDeletionSnackBar() {
        val snack = Snackbar.make(
            binding.root, "Previous participant's audios deleted.", Snackbar.LENGTH_LONG
        )
        snack.setAction("UNDO", {})
        snack.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onShown(transientBottomBar: Snackbar?) {
                super.onShown(transientBottomBar)
            }

            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    // Perform actual deletion
                    if (previousParticipant != null) {
                        AppRoomDatabase.databaseWriteExecutor.execute {
                            // Delete audios
                            AppRoomDatabase.INSTANCE?.AudioDao()
                                ?.deleteAudiosFromParticipantId(previousParticipant!!.id)

                            // Delete participant
                            AppRoomDatabase.INSTANCE?.ParticipantDao()
                                ?.deleteParticipant(previousParticipant!!)
                        }
                    }
                } else{
                    recreate()
                }
            }
        })
        snack.show()
    }

    private fun showPrivacyPolicyBottomSheetDialog() {
        val modalBottomSheet = PrivacyPolicyBottomSheet()
        modalBottomSheet.show(supportFragmentManager, PrivacyPolicyBottomSheet.TAG)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}