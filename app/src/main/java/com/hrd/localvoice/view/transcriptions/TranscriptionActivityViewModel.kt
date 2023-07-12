package com.hrd.localvoice.view.transcriptions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.models.TranscriptionResolutionAudio
import com.hrd.localvoice.models.UploadedAudio
import com.hrd.localvoice.models.User
import com.hrd.localvoice.network.RestApiFactory

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

    fun getTranscriptionResolutionAudios(): LiveData<List<TranscriptionAudio>>? {
        return repository.getTranscriptionToResolve()
    }
}