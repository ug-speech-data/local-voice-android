package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.Image


class ImagesResponse {
    @SerializedName("images")
    val images: List<Image> = listOf()
}

