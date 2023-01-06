package com.hrd.localvoice.view.audiorecorder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.models.Participant


class RecorderActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun insertAudio(audio: Audio) {
        repository.insertAudio(audio)
    }

    fun getImages(maxDescriptionCount: Int): LiveData<List<Image>>? {
        return repository.getAssignedImages(maxDescriptionCount)
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