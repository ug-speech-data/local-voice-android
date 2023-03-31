package com.hrd.localvoice.adapters

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ItemAudioBinding
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import java.io.File
import java.util.*


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
                val name =
                    if (audio.localFileURl.isNotEmpty()) audio.localFileURl.split("/")[audio.localFileURl.split(
                        "/"
                    ).size - 1] else (audio.description)
                description.text = name
                statusText.text = audio.status

                if (audio.localImageURl?.isNotEmpty() == true) {
                    val imageUrl = audio.localImageURl
                    val options: RequestOptions = RequestOptions().fitCenter()
                        .placeholder(R.drawable.ic_outline_audio_file_24)
                        .error(R.drawable.ic_outline_audio_file_24)
                    Glide.with(context).load(imageUrl).apply(options)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(imageView)
                }

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = audio.timestamp
                val formatter = SimpleDateFormat("E, dd MMM Y, hh:mm:a")
                timestamp.text = formatter.format(calendar.time);

                if (audio.status == UPLOAD_STATUS_PENDING) {
                    statusText.setTextColor(Color.rgb(200, 50, 50))
                } else if (audio.localFileURl.isEmpty()) {
                    statusText.setTextColor(Color.rgb(200, 200, 200))
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