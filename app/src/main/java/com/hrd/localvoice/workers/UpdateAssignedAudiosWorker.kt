package com.hrd.localvoice.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.ValidationAudiosResponse
import com.hrd.localvoice.utils.BinaryFileDownloader
import com.hrd.localvoice.utils.Constants.AUDIO_STATUS_DOWNLOADED
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class UpdateAssignedAudiosWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val tag = "UpdateAssignedAudiosWorker"
        val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE
        val apiService = RestApiFactory.create(context)

        val completed = database?.ValidationAudioDao()?.validationAudiosExists() as Boolean
        apiService?.getAssignedAudios(!completed)
            ?.enqueue(object : Callback<ValidationAudiosResponse?> {
                override fun onResponse(
                    call: Call<ValidationAudiosResponse?>,
                    response: Response<ValidationAudiosResponse?>
                ) {
                    val audios = response.body()?.audios
                    var count = 0
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        if (audios?.isNotEmpty() == true) {
                            audios.forEach { audio ->
                                audio.updatedAt = System.currentTimeMillis()
                                audio.createdAt = System.currentTimeMillis()
                                audio.validatedStatus = UPLOAD_STATUS_PENDING
                                audio.assetsDownloadStatus = UPLOAD_STATUS_PENDING
                                database.ValidationAudioDao().insertAudioValidation(audio)
                                val audioUrl = audio.remoteAudioUrl
                                val imageUrl = audio.remoteImageUrl
                                count += 1
                                // Check if the image does not exist
                                var alreadyExistingAudio =
                                    database.ValidationAudioDao().getAudioValidation(audio.id)
                                if (alreadyExistingAudio.remoteAudioUrl != audio.remoteAudioUrl) {
                                    database.ValidationAudioDao().updateAudioValidation(audio)
                                    alreadyExistingAudio =
                                        database.ValidationAudioDao().getAudioValidation(audio.id)
                                }

                                if (alreadyExistingAudio.localImageUrl?.let { File(it).exists() } == true && alreadyExistingAudio.localAudioUrl?.let {
                                        File(
                                            it
                                        ).exists()
                                    } == true) {
                                    alreadyExistingAudio.assetsDownloadStatus =
                                        AUDIO_STATUS_DOWNLOADED
                                    database.ValidationAudioDao()
                                        .updateAudioValidation(alreadyExistingAudio)
                                }

                                // Download the audio file
                                if (alreadyExistingAudio.localAudioUrl?.let { File(it).exists() } != true) {
                                    // Local file does not exist
                                    val title = "${audio.id}-" +
                                            audio.remoteAudioUrl.split("/")[audio.remoteAudioUrl.split(
                                                "/"
                                            ).size - 1]

                                    // Start download
                                    val downloader = BinaryFileDownloader()
                                    val destinationName =
                                        downloader.download(context, audioUrl, title)
                                    if (destinationName != null) {
                                        // Update configuration
                                        audio.localAudioUrl = destinationName
                                        if (audio.localImageUrl?.isNotEmpty() == true) {
                                            audio.assetsDownloadStatus = AUDIO_STATUS_DOWNLOADED
                                        }
                                        database.ValidationAudioDao().updateAudioValidation(audio)
                                    }
                                }

                                // Download the image file
                                if (alreadyExistingAudio.localImageUrl?.let { File(it).exists() } != true) {
                                    // Local file does not exist
                                    val title = "${audio.id}-" +
                                            audio.remoteImageUrl.split("/")[audio.remoteImageUrl.split(
                                                "/"
                                            ).size - 1]

                                    // Start download
                                    val downloader = BinaryFileDownloader()
                                    val destinationName =
                                        downloader.download(context, imageUrl, title)
                                    if (destinationName != null) {
                                        // Update configuration
                                        audio.localImageUrl = destinationName
                                        if (audio.localAudioUrl?.isNotEmpty() == true) {
                                            audio.assetsDownloadStatus = AUDIO_STATUS_DOWNLOADED
                                        }
                                        database.ValidationAudioDao().updateAudioValidation(audio)
                                    }
                                }
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context, "No audios found.", Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Finished downloading $count assigned audios.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ValidationAudiosResponse?>, t: Throwable) {
                    Toast.makeText(
                        context, "Couldn't connect to server to download audios.", Toast.LENGTH_LONG
                    ).show()
                }
            })
        return Result.success()
    }
}