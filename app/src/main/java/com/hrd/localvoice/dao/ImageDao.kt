package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.utils.Constants.IMAGES_TABLE

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertImage(image: Image): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertImages(images: List<Image>): LongArray

    @Query("DELETE FROM $IMAGES_TABLE")
    fun deleteAll()

    @Update()
    fun updateImage(image: Image)

    @Delete
    fun deleteImage(image: Image)

    @Query("SELECT * from $IMAGES_TABLE WHERE descriptionCount <:maxDescriptionCount AND localURl IS NOT NULL ORDER BY remoteId ASC")
    fun getImages(maxDescriptionCount: Int): LiveData<List<Image>>


    @Query("SELECT * from $IMAGES_TABLE ORDER BY remoteId ASC")
    fun getImages(): LiveData<List<Image>>

    @Query("SELECT * FROM $IMAGES_TABLE WHERE remoteId = :remoteId")
    fun getImage(remoteId: Long): Image
}