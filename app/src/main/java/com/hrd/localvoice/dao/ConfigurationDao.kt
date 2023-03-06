package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.utils.Constants.CONFIGURATIONS_TABLE

@Dao
interface ConfigurationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConfiguration(configuration: Configuration): Long

    @Update()
    fun updateConfiguration(configuration: Configuration)

    @Query("DELETE FROM $CONFIGURATIONS_TABLE")
    fun deleteAll()

    @Query("SELECT * from $CONFIGURATIONS_TABLE ORDER BY id ASC LIMIT 1")
    fun getConfigurationAsync(): LiveData<Configuration?>?


    @Query("SELECT * from $CONFIGURATIONS_TABLE ORDER BY id ASC LIMIT 1")
    fun getConfiguration(): Configuration?
}