package com.hrd.localvoice.view.participants

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Participant

class ParticipantBioActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    val participants: LiveData<List<Participant>>?
        get() = repository.participants

    val configuration: LiveData<Configuration?>?
        get() = repository.configuration

    fun createParticipant(participant: Participant) {
        return repository.insertParticipant(participant)
    }

    fun getParticipantById(id: Long): Participant? {
        return repository.getParticipantById(id)
    }
}