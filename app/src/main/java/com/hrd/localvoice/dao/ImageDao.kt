package com.hrd.localvoice.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hrd.localvoice.models.Image
import com.hrd.localvoice.utils.Constants.IMAGES_TABLE

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(image: Image): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImages(images: List<Image>): LongArray

    @Query("DELETE FROM $IMAGES_TABLE")
    fun deleteAll()

    @Update()
    fun updateImage(image: Image)

    @Delete
    fun deleteImage(image: Image)

    @Query("SELECT * from $IMAGES_TABLE WHERE remoteId NOT IN (:excludes) AND localURl IS NOT NULL ORDER BY descriptionCount ASC")
    fun getImages(excludes: List<Long>): List<Image>

    @Query("SELECT * from $IMAGES_TABLE WHERE remoteId NOT IN (:excludes) AND localURl IS NOT NULL ORDER BY  descriptionCount ASC, remoteId ASC")
    fun getImagesLive(excludes: List<Long>): LiveData<List<Image>>


    @Query("SELECT * from $IMAGES_TABLE ORDER BY remoteId ASC")
    fun getImages(): LiveData<List<Image>>

    @Query("SELECT * FROM $IMAGES_TABLE WHERE remoteId = :remoteId")
    fun getImage(remoteId: Long): Image?
}