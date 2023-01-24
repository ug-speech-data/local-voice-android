package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.User
import com.hrd.localvoice.utils.Constants.USER_TABLE

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(configuration: User): Long

    @Update()
    fun updateUser(configuration: User)

    @Query("DELETE FROM $USER_TABLE")
    fun deleteAll()

    @Query("SELECT * from $USER_TABLE ORDER BY id ASC LIMIT 1")
    fun getUserAsync(): LiveData<User?>?

    @Query("SELECT * from $USER_TABLE ORDER BY id ASC LIMIT 1")
    fun getUser(): User
}