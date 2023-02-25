package com.hrd.localvoice.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.PrivacyPolicyBottomSheetDialogLayoutBinding
import java.io.File
import java.io.IOException

class PrivacyPolicyBottomSheet : BottomSheetDialogFragment() {
    private var mediaPlayer: MediaPlayer? = MediaPlayer()
    private lateinit var binding: PrivacyPolicyBottomSheetDialogLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            PrivacyPolicyBottomSheetDialogLayoutBinding.inflate(layoutInflater, container, false)
        binding.actionButton?.setOnClickListener {
            dismiss();
        }

        AppRoomDatabase.databaseWriteExecutor.execute {
            val configuration = AppRoomDatabase.INSTANCE?.ConfigurationDao()?.getConfiguration()
            binding.privacyPolicyText.text = configuration?.privacyPolicyStatement

            binding.playPrivacyPolicy.setOnClickListener {
                if (configuration?.privacyPolicyStatementAudioLocalUrl != null) {
                    binding.playPrivacyPolicy.visibility = View.VISIBLE
                } else {
                    binding.playPrivacyPolicy.visibility = View.GONE
                }
                playFile(configuration?.privacyPolicyStatementAudioLocalUrl)
            }
        }
        return binding.root
    }


    private fun playFile(audioFilePath: String?) {
        val file: File? = audioFilePath?.let { File(it) }
        if (file?.exists() != true) return

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            binding.playPrivacyPolicy.setBackgroundResource(R.drawable.ic_baseline_play_circle_outline_24)
            return
        }

        val fileName = file.absolutePath
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(fileName)
            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
                binding.playPrivacyPolicy.setBackgroundResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            Log.d(tag, "onCreate: Can't open file: $fileName")
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}