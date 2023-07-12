package com.hrd.localvoice.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AudioValidationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class TranscriptionUploadWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val tag = "TranscriptionUploadWorker"
    private val apiService = RestApiFactory.create(context)
    val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE

    override fun doWork(): Result {
        uploadTranscribedAudios()
        uploadCorrectedTranscribedAudios()
        return Result.success()
    }

    private fun uploadTranscribedAudios() {
        val audios = database?.TranscriptionAudioDao()?.getTranscribedAudios()
        Log.d(tag, "uploadTranscribedAudios: ${audios?.size}")
        audios?.forEach { audio ->
            audio.text?.let {
                apiService?.submitTranscription(audio.id, it)
                    ?.enqueue(object : Callback<AudioValidationResponse?> {
                        override fun onResponse(
                            call: Call<AudioValidationResponse?>,
                            response: Response<AudioValidationResponse?>
                        ) {
                            if (response.body()?.status == "success") {
                                // Remove audio
                                if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
                                    File(it1).delete()
                                }
                                AppRoomDatabase.databaseWriteExecutor.execute {
                                    database?.TranscriptionAudioDao()?.delete(audio)
                                }
                            }
                        }

                        override fun onFailure(call: Call<AudioValidationResponse?>, t: Throwable) {
                            Log.d(tag, "uploadTranscribedAudios: ${t.message}")
                        }
                    })
            }
        }
    }

    private fun uploadCorrectedTranscribedAudios() {
        val audios = database?.TranscriptionAudioDao()?.getCorrectedTranscriptions()
        Log.d(tag, "uploadCorrectedTranscribedAudios: ${audios?.size}")
        audios?.forEach { audio ->
            audio.text?.let {
                apiService?.submitTranscriptionResolution(audio.id, it, "accepted")
                    ?.enqueue(object : Callback<AudioValidationResponse?> {
                        override fun onResponse(
                            call: Call<AudioValidationResponse?>,
                            response: Response<AudioValidationResponse?>
                        ) {
                            if (response.body()?.status == "success") {
                                // Remove audio
                                if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
                                    File(it1).delete()
                                }
                                AppRoomDatabase.databaseWriteExecutor.execute {
                                    database?.TranscriptionAudioDao()?.delete(audio)
                                }
                            }
                        }

                        override fun onFailure(call: Call<AudioValidationResponse?>, t: Throwable) {
                            Log.d(tag, "uploadTranscribedAudios: ${t.message}")
                        }
                    })
            }
        }
    }
}