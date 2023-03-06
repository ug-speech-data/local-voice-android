package com.hrd.localvoice

import android.app.Application
import androidx.lifecycle.LiveData
import com.hrd.localvoice.dao.*
import com.hrd.localvoice.models.*

class DataRepository(application: Application) {
    private var application: Application
    private var db: AppRoomDatabase?

    private val audios: LiveData<List<Audio>>?
    val configuration: LiveData<Configuration?>?

    val participants: LiveData<List<Participant>>?
    val user: LiveData<User?>?

    private val audioDao: AudioDao?
    private val participantDao: ParticipantDao?
    private val configurationDao: ConfigurationDao?
    private val imageDao: ImageDao?
    private val userDao: UserDao?

    fun getAssignedImages(excludes: List<Long>): LiveData<List<Image>>? {
        return imageDao?.getImages(excludes)
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

    fun getAudios(): LiveData<List<Audio>>? {
        return audioDao?.getAudios()
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
        return audioDao?.getAudiosByParticipant(participantId)
    }

    init {
        db = AppRoomDatabase.getDatabase(application)
        this.application = application
        audioDao = db?.AudioDao()
        participantDao = db?.ParticipantDao()
        imageDao = db?.ImageDao()
        configurationDao = db?.ConfigurationDao()
        userDao = db?.UserDao()

        audios = audioDao?.getAudios()
        participants = participantDao?.getParticipants()
        user = userDao?.getUserAsync()

        configuration = configurationDao?.getConfigurationAsync()
    }
}