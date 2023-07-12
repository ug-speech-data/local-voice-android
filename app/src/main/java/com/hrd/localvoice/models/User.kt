package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.ColumnInfo
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
    val age: Int? = null,
    val locale: String?,
    @SerializedName("last_login_date") val lastLoginDate: String,
    @SerializedName("accepted_privacy_policy") val acceptedPrivacyPolicy: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("balance") val balance: String = "0.00",
    @SerializedName("audios_submitted") val audiosSubmitted: Int = 0,
    @SerializedName("audios_validated") val audiosValidated: Int = 0,
    @ColumnInfo(defaultValue = "0") @SerializedName("audios_transcribed") val audiosTranscribed: Int = 0,
    @SerializedName("audios_pending") @ColumnInfo(defaultValue = "0") val audiosPending: Int = 0,
    @SerializedName("audios_accepted") @ColumnInfo(defaultValue = "0") val audiosAccepted: Int = 0,
    @SerializedName("transcriptions_resolved") @ColumnInfo(defaultValue = "0") val transcriptionsResolved: Int = 0,
    @SerializedName("audios_rejected") @ColumnInfo(defaultValue = "0") val audiosRejected: Int = 0,
    @SerializedName("estimated_deduction_amount") @ColumnInfo(defaultValue = "0.0") val estimatedDeductionAmount: Float = 0.0f,
    @TypeConverters(StringListConvector::class) @SerializedName("user_permissions") val permissions: List<String>?
) : Parcelable
