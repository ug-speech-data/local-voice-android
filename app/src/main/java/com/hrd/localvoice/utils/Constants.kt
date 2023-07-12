package com.hrd.localvoice.utils

object Constants {
    const val AUDIOS_TABLE = "audios"
    const val AUDIO_VALIDATION_TABLE = "validation_audios"
    const val AUDIO_TRANSCRIPTION_TABLE = "transcription_audios"
    const val AUDIO_TRANSCRIPTION_RESOLUTION_TABLE = "transcription_resolution_audios"
    const val IMAGES_TABLE = "images"
    const val CONFIGURATIONS_TABLE = "configurations"
    const val ACTIVITY_STATUS_TABLE = "activity_status"
    const val USER_TABLE = "users"
    const val PARTICIPANT_TABLE = "participants"
    const val UPLOAD_STATUS_PENDING = "PENDING"
    const val AUDIO_STATUS_UPLOADED = "UPLOADED"
    const val AUDIO_STATUS_DOWNLOADED = "DOWNLOADED"

    const val USER_TOKEN = "com.hrd.localvoice.USER_TOKEN"
    const val SHARED_PREFS_FILE = "localvoice-pf"
    const val USER_ID = "com.hrd.localvoice.USER_ID"
    const val IS_NEW_USER = "com.hrd.localvoice.IS_NEW_USER"
    const val USER_OBJECT = "com.hrd.localvoice.USER_OBJECT"
    const val DO_NOT_SHOW_SKIP_WARNING = "com.hrd.localvoice.DO_NOT_SHOW_SKIP_WARNING"
    const val DO_NOT_SHOW_TRANSCRIPTION_SKIP_WARNING =
        "com.hrd.localvoice.DO_NOT_SHOW_TRANSCRIPTION_SKIP_WARNING"
    const val IGNORED_UPDATE_VERSION = "com.hrd.localvoice.DO_NOT_UPDATE_WARNING"

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

object ConversionStatus {
    const val RETRY = "RETRY"
    const val CONVERTED = "CONVERTED"
    const val FAILED = "FAILED"
    const val NEW = "NEW"
}

object TranscriptionStatus {
    const val PENDING = "PENDING"
    const val TRANSCRIBED = "TRANSCRIBED"
    const val CORRECTED = "CORRECTED"
    const val UPLOADED = "UPLOADED"
}

object TranscriptionType {
    const val NEW = "NEW"
    const val RESOLUTION = "RESOLUTION"
}