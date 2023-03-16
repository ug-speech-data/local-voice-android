package com.hrd.localvoice.workers

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.utils.Constants.AUDIO_STATUS_UPLOADED
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File


class UploadWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val tag = "UploadWorker"
    private val apiService = RestApiFactory.create(context)
    val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE

    override fun doWork(): Result {
        uploadPendingAudios()
        return Result.success()
    }

    private fun uploadPendingAudios() {
        val audios = database?.AudioDao()?.getPendingAudios()

        audios?.forEach { audio ->
            var participant: Participant? = null
            if (audio.participantId != null) {
                participant = AppRoomDatabase.INSTANCE?.ParticipantDao()
                    ?.getParticipantNow(audio.participantId!!)
            }

            val gson = Gson()
            val audioString: String = gson.toJson(audio)

            if (participant != null) {
                participant.network = participant.network?.uppercase()
            }
            val participantString = gson.toJson(participant)

            val file = File(audio.localFileURl)
            if (file.exists() && (participant == null || participant.momoNumber != null)) {
                // Ensure that participants compensation details are filled before uploading audio
                // If audio is done by a participant
                // If participant is null, then the recording was done by the user themselves.
                val requestFile: RequestBody = RequestBody.create(MediaType.parse("audio/*"), file)
                val audioFile =
                    MultipartBody.Part.createFormData("audio_file", file.name, requestFile)

                val audioDataRequest =
                    RequestBody.create(MediaType.parse("text/plain"), audioString)
                val participantDataRequest =
                    RequestBody.create(MediaType.parse("text/plain"), participantString)

                val audioDataBody =
                    MultipartBody.Part.createFormData("audio_data", null, audioDataRequest)

                var participantDataBody: MultipartBody.Part? = null
                if (participant != null) {
                    participantDataBody = MultipartBody.Part.createFormData(
                        "participant_data", null, participantDataRequest
                    )
                }

                try {
                    val response =
                        apiService?.uploadAudioFile(audioFile, audioDataBody, participantDataBody)
                            ?.execute()
                    if (response?.body()?.success == true) {
                        audio.status = AUDIO_STATUS_UPLOADED
                        AppRoomDatabase.databaseWriteExecutor.execute {
                            database?.AudioDao()?.updateAudio(audio)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context, "Error: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                    Log.d(tag, "Error: ${e.message}")
                }
            }
        }
    }
}