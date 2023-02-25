package com.hrd.localvoice.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrd.localvoice.utils.Constants
import kotlinx.parcelize.Parcelize


@Entity(tableName = Constants.ACTIVITY_STATUS_TABLE)
@Parcelize
data class ActivityStatus(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
//    var audioUpload: String = "",
) : Parcelable
