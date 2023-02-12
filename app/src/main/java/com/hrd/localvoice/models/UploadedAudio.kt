package com.hrd.localvoice.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable


@Parcelize
data class UploadedAudio(
    val id: Long,

    @SerializedName("image_url") val imageURL: String,

    val thumbnail: String,
    val name: String,

    @SerializedName("submitted_by") val submittedBy: String,

    @SerializedName("audio_url") val audioURL: String,

    @SerializedName("image_batch_number") val imageBatchNumber: Long,

    val file: String,

    @SerializedName("device_id") val deviceID: String? = null,

    val year: Long,
    val locale: String,
    val duration: Long,
    val environment: String,

    @SerializedName("is_accepted") val isAccepted: Boolean,

    ) : Parcelable, Serializable
