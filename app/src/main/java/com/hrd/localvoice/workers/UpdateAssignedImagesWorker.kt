package com.hrd.localvoice.workers

import android.content.Context
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.ImagesResponse
import com.hrd.localvoice.utils.BinaryFileDownloader
import com.hrd.localvoice.utils.Functions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class UpdateAssignedImagesWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val tag = "Configuration"
        val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE
        val apiService = RestApiFactory.create(context)

        apiService?.getAssignedImages()?.enqueue(object : Callback<ImagesResponse?> {
            override fun onResponse(
                call: Call<ImagesResponse?>, response: Response<ImagesResponse?>
            ) {
                val images = response.body()?.images
                if (images != null && images.isNotEmpty()) {
                    images.forEach { image ->
                        if (image.remoteURL != null || image.localURl == null) {
                            AppRoomDatabase.databaseWriteExecutor.execute {
                                val sourceUrl = image.remoteURL
                                val extension =
                                    sourceUrl!!.split(".")[sourceUrl.split(".").size - 1]

                                // Accept only png,jpeg and jpg images
                                if (extension.isNotEmpty() && listOf(
                                        "png", "jpeg", "jpg"
                                    ).contains(extension)
                                ) {
                                    // Check if the image does not exist
                                    val alreadyExistingImage =
                                        database?.ImageDao()?.getImage(image.remoteId)
                                    val alreadyExistingLocalUrl: String =
                                        if (alreadyExistingImage?.localURl != null) alreadyExistingImage.localURl!! else ""

                                    if (alreadyExistingImage?.remoteURL != image.remoteURL || !File(
                                            alreadyExistingLocalUrl
                                        ).exists()
                                    ) {
                                        // Local file does not exist
                                        val title =
                                            "${image.remoteId}_${System.currentTimeMillis()}.$extension"

                                        // Start download
                                        Thread {
                                            val downloader = BinaryFileDownloader()
                                            val destinationName =
                                                downloader.download(context, sourceUrl, title)
                                            if (destinationName != null) {
                                                // Update configuration
                                                image.localURl = destinationName
                                                database?.ImageDao()?.insertImage(image)
                                            }
                                        }.start()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        context, "No images found: ${response.message()}", Toast.LENGTH_LONG
                    ).show()
                    if (response.code() == 401) {
                        // Remove token
                        Functions.removeUserToken(context)
                    }
                }
            }

            override fun onFailure(call: Call<ImagesResponse?>, t: Throwable) {
                Toast.makeText(
                    context, "Couldn't connect to server to download images.", Toast.LENGTH_LONG
                ).show()
            }
        })
        return Result.success()
    }
}