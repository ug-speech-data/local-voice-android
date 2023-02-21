package com.hrd.localvoice.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.models.User

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    val user: LiveData<User?>?
        get() = repository.user

    fun getImages(maxDescriptionCount: Int): LiveData<List<Image>>? {
        return repository.getAssignedImages(maxDescriptionCount)
    }

    fun getAudios(): LiveData<List<Audio>>? {
        return repository.getAudios()
    }

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }
}