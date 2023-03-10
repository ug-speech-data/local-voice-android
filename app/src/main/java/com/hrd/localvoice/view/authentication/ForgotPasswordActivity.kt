package com.hrd.localvoice.view.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.databinding.ActivityForgotPasswordBinding


class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityForgotPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toEmail = "drhelegah@st.ug.edu.gh"
        val subject = "SPEECH DATA: PASSWORD CHANGE"
        var message =
            "Dear Support Team,\n" + "\n" + "I am writing to request assistance with resetting my password for my account. Unfortunately, " + "I have forgotten my password and I am unable to access my account. " + "My email address associated with the account is [PLEASE ENTER YOUR EMAIL HERE]" + "\n" + "I would appreciate it if you could provide guidance on the steps to reset my password so that I may regain access to my account.\n" + "\n" + "Thank you for your prompt attention to this matter.\n" + "\n" + "Best regards"

        AppRoomDatabase.databaseWriteExecutor.execute {
            val user = AppRoomDatabase.INSTANCE?.UserDao()?.getUser()
            if (user != null) message =
                "Dear Support Team,\n" + "\n" + "I am writing to request assistance with resetting my password for my account. Unfortunately, " + "I have forgotten my password and I am unable to access my account. My email address associated with the account is ${user.emailAddress}.\n" + "\n" + "I would appreciate it if you could provide guidance on the steps to reset my password so that I may regain access to my account.\n" + "\n" + "Thank you for your prompt attention to this matter.\n" + "\n" + "Best regards," + "\n${user.surname} ${user.otherNames}."
        }

        binding.sendEmail.setOnClickListener {
            val email = Intent(Intent.ACTION_SEND)
            email.putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
            email.putExtra(Intent.EXTRA_SUBJECT, subject)
            email.putExtra(Intent.EXTRA_TEXT, message)
            email.type = "message/rfc822"
            startActivity(Intent.createChooser(email, "Choose an Email client :"))
        }

        binding.exitButton.setOnClickListener {
            finish()
        }
    }
}