package com.hrd.localvoice.view.authentication

import android.annotation.SuppressLint
import android.content.Intent
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
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.BuildConfig
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityProfileBinding
import com.hrd.localvoice.fragments.PrivacyPolicyBottomSheet
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.view.MainActivity
import java.io.File

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: AuthenticationActivityViewModel
    private var configuration: Configuration? = null
    var environment = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Update Profile"

        // Show version name
        binding.appVersionText.text = "v${BuildConfig.VERSION_NAME}"

        // Reset button
        binding.clearAppData.setOnClickListener {
            showClearAppDataDialog()
        }

        // Privacy policy
        binding.privacyPolicyLabel.setOnClickListener {
            showPrivacyPolicyBottomSheetDialog()
        }

        // Password Change
        binding.changePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        viewModel.user?.observe(this) {
            if (it == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
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
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
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
                Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_LONG).show()
                startActivity(intent)
                finish()
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

        // Gender
        when (user.gender?.uppercase()) {
            "MALE" -> binding.genderGroup.check(R.id.gender_male)
            "FEMALE" -> binding.genderGroup.check(R.id.gender_female)
            "OTHER" -> binding.genderGroup.check(R.id.gender_other)
        }

        // Locale
        when (user.locale?.lowercase()) {
            "ak_gh" -> binding.localeGroup.check(R.id.locale_akan)
            "ee_gh" -> binding.localeGroup.check(R.id.locale_ewe)
            "dag_gh" -> binding.localeGroup.check(R.id.locale_dagaaree)
            "dga_gh" -> binding.localeGroup.check(R.id.locale_dagbani)
            "kpo_gh" -> binding.localeGroup.check(R.id.locale_ikposo)
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
            val radioButton: RadioButton? = binding.genderGroup.findViewById(genderSelectedId)
            if (radioButton == null) {
                allFieldsValid = false
            } else {
                gender = radioButton?.text.toString()
                binding.genderErrorLabel.visibility = View.GONE
            }
        }

        // Locale
        var locale = ""
        val localeSelectedId: Int = binding.localeGroup.checkedRadioButtonId
        if (localeSelectedId == -1) {
            binding.localeErrorLabel.visibility = View.VISIBLE
            allFieldsValid = false
        } else {
            val radioButton: RadioButton = binding.localeGroup.findViewById(localeSelectedId)
            val lo = radioButton.text.toString()
            binding.localeErrorLabel.visibility = View.GONE

            when (lo.lowercase()) {
                "twi" -> locale = "ak_gh"
                "ewe" -> locale = "ee_gh"
                "dagbani" -> locale = "dag_gh"
                "dagaare" -> locale = "dga_gh"
                "ikposo" -> locale = "kpo_gh"
            }
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
                locale,
                age
            )
        } else {
            viewModel.errorMessage.value = "All fields are required."
        }
    }

    private fun showPrivacyPolicyBottomSheetDialog() {
        val modalBottomSheet = PrivacyPolicyBottomSheet()
        modalBottomSheet.show(supportFragmentManager, PrivacyPolicyBottomSheet.TAG)
    }

    private fun showClearAppDataDialog() {
        val dialog = AlertDialog.Builder(this).setTitle("Clear App Data")
            .setNegativeButton("CANCEL") { _, _ ->
            }.setPositiveButton(getString(R.string.yes)) { _, _ ->
                val database: AppRoomDatabase? = AppRoomDatabase.getDatabase(application)

                AppRoomDatabase.databaseWriteExecutor.execute {
                    database?.ConfigurationDao()?.deleteAll()
                }

                // Delete all images
                database?.ImageDao()?.getImages()?.observe(this) {
                    it.forEach { image ->
                        val file = image.localURl?.let { it1 -> File(it1) }
                        if (file?.exists() == true) {
                            file.delete()
                        }
                    }

                    AppRoomDatabase.databaseWriteExecutor.execute {
                        // Delete record in db
                        database.ImageDao().deleteAll()
                    }
                }

                // Delete all audios
                database?.AudioDao()?.getAudios()?.observe(this) {
                    it.forEach { audio ->
                        val file = File(audio.localFileURl)
                        if (file.exists()) {
                            file.delete()
                        }
                    }

                    AppRoomDatabase.databaseWriteExecutor.execute {
                        // Delete record in db
                        database.AudioDao().deleteAll()
                    }
                }
                Toast.makeText(this, "All app data cleared", Toast.LENGTH_SHORT).show()
            }
        dialog.setMessage("Are you sure you want to delete app data i.e., images, audios, and configurations?")
        dialog.create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}