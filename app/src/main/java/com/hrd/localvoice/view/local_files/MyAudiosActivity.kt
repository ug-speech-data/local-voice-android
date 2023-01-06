package com.hrd.localvoice.view.local_files

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hrd.localvoice.R
import com.hrd.localvoice.adapters.AudioAdapter
import com.hrd.localvoice.databinding.ActivityMyAudiosBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.AUDIO_STATUS_UPLOADED
import java.io.File
import java.io.IOException

class MyAudiosActivity : AppCompatActivity() {
    private lateinit var adapter: AudioAdapter
    private lateinit var binding: ActivityMyAudiosBinding
    private lateinit var viewModel: MyAudiosActivityViewModel
    private var mediaPlayer: MediaPlayer? = MediaPlayer()
    private val tag = "MyAudiosActivity"
    private var currentAudio: Audio? = null
    private var audios: List<Audio>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAudiosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "My Audios"

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setup()
        loadAudios()

        // Swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            loadAudios()
        }

        adapter.setOnPlayStopAudioListener(object : AudioAdapter.OnPlayStopButtonClickListener {
            override fun playStopAudioListener(audio: Audio) {
                if (mediaPlayer?.isPlaying == true && currentAudio?.localFileURl == audio.localFileURl) {
                    mediaPlayer?.stop()
                } else {
                    playFile(File(audio.localFileURl))
                }
                currentAudio = audio
            }
        })
    }

    private fun setup() {
        viewModel = ViewModelProvider(this)[MyAudiosActivityViewModel::class.java]
        adapter = AudioAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadAudios() {
        viewModel.getAudios()?.observe(this) {
            audios = it
            adapter.setData(it)
            binding.swiperefresh.isRefreshing = false

            if (it.isEmpty()) {
                binding.infoText.visibility = View.VISIBLE
            } else {
                binding.infoText.visibility = View.GONE
            }
        }
    }

    fun playFile(file: File) {
        if (!file.exists()) return
        val fileName = file.absolutePath

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(fileName)
            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
            }
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            Log.d(tag, "onCreate: Can't open file: $fileName")
        }
    }

    private fun showAlertDialogBox() {
        val dialog =
            AlertDialog.Builder(this).setTitle("DELETE UPLOADED AUDIOS").setCancelable(false)
                .setNegativeButton("Cancel") { _, _ -> }.setPositiveButton("Delete") { _, _ ->
                    audios?.forEach { audio ->
                        if (audio.status == AUDIO_STATUS_UPLOADED) {
                            val file = File(audio.localFileURl)
                            file.delete()
                            viewModel.deleteAudio(audio)
                        }
                    }
                }.setMessage(getString(R.string.audio_deletion_info))

        dialog.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.my_audios_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_uploaded ->
                showAlertDialogBox()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}