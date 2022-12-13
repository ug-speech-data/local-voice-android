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
    @SerializedName("id")
    val remoteId: Long?,

    @PrimaryKey(autoGenerate = true)
    @SerializedName("local_id")
    var id: Long = 0,
) : Parcelable
