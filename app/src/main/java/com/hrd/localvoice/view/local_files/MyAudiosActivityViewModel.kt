package com.hrd.localvoice.view.local_files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Image

class MyAudiosActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun getAudios(): LiveData<List<Audio>>? {
        return repository.getAudios()
    }

    fun getImages(): LiveData<List<Image>>? {
        return repository.getImages()
    }

    fun deleteAudio(audio: Audio) {
        repository.deleteAudio(audio)
    }


    fun deleteImage(image: Image) {
        return repository.deleteImage(image)
    }

}