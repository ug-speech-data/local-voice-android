package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.UploadedAudio


class UploadResponse {
    @SerializedName("success")
    val success: Boolean? = null

    @SerializedName("message")
    val message: String? = null

    @SerializedName("audio")
    val audio: UploadedAudio? = null

}
