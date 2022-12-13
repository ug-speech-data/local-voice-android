package com.hrd.localvoice.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun getImages(maxDescriptionCount: Int): LiveData<List<Image>>? {
        return repository.getAssignedImages(maxDescriptionCount)
    }

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }
}