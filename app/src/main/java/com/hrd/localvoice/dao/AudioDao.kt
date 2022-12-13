package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Audio
import com.hrd.localvoice.utils.Constants.AUDIOS_TABLE
import com.hrd.localvoice.utils.Constants.UPLOAD_STATUS_PENDING

@Dao
interface AudioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAudio(audio: Audio): Long

    @Update()
    fun updateAudio(audio: Audio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAudios(audios: List<Audio>): LongArray

    @Query("DELETE FROM $AUDIOS_TABLE")
    fun deleteAll()

    @Delete
    fun deleteAudio(audio: Audio)

    @Query("SELECT * from $AUDIOS_TABLE ORDER BY timestamp DESC")
    fun getAudios(): LiveData<List<Audio>>

    @Query("SELECT * from $AUDIOS_TABLE WHERE status = '$UPLOAD_STATUS_PENDING' ORDER BY timestamp ASC")
    fun getPendingAudios(): List<Audio>

    @Query("SELECT * FROM $AUDIOS_TABLE WHERE id = :id")
    fun getAudio(id: Long): LiveData<Audio>
}