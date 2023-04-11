package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants.AUDIOS_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import kotlinx.parcelize.Parcelize
import java.io.Serializable

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
    var localFileURl: String,
    @SerializedName("device_id") val deviceId: String,
    @ColumnInfo(defaultValue = "") var localImageURl: String? = "",
    val environment: String,
    @SerializedName("name") var description: String = "Image Description",
    var status: String = UPLOAD_STATUS_PENDING,
    var remoteId: Long? = null,
    @SerializedName("audio_url") var remoteURL: Long? = null,
    var duration: Long? = null,
    var sizeInBytes: Long? = null,
    var participantId: Long? = null,
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(defaultValue = "1") var uploadCount: Long = 0,
) : Parcelable, Serializable
