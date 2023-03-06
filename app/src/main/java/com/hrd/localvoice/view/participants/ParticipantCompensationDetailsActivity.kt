package com.hrd.localvoice.view.participants

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.components.DigitInput
import com.hrd.localvoice.databinding.ActivityParticipantCompensationDetailsBinding
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.DoneActivity

class ParticipantCompensationDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityParticipantCompensationDetailsBinding
    private lateinit var viewModel: ParticipantBioActivityViewModel
    private var currentParticipant: Participant? = null

    // Momo number
    private var momoNumber = ""
    private var momoNumberConfirm = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticipantCompensationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[ParticipantBioActivityViewModel::class.java]

        // Show back button
        title = "Momo Details"

        AppRoomDatabase.databaseWriteExecutor.execute {
            // Retrieve current participant
            val prefsEditor: SharedPreferences =
                getSharedPreferences(Constants.SHARED_PREFS_FILE, MODE_PRIVATE)
            val participantId =
                prefsEditor.getLong(ParticipantBioActivity.currentParticipantData, -1)

            currentParticipant = viewModel.getParticipantById(participantId)

            if (currentParticipant == null) {
                Toast.makeText(this, "No participant found", Toast.LENGTH_LONG).show()
            }
        }

        binding.saveButton.setOnClickListener {
            if (saveDetails()) {
                startActivity(Intent(this, DoneActivity::class.java))
                finish()
            }
        }

        // Insert phone digit input
        binding.phoneDigitInput.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    DigitInput(title = "Momo Number") {
                        momoNumber = it
                    }
                }
            }
        }

        binding.phoneDigitInput2.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    DigitInput(title = "Confirm Momo Number") {
                        momoNumberConfirm = it
                        if (momoNumber != momoNumberConfirm) {
                            binding.phoneErrorLabel.visibility = View.VISIBLE
                            binding.phoneErrorLabel.text =
                                context.getString(R.string.numbers_do_not_match)
                        } else {
                            binding.phoneErrorLabel.text =
                                getString(R.string.enter_a_validate_10_digit_ghanaian_momo_number)
                            binding.phoneErrorLabel.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun saveDetails(): Boolean {
        // Phone
        val phone = momoNumber
        var phoneIsValid = false
        if (phone.length != 10 || !phone.startsWith("0")) {
            binding.phoneErrorLabel.visibility = View.VISIBLE
            phoneIsValid = false
        } else if (momoNumber == momoNumberConfirm) {
            binding.phoneErrorLabel.visibility = View.GONE
            phoneIsValid = true
        }

        // Network
        var network = ""
        val selectedId: Int = binding.networkGroup.checkedRadioButtonId
        if (selectedId == -1) {
            binding.networkErrorLabel.visibility = View.VISIBLE
        } else {
            val radioButton: RadioButton = binding.networkGroup.findViewById(selectedId)
            network = radioButton.text.toString()
            binding.networkErrorLabel.visibility = View.GONE
        }

        // Full name
        val fullName = binding.fullNameInput.text.toString()
        if (fullName.isEmpty()) {
            binding.fullNameErrorLabel.visibility = View.VISIBLE
        } else {
            binding.fullNameErrorLabel.visibility = View.GONE
        }

        if (phoneIsValid && network.isNotEmpty() && fullName.isNotEmpty() && currentParticipant != null) {
            currentParticipant!!.momoNumber = phone
            currentParticipant!!.network = network
            currentParticipant!!.fullname = fullName

            AppRoomDatabase.databaseWriteExecutor.execute {
                AppRoomDatabase.INSTANCE?.ParticipantDao()?.updateParticipant(currentParticipant!!)
            }
            return true
        } else {
            Toast.makeText(this, "Please fix the errors.", Toast.LENGTH_LONG).show()
        }
        return false;
    }
}