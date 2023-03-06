package com.hrd.localvoice.adapters

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hrd.localvoice.databinding.ItemAudioBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import java.io.File
import java.util.*
import kotlin.Int
import kotlin.with


class AudioAdapter(private val context: Context) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {
    private var listener: OnPlayStopButtonClickListener? = null
    private var dataset = listOf<Audio>()

    interface OnPlayStopButtonClickListener {
        fun playStopAudioListener(audio: Audio)
    }

    fun setOnPlayStopAudioListener(listener: OnPlayStopButtonClickListener) {
        this.listener = listener
    }

    inner class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemAudioBinding.bind(view)

        fun bind(audio: Audio) {
            with(binding) {
                description.text = audio.description
                statusText.text = audio.status

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = audio.timestamp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val formatter = SimpleDateFormat("E, dd MMM Y, hh:mm:a")
                    timestamp.text = formatter.format(calendar.time);
                }

                if (audio.status == UPLOAD_STATUS_PENDING) {
                    statusText.setTextColor(Color.rgb(200, 50, 50))
                } else {
                    statusText.setTextColor(Color.rgb(50, 200, 50))
                }

                cardView.setOnClickListener {
                    listener?.playStopAudioListener(audio)
                }

                val file = File(audio.localFileURl)
                val fileSize = "%.2f".format(file.length() / (1024 * 1024).toFloat())
                sizeText.text = "${audio.duration} seconds  $fileSize MB"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding = ItemAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(dataset[position])
    }

    fun setData(dataset: List<Audio>) {
        this.dataset = dataset
        notifyDataSetChanged()
    }

    override fun getItemCount() = this.dataset.size;
}