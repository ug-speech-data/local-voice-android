package com.hrd.localvoice.adapters

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hrd.localvoice.databinding.ItemTranscriptionAudioBinding
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import java.io.File
import java.util.*


class TranscriptionAudioAdapter(private val context: Context) :
    RecyclerView.Adapter<TranscriptionAudioAdapter.AudioViewHolder>() {

    private var listener: OnClickListener? = null
    private var dataset = listOf<TranscriptionAudio>()

    interface OnClickListener {
        fun onItemClickListener(position: Int)
    }

    fun setOnClickListener(listener: OnClickListener) {
        this.listener = listener
    }

    inner class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemTranscriptionAudioBinding.bind(view)

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
                transcriptionStatusText.text = audio.transcriptionStatus?.uppercase()

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

                val file = audio.localAudioUrl?.let { File(it) }
                val fileSize = "%.2f".format((file?.length())?.div((1024 * 1024).toFloat()))
                sizeText.text = "${audio.duration} seconds  $fileSize MB"

                cardView.setOnClickListener {
                    listener?.onItemClickListener(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding =
            ItemTranscriptionAudioBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return AudioViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(position)
    }

    fun setData(dataset: List<TranscriptionAudio>) {
        this.dataset = dataset
        notifyDataSetChanged()
    }

    override fun getItemCount() = this.dataset.size;
}