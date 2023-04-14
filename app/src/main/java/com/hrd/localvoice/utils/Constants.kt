package com.hrd.localvoice.utils

import com.hrd.localvoice.R

object Constants {
    const val AUDIOS_TABLE = "audios"
    const val IMAGES_TABLE = "images"
    const val CONFIGURATIONS_TABLE = "configurations"
    const val ACTIVITY_STATUS_TABLE = "activity_status"
    const val USER_TABLE = "users"
    const val PARTICIPANT_TABLE = "participants"
    const val UPLOAD_STATUS_PENDING = "PENDING"
    const val AUDIO_STATUS_UPLOADED = "UPLOADED"

    const val USER_TOKEN = "com.hrd.localvoice.USER_TOKEN"
    const val SHARED_PREFS_FILE = "localvoice-pf"
    const val USER_ID = "com.hrd.localvoice.USER_ID"
    const val IS_NEW_USER = "com.hrd.localvoice.IS_NEW_USER"
    const val USER_OBJECT = "com.hrd.localvoice.USER_OBJECT"

    val ENVIRONMENTS = arrayOf(
        "Outdoor",
        "Office",
        "In a car",
        "Studio",
        "On bus",
        "Indoor",
        "Other",
    )
}

object AudioStatus {
    const val ACCEPTED = "accepted"
    const val REJECTED = "reject"
}