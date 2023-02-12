package com.hrd.localvoice.view.validations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.R
import com.hrd.localvoice.models.UploadedAudio
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.UploadedAudioResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ValidationActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RestApiFactory.create(application);
    val errorMessage = MutableLiveData<String>()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val audio = MutableLiveData<UploadedAudio>()
    private val context: Application = application
    val validationSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    val noMoreImages: MutableLiveData<Boolean> = MutableLiveData(false)

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
                errorMessage.value = t.message
                isLoading.value = false
            }
        })
    }

    fun validateDate(id: Long, status: String) {
        isLoading.value = true
        apiService?.validateAudio(id, status)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>, response: Response<ResponseBody?>
            ) {
                validationSuccess.value = response.body()?.string() != null
                isLoading.value = false
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                errorMessage.value = t.message
                isLoading.value = false
                validationSuccess.value = false
            }
        })
    }
}