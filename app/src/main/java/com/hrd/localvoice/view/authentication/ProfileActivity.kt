package com.hrd.localvoice.view.authentication

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityProfileBinding
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.MainActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: AuthenticationActivityViewModel
    private var configuration: Configuration? = null
    var environment = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Update Profile"

        // Privacy policy
        binding.privacyPolicyLabel.setOnClickListener {
            showPrivacyPolicyBottomSheetDialog()
        }

        // Configuration
        viewModel.configuration?.observe(this) {
            configuration = it
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
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    environment = environments[position]
                }
            }

        viewModel.isLoading.observe(this) { value ->
            if (value) {
                binding.updateButton.isEnabled = false
                binding.loginLoadingProcessBar.visibility = View.VISIBLE
            } else {
                binding.updateButton.isEnabled = true
                binding.loginLoadingProcessBar.visibility = View.GONE
            }
        }

        viewModel.profileUpdate.observe(this) { value ->
            if (value) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.errorMessage.observe(this) { value ->
            if (value != null && value.isNotEmpty()) {
                binding.errorMessageLabel.visibility = View.VISIBLE
                binding.errorMessageLabel.text = value
            } else {
                binding.errorMessageLabel.visibility = View.GONE
            }
        }

        viewModel.user?.observe(this) {
            if (it != null) {
                populateFields(it)
            }
        }

        binding.updateButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun populateFields(user: User) {
        binding.emailAddress.text = user.emailAddress
        binding.surnameInput.setText(user.surname)
        binding.otherNamesInput.setText(user.otherNames)
        user.age?.let { binding.ageInput.setText(it.toString()) }
        binding.phoneInput.setText(user.phone)

        // Network
        when (user.network?.uppercase()) {
            "MTN" -> binding.networkGroup.check(R.id.network_mtn)
            "VODAFONE" -> binding.networkGroup.check(R.id.network_vodafone)
            "AIRTELTIGO" -> binding.networkGroup.check(R.id.network_airteltigo)
        }

        // Network
        when (user.gender?.uppercase()) {
            "MALE" -> binding.genderGroup.check(R.id.gender_male)
            "FEMALE" -> binding.genderGroup.check(R.id.gender_female)
            "OTHER" -> binding.genderGroup.check(R.id.gender_other)
        }

        binding.privacyPolicyCheckBox.isChecked = user.acceptedPrivacyPolicy

        // Environment
        binding.environmentSpinner.setSelection(Constants.ENVIRONMENTS.indexOf(user.environment))
    }

    private fun updateProfile() {
        var allFieldsValid = true
        binding.errorMessageLabel.visibility = View.GONE
        // Surname
        val surname = binding.surnameInput.text.toString()
        if (surname.isEmpty()) {
            binding.surnameError.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.surnameError.visibility = View.GONE
        }

        // Other names
        val otherNames = binding.otherNamesInput.text.toString()
        if (otherNames.isEmpty()) {
            binding.otherNamesError.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.otherNamesError.visibility = View.GONE
        }

        // Age
        val age = binding.ageInput.text.toString().toIntOrNull()
        if ((age == null) || (age.toString().length != 2)) {
            binding.ageErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.ageErrorLabel.visibility = View.GONE
        }

        // Gender
        var gender = ""
        val genderSelectedId: Int = binding.genderGroup.checkedRadioButtonId
        if (genderSelectedId == -1) {
            binding.genderErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            val radioButton: RadioButton = binding.genderGroup.findViewById(genderSelectedId)
            gender = radioButton.text.toString()
            binding.genderErrorLabel.visibility = View.GONE
        }

        // Environment
        if (environment.isEmpty()) {
            binding.environmentErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.environmentErrorLabel.visibility = View.GONE
        }

        // Privacy Policy
        val checkedPrivacyPolicy = binding.privacyPolicyCheckBox.isChecked
        if (!checkedPrivacyPolicy) {
            binding.privacyPolicyErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.privacyPolicyErrorLabel.visibility = View.GONE
        }

        // Phone
        val phone = binding.phoneInput.text.toString()
        if (phone.length != 10 || !phone.startsWith("0")) {
            binding.phoneErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            binding.phoneErrorLabel.visibility = View.GONE
        }

        // Network
        var network = ""
        val selectedId: Int = binding.networkGroup.checkedRadioButtonId
        if (selectedId == -1) {
            binding.networkErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            val radioButton: RadioButton = binding.networkGroup.findViewById(selectedId)
            network = radioButton.text.toString()
            binding.networkErrorLabel.visibility = View.GONE
        }

        if (allFieldsValid) {
            viewModel.updateProfile(
                gender,
                surname,
                otherNames,
                network.uppercase(),
                phone,
                checkedPrivacyPolicy,
                environment,
                age
            )
        } else {
            viewModel.errorMessage.value = "All fields are required."
        }
    }

    private fun showPrivacyPolicyBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.privacy_policy_bottom_sheet_dialog_layout)

        val textView = bottomSheetDialog.findViewById<TextView>(R.id.privacy_policy_text)
        val actionButton = bottomSheetDialog.findViewById<Button>(R.id.action_button)

        actionButton?.setOnClickListener {
            bottomSheetDialog.dismiss();
        }

        if (configuration != null && textView != null) {
            textView.text = configuration?.privacyPolicyStatement
        }
        bottomSheetDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}