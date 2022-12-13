package com.hrd.localvoice.view.participants

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Participant

class ParticipantBioActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    private val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    val participants: LiveData<List<Participant>>?
        get() = repository.participants

    fun createParticipant(participant: Participant) {
        return repository.insertParticipant(participant)
    }

}