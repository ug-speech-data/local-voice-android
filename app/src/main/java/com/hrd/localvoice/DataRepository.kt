package com.hrd.localvoice

import android.app.Application
import androidx.lifecycle.LiveData
import com.hrd.localvoice.dao.*
import com.hrd.localvoice.models.*

class DataRepository(application: Application) {
    private var application: Application
    private var db: AppRoomDatabase?
    val configuration: LiveData<Configuration?>?
    val participants: LiveData<List<Participant>>?
    val user: LiveData<User?>?

    private val audioDao: AudioDao?
    private val participantDao: ParticipantDao?
    private val configurationDao: ConfigurationDao?
    private val imageDao: ImageDao?
    private val userDao: UserDao?
    private val validationAudioDao: ValidationAudioDao?
    private val transcriptionAudioDao: TranscriptionAudioDao?

    fun getAssignedImages(excludes: List<Long>): LiveData<List<Image>>? {
        return imageDao?.getImagesLive(excludes)
    }

    fun getValidationAudios(): LiveData<List<ValidationAudio>>? {
        return validationAudioDao?.getValidationAudios()
    }

    fun getTranscriptionAudios(): LiveData<List<TranscriptionAudio>>? {
        return transcriptionAudioDao?.getTranscriptionAudios()
    }

    fun getPendingAudioTranscriptions(): LiveData<List<TranscriptionAudio>>? {
        return transcriptionAudioDao?.getPendingAudioTranscriptions()
    }

    fun getPendingAudioValidations(): LiveData<List<ValidationAudio>>? {
        return validationAudioDao?.getPendingAudioValidations()
    }

    fun getImages(): LiveData<List<Image>>? {
        return imageDao?.getImages()
    }

    fun insertAudio(audio: Audio) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            audioDao?.insertAudio(audio)
        }
    }

    fun insertParticipant(participant: Participant) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            participantDao?.insertParticipant(participant)
        }
    }

    fun deleteAudio(audio: Audio) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            audioDao?.deleteAudio(audio)
        }
    }

    fun deleteImage(image: Image) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            imageDao?.deleteImage(image)
        }
    }

    fun getAudios(id: Long?): LiveData<List<Audio>>? {
        return audioDao?.getAudios() // Ignore filtering the audios per user.
    }

    fun updateImage(image: Image) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            imageDao?.updateImage(image)
        }
    }

    fun getParticipantById(id: Long): Participant? {
        return participantDao?.getParticipantSync(id)
    }

    fun getParticipantByIdAsync(id: Long): LiveData<Participant>? {
        return participantDao?.getParticipant(id)
    }

    fun getPendingParticipant(): LiveData<Participant>? {
        return participantDao?.getPendingParticipant()
    }

    fun getAudiosByParticipant(participantId: Long): LiveData<List<Audio>>? {
        return audioDao?.getAudiosByParticipantLive(participantId)
    }

    fun getTranscriptionToResolve(): LiveData<List<TranscriptionAudio>>? {
        return transcriptionAudioDao?.getTranscriptionToResolve()

    }

    init {
        db = AppRoomDatabase.getDatabase(application)
        this.application = application
        audioDao = db?.AudioDao()
        participantDao = db?.ParticipantDao()
        imageDao = db?.ImageDao()
        configurationDao = db?.ConfigurationDao()
        userDao = db?.UserDao()
        validationAudioDao = db?.ValidationAudioDao()
        transcriptionAudioDao = db?.TranscriptionAudioDao()

        participants = participantDao?.getParticipants()
        user = userDao?.getUserAsync()

        configuration = configurationDao?.getConfigurationAsync()
    }
}