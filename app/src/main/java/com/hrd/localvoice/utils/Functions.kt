package com.hrd.localvoice.utils

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.utils.Constants.USER_ID
import com.hrd.localvoice.utils.Constants.USER_OBJECT
import com.hrd.localvoice.utils.Constants.USER_TOKEN

class Functions {
    companion object {
        fun removeUserToken(context: Context) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
            prefs.edit().remove(USER_TOKEN).apply()
            prefs.edit().remove(USER_ID).apply()
        }

        fun getUserToken(context: Context): String? {
            val prefs: SharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
            return prefs.getString(USER_TOKEN, "")
        }

        fun getUserId(context: Context): Long {
            val prefs: SharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
            return prefs.getLong(USER_ID, -1L)
        }

        fun getLoggedInUser(context: Context): User? {
            val prefs: SharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
            val userString = prefs.getString(USER_OBJECT, "")
            val gson = Gson()
            return if (userString?.isNotEmpty() == true) gson.fromJson(
                userString, User::class.java
            ) else null;
        }

        fun downloadFile(
            context: Context, url: String, title: String,
        ) {
            try {
                val uri: Uri = Uri.parse(url) // Path where you want to download file.
                val request = DownloadManager.Request(uri)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI) // Tell on which network you want to download file.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // This will show notification on top when downloading the file.
                    // Title for notification.
                    .setTitle(title)
                    .setDescription("Please wait, downloading...")
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOCUMENTS, "LocalVoice/$title"
                    )
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?

                // Start downloading
                downloadManager?.enqueue(request)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getPathFromUri(context: Context, uri: Uri): String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor =
                context.contentResolver.query(uri, projection, null, null, null) ?: return null
            val columnIndex: Int = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val s: String = cursor.getString(columnIndex)
            cursor.close()
            return s
        }
    }

}