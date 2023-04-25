package com.hrd.localvoice.adapters

import android.content.Context
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
import com.hrd.localvoice.utils.ConversionStatus
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

                var sText = audio.status
                if (audio.uploadCount > 1) {
                    sText += " (${audio.uploadCount})"
                }

                description.text = name
                statusText.text = sText
                conversionStatus.text = audio.conversionStatus

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

                // Color code uploaded status
                if (audio.status == UPLOAD_STATUS_PENDING) {
                    statusText.setTextColor(context.getColor(R.color.color_warning))
                } else if (audio.localFileURl.isEmpty()) {
                    statusText.setTextColor(context.getColor(R.color.gray_400))
                } else {
                    statusText.setTextColor(context.getColor(R.color.color_success))
                }

                // Color code conversion status
                if (audio.conversionStatus == ConversionStatus.CONVERTED) {
                    conversionStatus.setTextColor(context.getColor(R.color.color_success))
                } else {
                    conversionStatus.setTextColor(context.getColor(R.color.color_warning))
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