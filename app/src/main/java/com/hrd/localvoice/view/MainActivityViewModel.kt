package com.hrd.localvoice.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.*

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    val user: LiveData<User?>?
        get() = repository.user

    fun getImages(): LiveData<List<Image>>? {
        return repository.getAssignedImages(listOf())
    }

    fun getValidationAudios(): LiveData<List<ValidationAudio>>? {
        return repository.getValidationAudios()
    }

    fun getAudios(): LiveData<List<Audio>>? {
        return repository.getAudios()
    }

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }
}