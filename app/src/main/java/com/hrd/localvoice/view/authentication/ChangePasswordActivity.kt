package com.hrd.localvoice.view.authentication

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityChangePasswordBinding
    private lateinit var viewModel: AuthenticationActivityViewModel
    private var showOldPassword = false
    private var showNewPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]

        binding.exitButton.setOnClickListener {
            finish()
        }

        binding.toggleOldPasswordVisibilityButton.setOnClickListener {
            binding.oldPasswordInput.transformationMethod =
                if (showOldPassword) PasswordTransformationMethod() else null
            binding.oldPasswordInput.setSelection(binding.oldPasswordInput.length());
            showOldPassword = !showOldPassword

            if (showOldPassword) it.setBackgroundResource(R.drawable.baseline_visibility_off_24)
            else {
                it.setBackgroundResource(R.drawable.baseline_visibility_24)
            }
        }

        binding.toggleNewPasswordVisibilityButton.setOnClickListener {
            binding.newPasswordInput.transformationMethod =
                if (showNewPassword) PasswordTransformationMethod() else null
            binding.newPasswordInput.setSelection(binding.newPasswordInput.length());
            showNewPassword = !showNewPassword

            if (showOldPassword) it.setBackgroundResource(R.drawable.baseline_visibility_off_24)
            else {
                it.setBackgroundResource(R.drawable.baseline_visibility_24)
            }
        }

        viewModel.isLoading.observe(this) { value ->
            if (value) {
                binding.submitButton.isEnabled = false
                binding.loginLoadingProcessBar.visibility = View.VISIBLE
            } else {
                binding.submitButton.isEnabled = true
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
                finish()
            }
        }

        binding.submitButton.setOnClickListener {
            binding.errorMessageLabel.visibility = View.GONE
            val oldPassword = binding.oldPasswordInput.text.toString()
            val newPassword = binding.newPasswordInput.text.toString()

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                viewModel.errorMessage.value = "Please enter your old and new passwords."
            } else {
                viewModel.changePassword(oldPassword, newPassword)
            }
        }
    }
}