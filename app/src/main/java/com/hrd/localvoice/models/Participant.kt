package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants.PARTICIPANT_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import kotlinx.parcelize.Parcelize

@Entity(tableName = PARTICIPANT_TABLE)
@Parcelize
data class Participant(
    var age: Int,
    val gender: String,
    val status: String? = UPLOAD_STATUS_PENDING,
    var momoNumber: String? = null,
    var network: String? = null,
    var environment: String? = null,
    var locale: String? = null,
    var deviceId: String? = null,
    var fullname: String? = null,
    var audioDurationInSeconds: Long? = 0,
    var acceptedPrivacyPolicy : Boolean? = false,
    @PrimaryKey(autoGenerate = true)
    @SerializedName("local_id")
    var id: Long = 0,
) : Parcelable
