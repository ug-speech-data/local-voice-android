package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants.IMAGES_TABLE
import kotlinx.parcelize.Parcelize

@Entity(tableName = IMAGES_TABLE)
@Parcelize
data class Image(
    var name: String,
    @SerializedName("image_url") var remoteURL: String? = null,

    @PrimaryKey @SerializedName("id") var remoteId: Long,

    var localURl: String? = null,
    var category: String? = "",

    @SerializedName("description_count") var descriptionCount: Int = 0,
) : Parcelable
