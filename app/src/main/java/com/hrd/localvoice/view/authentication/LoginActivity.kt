package com.hrd.localvoice.view.authentication

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityLoginBinding
import com.hrd.localvoice.view.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthenticationActivityViewModel
    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]

        binding.createAccountLabel.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        viewModel.isLoading.observe(this) { value ->
            if (value) {
                binding.loginButton.isEnabled = false
                binding.loginLoadingProcessBar.visibility = View.VISIBLE
            } else {
                binding.loginButton.isEnabled = true
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
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }

        binding.togglePasswordVisibilityButton.setOnClickListener {
            binding.passwordInput.transformationMethod =
                if (showPassword) PasswordTransformationMethod() else null
            binding.passwordInput.setSelection(binding.passwordInput.length());
            showPassword = !showPassword

            if (showPassword) it.setBackgroundResource(R.drawable.baseline_visibility_off_24)
            else {
                it.setBackgroundResource(R.drawable.baseline_visibility_24)
            }
        }


        binding.loginButton.setOnClickListener {
            binding.errorMessageLabel.visibility = View.GONE
            val password = binding.passwordInput.text.toString()
            val emailAddress = binding.emailAddressInput.text.toString()
            if (password.isEmpty() || emailAddress.isEmpty()) {
                viewModel.errorMessage.value = "Email Address and password are required."
            } else {
                viewModel.login(emailAddress, password)
            }
        }
    }
}