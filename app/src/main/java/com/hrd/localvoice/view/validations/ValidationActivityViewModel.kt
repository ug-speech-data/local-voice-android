package com.hrd.localvoice.view.validations

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.R
import com.hrd.localvoice.models.UploadedAudio
import com.hrd.localvoice.models.User
import com.hrd.localvoice.models.ValidationAudio
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AudioValidationResponse
import com.hrd.localvoice.network.response_models.UploadedAudioResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ValidationActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ValidationActivityViewModel"
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

    fun getAssignedAudios(offset: Long) {
        isLoading.value = true
        errorMessage.value = ""
        apiService?.getAssignedAudios(offset)?.enqueue(object : Callback<UploadedAudioResponse?> {
            override fun onResponse(
                call: Call<UploadedAudioResponse?>, response: Response<UploadedAudioResponse?>
            ) {
                if (response.body()?.audio != null) {
                    audio.value = response.body()?.audio
                    noMoreImages.value = false
                } else {
                    errorMessage.value = context.getString(R.string.no_more_audios)
                    noMoreImages.value = true
                }
                isLoading.value = false
            }

            override fun onFailure(call: Call<UploadedAudioResponse?>, t: Throwable) {
                errorMessage.value = "Couldn't connect to server."
                isLoading.value = false
                Log.e(tag, "onFailure: ${t.message}")
            }
        })
    }

    fun getValidationAudios(): LiveData<List<ValidationAudio>>? {
        return repository.getValidationAudios()
    }

    fun getPendingAudioValidations(): LiveData<List<ValidationAudio>>? {
        return repository.getPendingAudioValidations()
    }

    fun validateDate(id: Long, status: String) {
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