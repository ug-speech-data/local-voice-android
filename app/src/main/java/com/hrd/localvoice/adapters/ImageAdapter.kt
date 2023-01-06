package com.hrd.localvoice.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.hrd.localvoice.R
import com.hrd.localvoice.databinding.ItemImageBinding
import com.hrd.localvoice.models.Image


class ImageAdapter(private val context: Context) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    private var dataset = listOf<Image>()


    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemImageBinding.bind(view)
        fun bind(image: Image) {
            with(binding) {
                description.text = image.name

                val imageUrl =
                    if (image.localURl != null) image.localURl else image.remoteURL
                val options: RequestOptions =
                    RequestOptions().fitCenter().placeholder(R.mipmap.loading)
                        .error(R.mipmap.loading)

                Glide.with(context).load(imageUrl).apply(options)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(imageView)

                categoryText.text = image.category
                statusText.text = "${image.descriptionCount} description(s)"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(dataset[position])
    }

    fun setData(dataset: List<Image>) {
        this.dataset = dataset
        notifyDataSetChanged()
    }

    override fun getItemCount() = this.dataset.size;
}