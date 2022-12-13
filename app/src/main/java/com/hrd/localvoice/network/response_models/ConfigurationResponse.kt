package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.Configuration


class ConfigurationResponse {
    @SerializedName("configuration")
    val configuration: Configuration? = null
}
