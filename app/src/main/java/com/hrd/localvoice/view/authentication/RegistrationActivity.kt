package com.hrd.localvoice.view.authentication

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.databinding.ActivityRegistrationBinding

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var viewModel: AuthenticationActivityViewModel
    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]
        binding.alreadyHaveAccountLabel.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        viewModel.isLoading.observe(this) { value ->
            if (value) {
                binding.registerButton.isEnabled = false
                binding.loginLoadingProcessBar.visibility = View.VISIBLE
            } else {
                binding.registerButton.isEnabled = true
                binding.loginLoadingProcessBar.visibility = View.GONE
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

        viewModel.isLoggedIn.observe(this) { value ->
            if (value) {
                // Navigate to update profile
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
            }
        }

        binding.togglePasswordVisibilityButton.setOnClickListener {
            binding.passwordInput.transformationMethod =
                if (showPassword) PasswordTransformationMethod() else null
            binding.passwordInput.setSelection(binding.passwordInput.length());
            showPassword = !showPassword
        }

        binding.registerButton.setOnClickListener {
            binding.errorMessageLabel.visibility = View.GONE
            val password = binding.passwordInput.text.toString()
            val emailAddress = binding.emailAddressInput.text.toString()
            if (password.isEmpty() || emailAddress.isEmpty()) {
                viewModel.errorMessage.value = "All fields are required."
            } else {
                viewModel.register(emailAddress, password)
            }
        }
    }
}