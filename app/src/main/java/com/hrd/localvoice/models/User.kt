package com.hrd.localvoice.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Long,
    val photo: Long,
    @SerializedName("email_address")
    var emailAddress: String,
    var phone: String,
    val surname: String,

    @SerializedName("other_names")
    val otherNames: String,

    @SerializedName("last_login_date")
    val lastLoginDate: String,

    @SerializedName("created_at")
    val createdAt: String,

    ) : Parcelable
