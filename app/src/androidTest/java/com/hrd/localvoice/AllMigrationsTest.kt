package com.hrd.localvoice

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "test"

@RunWith(AndroidJUnit4::class)
class AllMigrationsTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppRoomDatabase::class.java,
        listOf(AppRoomDatabase.Migration19To20()),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testAllMigrations() {
        helper.createDatabase(DB_NAME, 17).apply { close() }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppRoomDatabase::class.java,
            DB_NAME
        ).addMigrations(
            AppRoomDatabase.migration18To19,
            AppRoomDatabase.migration24To25,
            AppRoomDatabase.migration29To30
        )
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }
}