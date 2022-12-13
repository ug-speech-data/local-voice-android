package com.hrd.localvoice.network.response_models

import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.models.User


class AuthenticationResponse {
    @SerializedName("user")
    val user: User? = null

    @SerializedName("error_message")
    val errorMessage: String? = null

    @SerializedName("token")
    val token: String? = null
}
