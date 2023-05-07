package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.TranscriptionAudio


class TranscriptionAudiosResponse {
    @SerializedName("audios")
    val audios: List<TranscriptionAudio> = listOf()
}

