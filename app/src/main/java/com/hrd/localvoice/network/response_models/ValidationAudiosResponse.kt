package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.ValidationAudio


class ValidationAudiosResponse {
    @SerializedName("audios")
    val audios: List<ValidationAudio> = listOf()
}

