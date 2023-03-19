package com.hrd.localvoice.view.audiorecorder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.*


class RecorderActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)
    val descriptionCount: MutableLiveData<Int> = MutableLiveData(0)

    val user: LiveData<User?>?
        get() = repository.user

    fun insertAudio(audio: Audio) {
        repository.insertAudio(audio)
    }

    fun getImages(excludes: List<Long>): LiveData<List<Image>>? {
        return repository.getAssignedImages(excludes)
    }

    fun updateImage(image: Image) {
        return repository.updateImage(image)
    }

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }

    fun getParticipantById(id: Long): Participant? {
        return repository.getParticipantById(id)
    }
}