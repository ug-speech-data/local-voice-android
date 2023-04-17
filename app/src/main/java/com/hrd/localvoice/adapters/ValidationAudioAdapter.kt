package com.hrd.localvoice.adapters

import android.content.Context
import android.content.Intent
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
import com.hrd.localvoice.databinding.ItemValidationAudioBinding
import com.hrd.localvoice.models.ValidationAudio
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import com.hrd.localvoice.view.validations.AudioValidationActivity
import java.io.File
import java.util.*


class ValidationAudioAdapter(private val context: Context) :
    RecyclerView.Adapter<ValidationAudioAdapter.AudioViewHolder>() {

    private var listener: OnPlayStopButtonClickListener? = null
    private var dataset = listOf<ValidationAudio>()

    interface OnPlayStopButtonClickListener {
        fun playStopAudioListener(audio: ValidationAudio)
    }


    inner class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemValidationAudioBinding.bind(view)

        fun bind(position: Int) {
            val audio = dataset[position]
            with(binding) {
                val fileName =
                    if (audio.localAudioUrl?.isNotEmpty() == true)
                        if (audio.localAudioUrl!!.isNotEmpty()) audio.localAudioUrl!!.split("/")[audio.localAudioUrl!!.split(
                            "/"
                        ).size - 1] else (audio.name)
                    else {
                        if (audio.remoteAudioUrl.isNotEmpty()) audio.remoteAudioUrl.split("/")[audio.remoteAudioUrl.split(
                            "/"
                        ).size - 1] else (audio.name)
                    }

                name.text = fileName
                statusText.text = audio.assetsDownloadStatus
                validationStatusText.text = audio.validatedStatus?.uppercase()

                if (audio.localImageUrl?.isNotEmpty() == true) {
                    val imageUrl = audio.localImageUrl
                    val options: RequestOptions = RequestOptions().fitCenter()
                        .placeholder(R.drawable.ic_outline_audio_file_24)
                        .error(R.drawable.ic_outline_audio_file_24)
                    Glide.with(context).load(imageUrl).apply(options)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(imageView)
                }

                val calendar = Calendar.getInstance()
                audio.updatedAt.also {
                    if (it != null) {
                        calendar.timeInMillis = it
                    }
                }
                val formatter = SimpleDateFormat("E, dd MMM Y, hh:mm:a")
                timestamp.text = formatter.format(calendar.time);

                if (audio.assetsDownloadStatus == UPLOAD_STATUS_PENDING) {
                    statusText.setTextColor(Color.rgb(200, 50, 50))
                } else {
                    statusText.setTextColor(Color.rgb(200, 200, 200))
                }

                cardView.setOnClickListener {
                    listener?.playStopAudioListener(audio)
                }

                val file = audio.localAudioUrl?.let { File(it) }
                val fileSize = "%.2f".format((file?.length())?.div((1024 * 1024).toFloat()))
                sizeText.text = "${audio.duration} seconds  $fileSize MB"

                cardView.setOnClickListener {
                    val intent = Intent(context, AudioValidationActivity::class.java)
                    intent.putExtra("position", position)
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding =
            ItemValidationAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(position)
    }

    fun setData(dataset: List<ValidationAudio>) {
        this.dataset = dataset
        notifyDataSetChanged()
    }

    override fun getItemCount() = this.dataset.size;
}