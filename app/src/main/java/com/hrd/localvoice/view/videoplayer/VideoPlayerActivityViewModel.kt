package com.hrd.localvoice.view.videoplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image

class VideoPlayerActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }
}