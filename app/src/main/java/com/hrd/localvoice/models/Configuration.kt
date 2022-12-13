package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants
import kotlinx.parcelize.Parcelize


@Entity(tableName = Constants.CONFIGURATIONS_TABLE)
@Parcelize
data class Configuration(
    var demoVideoLocalUrl: String = "",

    @SerializedName("demo_video_url")
    var demoVideoRemoteUrl: String = "",

    @SerializedName("max_image_description_count") var maxImageDescriptionCount: Int? = 3,

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
) : Parcelable
