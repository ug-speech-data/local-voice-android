package com.hrd.localvoice

import android.app.Application
import androidx.lifecycle.LiveData
import com.hrd.localvoice.dao.AudioDao
import com.hrd.localvoice.dao.ConfigurationDao
import com.hrd.localvoice.dao.ImageDao
import com.hrd.localvoice.dao.ParticipantDao
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.models.Participant

class DataRepository(application: Application) {
    private var application: Application
    private var db: AppRoomDatabase?

    private val audios: LiveData<List<Audio>>?
    val configuration: LiveData<Configuration?>?

    val participants: LiveData<List<Participant>>?

    private val audioDao: AudioDao?
    private val participantDao: ParticipantDao?
    private val configurationDao: ConfigurationDao?
    private val imageDao: ImageDao?

    fun getAssignedImages(maxDescriptionCount: Int): LiveData<List<Image>>? {
        return imageDao?.getImages(maxDescriptionCount)
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

    fun getAudios(): LiveData<List<Audio>>? {
        return audioDao?.getAudios()
    }


    fun updateImage(image: Image) {
        AppRoomDatabase.databaseWriteExecutor.execute {
            imageDao?.updateImage(image)
        }
    }

    init {
        db = AppRoomDatabase.getDatabase(application)
        this.application = application
        audioDao = db?.AudioDao()
        participantDao = db?.ParticipantDao()
        imageDao = db?.ImageDao()
        configurationDao = db?.ConfigurationDao()

        audios = audioDao?.getAudios()
        participants = participantDao?.getParticipants()

        configuration = configurationDao?.getConfigurationAsync()
    }
}