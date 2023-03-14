package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.hrd.localvoice.utils.Constants
import kotlinx.parcelize.Parcelize


@Entity(tableName = Constants.CONFIGURATIONS_TABLE)
@Parcelize
data class Configuration(
    var demoVideoLocalUrl: String = "",
    @SerializedName("demo_video_url") var demoVideoRemoteUrl: String = "",
    @SerializedName("max_image_description_count") var maxImageDescriptionCount: Int? = 3,
    @SerializedName("participant_privacy_statement") var privacyPolicyStatement: String? = "",

    @PrimaryKey @SerializedName("id") var id: Long = 0,

    @SerializedName("participant_privacy_statement_audio") @ColumnInfo(defaultValue = "") var privacyPolicyStatementAudioRemoteUrl: String? = "",
    @ColumnInfo(defaultValue = "") var privacyPolicyStatementAudioLocalUrl: String? = "",

    @SerializedName("max_background_noise_level") @ColumnInfo(defaultValue = "350") var maximumBackgroundNoiseLevel: Int? = 350,
) : Parcelable
