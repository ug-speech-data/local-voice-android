package com.hrd.localvoice.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.TranscriptionAudiosResponse
import com.hrd.localvoice.utils.BinaryFileDownloader
import com.hrd.localvoice.utils.Constants
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class UpdateAssignedTranscriptionAudiosWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val tag = "UpdateAssignedTranscriptionAudiosWorker"
        val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE
        val apiService = RestApiFactory.create(context)

        val completed = database?.TranscriptionAudioDao()?.transcriptionAudiosExists() as Boolean
        apiService?.updateAssignedTranscriptionAudiosWorker(!completed)
            ?.enqueue(object : Callback<TranscriptionAudiosResponse?> {
                override fun onResponse(
                    call: Call<TranscriptionAudiosResponse?>,
                    response: Response<TranscriptionAudiosResponse?>
                ) {
                    val audios = response.body()?.audios
                    var count = 0
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        if (audios?.isNotEmpty() == true) {
                            audios.forEach { audio ->
                                audio.updatedAt = System.currentTimeMillis()
                                audio.createdAt = System.currentTimeMillis()
                                audio.transcriptionStatus = UPLOAD_STATUS_PENDING
                                audio.assetsDownloadStatus = UPLOAD_STATUS_PENDING
                                database.TranscriptionAudioDao().insertAudioTranscription(audio)
                                val audioUrl = audio.remoteAudioUrl
                                count += 1

                                var alreadyExistingAudio =
                                    database.TranscriptionAudioDao().getAudioTranscription(audio.id)
                                if (alreadyExistingAudio.remoteAudioUrl != audio.remoteAudioUrl) {
                                    database.TranscriptionAudioDao().updateAudioTranscription(audio)
                                    alreadyExistingAudio = database.TranscriptionAudioDao()
                                        .getAudioTranscription(audio.id)
                                }

                                // Download the audio file
                                if (alreadyExistingAudio.localAudioUrl?.let { File(it).exists() } != true) {
                                    // Local file does not exist
                                    val title =
                                        "${audio.id}-" + audio.remoteAudioUrl.split("/")[audio.remoteAudioUrl.split(
                                            "/"
                                        ).size - 1]

                                    // Start download
                                    val downloader = BinaryFileDownloader()
                                    val destinationName =
                                        downloader.download(context, audioUrl, title)
                                    if (destinationName != null) {
                                        // Update configuration
                                        audio.localAudioUrl = destinationName
                                        audio.assetsDownloadStatus =
                                            Constants.AUDIO_STATUS_DOWNLOADED
                                        database.TranscriptionAudioDao()
                                            .updateAudioTranscription(audio)
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

                override fun onFailure(call: Call<TranscriptionAudiosResponse?>, t: Throwable) {
                    Toast.makeText(
                        context, "Couldn't connect to server to download audios.", Toast.LENGTH_LONG
                    ).show()
                }
            })
        return Result.success()
    }
}