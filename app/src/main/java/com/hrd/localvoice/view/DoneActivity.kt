package com.hrd.localvoice.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hrd.localvoice.databinding.ActivityDoneBinding
import com.hrd.localvoice.view.participants.ParticipantBioActivity

class DoneActivity : AppCompatActivity() {
    lateinit var binding: ActivityDoneBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.homeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.nextParticipant.setOnClickListener {
            startActivity(Intent(this, ParticipantBioActivity::class.java))
        }
    }
}