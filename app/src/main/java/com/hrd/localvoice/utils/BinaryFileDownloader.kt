package com.hrd.localvoice.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class BinaryFileDownloader() {
    private val client: OkHttpClient = OkHttpClient().newBuilder().build()

    @Throws(IOException::class)
    fun download(context: Context, url: String, fileName: String): String? {
        val request: Request = Request.Builder().url(url).build()
        val response: Response = client.newCall(request).execute()

        val responseBody: ResponseBody? = response.body()
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