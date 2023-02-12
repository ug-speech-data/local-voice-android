package com.hrd.localvoice.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException


class BinaryFileDownloader() {
    private val client: OkHttpClient = OkHttpClient().newBuilder().build()

    fun download(context: Context, url: String, fileName: String): String? {
        var response: Response? = null
        val request: Request = Request.Builder().url(url).build()
        try {
            response = client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            Log.e("BinaryFileDownloader", "download: $e")
            return null
        }
        val responseBody: ResponseBody? = response?.body()
        val responseData = responseBody?.bytes()
        if (responseData != null) {
            val fOut: FileOutputStream =
                context.openFileOutput(fileName, AppCompatActivity.MODE_PRIVATE)
            fOut.write(responseData)
            fOut.close()
            return File(context.filesDir, fileName).absolutePath
        }
        return null
    }
}