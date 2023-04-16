package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants.AUDIO_VALIDATION_TABLE
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Entity(tableName = AUDIO_VALIDATION_TABLE)
@Parcelize
data class ValidationAudio(
    @ColumnInfo(defaultValue = "") @SerializedName("audio_url") val remoteAudioUrl: String,
    @ColumnInfo(defaultValue = "") @SerializedName("image_url") val remoteImageUrl: String,
    @ColumnInfo(defaultValue = "") val name: String,
    @ColumnInfo(defaultValue = "") val locale: String,
    @ColumnInfo(defaultValue = "0") val duration: Long,
    @ColumnInfo(defaultValue = "") val environment: String,
    @ColumnInfo(defaultValue = "") var localAudioUrl: String? = "",
    @ColumnInfo(defaultValue = "") var localImageUrl: String? = "",
    @ColumnInfo(defaultValue = "pending") var assetsDownloadStatus: String? = "pending",
    @ColumnInfo(defaultValue = "pending") var validatedStatus: String? = "pending",
    @ColumnInfo(defaultValue = "1") @SerializedName("id") @PrimaryKey() var id: Long = 0,
    @ColumnInfo(defaultValue = "0") var updatedAt: Long? = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") var createdAt: Long? = System.currentTimeMillis()
) : Parcelable, Serializable
