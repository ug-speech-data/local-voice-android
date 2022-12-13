package com.hrd.localvoice.view.participants

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ActivityParticipantBioBinding
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.view.VideoPlayerActivity


class ParticipantBioActivity : AppCompatActivity() {
    private lateinit var viewModel: ParticipantBioActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityParticipantBioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.participant_bio)

        viewModel = ViewModelProvider(this)[ParticipantBioActivityViewModel::class.java]

        binding.nextButton.setOnClickListener {
            // Age
            val age = binding.ageInput.text.toString()
            if (age.isEmpty()) {
                binding.ageErrorLabel.visibility = View.VISIBLE
            } else {
                binding.ageErrorLabel.visibility = View.GONE
            }

            // Gender
            var gender: String = ""
            val selectedId: Int = binding.genderGroup.checkedRadioButtonId
            if (selectedId == -1) {
                binding.genderErrorLabel.visibility = View.VISIBLE
            } else {
                val radioButton: RadioButton = binding.genderGroup.findViewById(selectedId)
                gender = radioButton.text.toString()
                binding.genderErrorLabel.visibility = View.GONE
            }

            if (age.isNotEmpty() && gender.isNotEmpty()) {
                val participant = Participant(age.toInt(), gender, null, null)
                viewModel.createParticipant(participant)

                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}