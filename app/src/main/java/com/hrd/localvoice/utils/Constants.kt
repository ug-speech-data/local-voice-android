package com.hrd.localvoice.utils

object Constants {
    const val AUDIOS_TABLE = "audios"
    const val IMAGES_TABLE = "images"
    const val CONFIGURATIONS_TABLE = "configurations"
    const val PARTICIPANT_TABLE = "participants"
    const val UPLOAD_STATUS_PENDING = "PENDING"
    const val AUDIO_STATUS_UPLOADED = "UPLOADED"
    const val AUDIO_STATUS_UPLOAD_FAILED = "FAILED"

    const val USER_TOKEN = "com.hrd.localvoice.USER_TOKEN"
    const val SHARED_PREFS_FILE = "localvoice-pf"
    const val USER_ID = "com.hrd.localvoice.USER_ID"
    const val IS_NEW_USER = "com.hrd.localvoice.IS_NEW_USER"
    const val USER_OBJECT = "com.hrd.localvoice.USER_OBJECT"

//    const val TEST_BASE_API_URL: String = "https://745a-197-255-119-9.ngrok.io/api/"
//    const val TEST_BASE_API_URL: String = "https://localvoice.pythonanywhere.com/api/"

    const val TEST_BASE_API_URL: String = "http://192.168.8.100:8000/api/"

    const val LIVE_BASE_API_URL: String = "https://localvoice.pythonanywhere.com/api/"
}