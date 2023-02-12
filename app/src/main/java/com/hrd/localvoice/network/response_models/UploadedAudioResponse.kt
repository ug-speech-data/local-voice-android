package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.UploadedAudio


class UploadedAudioResponse {
    @SerializedName("audio")
    val audio: UploadedAudio? = null

    @SerializedName("error_message")
    val errorMessage: String? = null
}
