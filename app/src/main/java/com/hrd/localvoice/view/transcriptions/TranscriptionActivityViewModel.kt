package com.hrd.localvoice.view.transcriptions

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.models.UploadedAudio
import com.hrd.localvoice.models.User
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AudioValidationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TranscriptionActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "TranscriptionActivityViewModel"
    private val repository: DataRepository = DataRepository(application)
    private val apiService = RestApiFactory.create(application);
    val errorMessage = MutableLiveData<String>()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val audio = MutableLiveData<UploadedAudio>()
    private val context: Application = application
    val validationSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    val noMoreImages: MutableLiveData<Boolean> = MutableLiveData(false)

    val user: LiveData<User?>?
        get() = repository.user

    fun getTranscriptionAudios(): LiveData<List<TranscriptionAudio>>? {
        return repository.getTranscriptionAudios()
    }

    fun getPendingAudioTranscriptions(): LiveData<List<TranscriptionAudio>>? {
        return repository.getPendingAudioTranscriptions()
    }

    fun sendTranscription(id: Long, status: String) {
        isLoading.value = true
        apiService?.validateAudio(id, status)?.enqueue(object : Callback<AudioValidationResponse?> {
            override fun onResponse(
                call: Call<AudioValidationResponse?>, response: Response<AudioValidationResponse?>
            ) {
                validationSuccess.value = response.body()?.message != null
                isLoading.value = false
                if (response.body()?.message != null) {
                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AudioValidationResponse?>, t: Throwable) {
                errorMessage.value = "Couldn't connect to server."
                isLoading.value = false
                validationSuccess.value = false
                Log.e(tag, "onFailure: ${t.message}")
            }
        })
    }
}