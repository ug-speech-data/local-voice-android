package com.hrd.localvoice.view.transcriptions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hrd.localvoice.databinding.ActivityTranscriptionResolutionBinding

class TranscriptionResolutionActivity : AppCompatActivity() {
    lateinit var binding: ActivityTranscriptionResolutionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscriptionResolutionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}