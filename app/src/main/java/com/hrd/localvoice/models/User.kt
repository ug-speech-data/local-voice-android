package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.StringListConvector
import kotlinx.parcelize.Parcelize

@Entity(tableName = Constants.USER_TABLE)
@Parcelize
data class User(
    @PrimaryKey val id: Long,
    val photo: String?,
    @SerializedName("email_address") var emailAddress: String,
    var phone: String?,
    @SerializedName("phone_network") val network: String?,
    val surname: String?,
    @SerializedName("other_names") val otherNames: String?,
    val gender: String?,
    @SerializedName("recording_environment")
    val environment: String?,
    val age: Int?,
    @SerializedName("last_login_date") val lastLoginDate: String,
    @SerializedName("accepted_privacy_policy") val acceptedPrivacyPolicy: Boolean,
    @SerializedName("created_at") val createdAt: String,

    @TypeConverters(StringListConvector::class) val permissions: List<String>?

) : Parcelable
