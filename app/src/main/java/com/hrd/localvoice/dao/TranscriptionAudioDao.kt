package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.TranscriptionAudio
import com.hrd.localvoice.utils.Constants.AUDIO_TRANSCRIPTION_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING
import com.hrd.localvoice.utils.TranscriptionStatus

@Dao
interface TranscriptionAudioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAudioTranscription(audio: TranscriptionAudio): Long

    @Update
    fun updateAudioTranscription(audio: TranscriptionAudio): Int

    @Query("DELETE FROM $AUDIO_TRANSCRIPTION_TABLE")
    fun deleteAll()

    @Delete
    fun delete(audio: TranscriptionAudio): Int

    @Delete
    fun delete(audios: List<TranscriptionAudio>): Int

    @Query("SELECT * FROM $AUDIO_TRANSCRIPTION_TABLE where createdAt < :deadlineInMills AND transcriptionStatus = '$UPLOAD_STATUS_PENDING'")
    fun getExpiredAudios(deadlineInMills: Long): List<TranscriptionAudio>

    @Query("SELECT * from $AUDIO_TRANSCRIPTION_TABLE ORDER BY createdAt DESC")
    fun getTranscriptionAudios(): LiveData<List<TranscriptionAudio>>

    @Query("SELECT * from $AUDIO_TRANSCRIPTION_TABLE WHERE transcriptionStatus = '$UPLOAD_STATUS_PENDING' ORDER BY createdAt ASC")
    fun getPendingAudioTranscriptions(): LiveData<List<TranscriptionAudio>>

    @Query("SELECT * from $AUDIO_TRANSCRIPTION_TABLE WHERE transcriptionStatus = '$UPLOAD_STATUS_PENDING' ORDER BY createdAt ASC")
    fun getSyncPendingAudioTranscriptions(): List<TranscriptionAudio>

    @Query("SELECT * from $AUDIO_TRANSCRIPTION_TABLE WHERE transcriptionStatus == '${TranscriptionStatus.TRANSCRIBED}' ORDER BY createdAt ASC")
    fun getTranscribedAudios(): List<TranscriptionAudio>

    @Query("SELECT * FROM $AUDIO_TRANSCRIPTION_TABLE WHERE id = :id")
    fun getAudioTranscription(id: Long): TranscriptionAudio

    @Query("SELECT EXISTS (SELECT * FROM $AUDIO_TRANSCRIPTION_TABLE)")
    fun transcriptionAudiosExists(): Boolean
}