package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrd.localvoice.utils.Constants.AUDIOS_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = AUDIOS_TABLE,
    indices = [Index(value = ["participantId"])],
    foreignKeys = [ForeignKey(
        entity = Participant::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("participantId"),
        onDelete = ForeignKey.SET_NULL
    )]
)

@Parcelize
data class Audio(
    val userId: Long,
    val timestamp: Long,
    var remoteImageID: Long,
    val localFileURl: String,
    var description: String = "Image Description",
    var status: String = UPLOAD_STATUS_PENDING,
    var remoteId: Long? = null,
    var remoteURL: Long? = null,
    val participantId: Long? = null,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
) : Parcelable
