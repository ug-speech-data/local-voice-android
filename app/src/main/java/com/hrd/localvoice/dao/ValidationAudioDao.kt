package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.ValidationAudio
import com.hrd.localvoice.utils.Constants.AUDIO_VALIDATION_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING

@Dao
interface ValidationAudioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAudioValidation(audio: ValidationAudio): Long

    @Update
    fun updateAudioValidation(audio: ValidationAudio): Int

    @Query("DELETE FROM $AUDIO_VALIDATION_TABLE")
    fun deleteAll()

    @Delete
    fun delete(audio: ValidationAudio): Int

    @Delete
    fun delete(audios: List<ValidationAudio>): Int

    @Query("SELECT * from $AUDIO_VALIDATION_TABLE ORDER BY createdAt DESC")
    fun getValidationAudios(): LiveData<List<ValidationAudio>>

    @Query("SELECT * from $AUDIO_VALIDATION_TABLE WHERE validatedStatus = '$UPLOAD_STATUS_PENDING' ORDER BY createdAt ASC")
    fun getPendingAudioValidations(): LiveData<List<ValidationAudio>>

    @Query("SELECT * from $AUDIO_VALIDATION_TABLE WHERE validatedStatus = '$UPLOAD_STATUS_PENDING' ORDER BY createdAt ASC")
    fun getSyncPendingAudioValidations(): List<ValidationAudio>

    @Query("SELECT * from $AUDIO_VALIDATION_TABLE WHERE validatedStatus != '$UPLOAD_STATUS_PENDING' ORDER BY createdAt ASC")
    fun getValidatedAudios(): List<ValidationAudio>

    @Query("SELECT * FROM $AUDIO_VALIDATION_TABLE WHERE id = :id")
    fun getAudioValidation(id: Long): ValidationAudio

    @Query("SELECT EXISTS (SELECT * FROM $AUDIO_VALIDATION_TABLE)")
    fun validationAudiosExists(): Boolean
}