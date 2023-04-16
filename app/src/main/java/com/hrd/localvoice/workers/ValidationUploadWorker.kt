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


class ValidationUploadWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val tag = "ValidationUploadWorker"
    private val apiService = RestApiFactory.create(context)
    val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE

    override fun doWork(): Result {
        uploadValidatedAudios()
        return Result.success()
    }

    private fun uploadValidatedAudios() {
        val audios = database?.ValidationAudioDao()?.getValidatedAudios()
        audios?.forEach { audio ->
            audio.validatedStatus?.let {
                apiService?.validateAudio(audio.id, it)
                    ?.enqueue(object : Callback<AudioValidationResponse?> {
                        override fun onResponse(
                            call: Call<AudioValidationResponse?>,
                            response: Response<AudioValidationResponse?>
                        ) {
                            if (response.body()?.status == "success") {
                                // Remove audio
                                if (audio.localImageUrl?.let { it1 -> File(it1).exists() } == true) audio.localImageUrl?.let { it1 ->
                                    File(
                                        it1
                                    ).delete()
                                }
                                if (audio.localAudioUrl?.let { it1 -> File(it1).exists() } == true) audio.localAudioUrl?.let { it1 ->
                                    File(
                                        it1
                                    ).delete()
                                }

                                AppRoomDatabase.databaseWriteExecutor.execute {
                                    database?.ValidationAudioDao()?.delete(audio)
                                }
                            }
                        }

                        override fun onFailure(call: Call<AudioValidationResponse?>, t: Throwable) {
                            Log.d(tag, "uploadValidatedAudios: ${t.message}")
                        }
                    })
            }
        }
    }
}