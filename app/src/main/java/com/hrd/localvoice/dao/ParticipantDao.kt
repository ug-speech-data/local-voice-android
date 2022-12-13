package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Participant
import com.hrd.localvoice.utils.Constants.PARTICIPANT_TABLE

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParticipant(participant: Participant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParticipants(participants: List<Participant>): LongArray

    @Query("DELETE FROM $PARTICIPANT_TABLE")
    fun deleteAll()

    @Delete
    fun deleteParticipant(participant: Participant)

    @Query("SELECT * from $PARTICIPANT_TABLE ORDER BY id ASC")
    fun getParticipants(): LiveData<List<Participant>>

    @Query("SELECT * FROM $PARTICIPANT_TABLE WHERE id = :id")
    fun getParticipant(id: Long): LiveData<Participant>
}