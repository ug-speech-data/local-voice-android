package com.hrd.localvoice.view.myaudios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Audio

class MyAudiosActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun getAudios(): LiveData<List<Audio>>? {
        return repository.getAudios()
    }

    fun deleteAudio(audio: Audio) {
        repository.deleteAudio(audio)
    }
}