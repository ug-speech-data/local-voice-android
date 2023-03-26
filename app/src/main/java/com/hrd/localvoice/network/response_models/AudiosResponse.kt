package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.UploadedAudio


class AudiosResponse {
    @SerializedName("audios")
    val audios: List<UploadedAudio> = listOf()
}

