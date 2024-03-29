package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.AUDIOS_TABLE
import com.hrd.localvoice.utils.Constants.AUDIO_STATUS_UPLOADED
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING

@Dao
interface AudioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAudio(audio: Audio): Long

    @Update
    fun updateAudio(audio: Audio): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAudios(audios: List<Audio>): LongArray

    @Query("DELETE FROM $AUDIOS_TABLE")
    fun deleteAll()

    @Query("DELETE FROM $AUDIOS_TABLE WHERE participantId = :id")
    fun deleteAudiosFromParticipantId(id: Long)

    @Delete
    fun deleteAudio(audio: Audio)

    @Query("SELECT * from $AUDIOS_TABLE ORDER BY timestamp DESC")
    fun getAudios(): LiveData<List<Audio>>

    @Query("SELECT * from $AUDIOS_TABLE WHERE status = '$UPLOAD_STATUS_PENDING' ORDER BY timestamp ASC")
    fun getPendingAudios(): List<Audio>

    @Query("SELECT * from $AUDIOS_TABLE WHERE status = '$AUDIO_STATUS_UPLOADED' ORDER BY uploadCount ASC, timestamp ASC")
    fun getUploadedAudios(): List<Audio>

    @Query("SELECT * from $AUDIOS_TABLE WHERE status != '$UPLOAD_STATUS_PENDING' ORDER BY uploadCount ASC, timestamp ASC")
    fun getAudiosNotPendingUpload(): List<Audio>

    @Query("SELECT * FROM $AUDIOS_TABLE WHERE id = :id")
    fun getAudio(id: Long): LiveData<Audio>

    @Query("SELECT * FROM $AUDIOS_TABLE WHERE participantId = :participantId")
    fun getAudiosByParticipant(participantId: Long): List<Audio>

    @Query("SELECT * FROM $AUDIOS_TABLE WHERE participantId is NUll")
    fun getAudiosByUser(): List<Audio>

    @Query("SELECT EXISTS(SELECT * FROM $AUDIOS_TABLE WHERE localFileURl LIKE :fileName OR remoteId = :remoteId)")
    fun checkAudioWithFileNameExists(fileName: String, remoteId: Long): Boolean

    @Query("SELECT * FROM $AUDIOS_TABLE WHERE participantId = :participantId")
    fun getAudiosByParticipantLive(participantId: Long): LiveData<List<Audio>>
}