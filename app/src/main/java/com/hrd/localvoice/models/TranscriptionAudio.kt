package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants.AUDIO_TRANSCRIPTION_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import com.hrd.localvoice.utils.TranscriptionStatus
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Entity(tableName = AUDIO_TRANSCRIPTION_TABLE)
@Parcelize
data class TranscriptionAudio(
    @ColumnInfo(defaultValue = "") @SerializedName("audio_url") val remoteAudioUrl: String,
    @ColumnInfo(defaultValue = "") val name: String,
    @ColumnInfo(defaultValue = "") val locale: String,
    @ColumnInfo(defaultValue = "0") val duration: Long,
    @ColumnInfo(defaultValue = "") val environment: String,
    @ColumnInfo(defaultValue = "") var localAudioUrl: String? = "",
    @ColumnInfo(defaultValue = UPLOAD_STATUS_PENDING) var assetsDownloadStatus: String? = UPLOAD_STATUS_PENDING,
    @ColumnInfo(defaultValue = TranscriptionStatus.PENDING) var transcriptionStatus: String? = TranscriptionStatus.PENDING,
    @ColumnInfo(defaultValue = "pending") var text: String? = "",
    @ColumnInfo(defaultValue = "1") @SerializedName("id") @PrimaryKey() var id: Long = 0,
    @ColumnInfo(defaultValue = "0") var updatedAt: Long? = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") var createdAt: Long? = System.currentTimeMillis()
) : Parcelable, Serializable
