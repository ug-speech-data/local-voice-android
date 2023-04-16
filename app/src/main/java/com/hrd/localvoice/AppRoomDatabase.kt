package com.hrd.localvoiceimport android.app.Applicationimport android.database.sqlite.SQLiteExceptionimport android.util.Logimport androidx.room.*import androidx.room.migration.AutoMigrationSpecimport androidx.room.migration.Migrationimport androidx.sqlite.db.SupportSQLiteDatabaseimport com.hrd.localvoice.dao.*import com.hrd.localvoice.models.*import com.hrd.localvoice.utils.Constantsimport com.hrd.localvoice.utils.StringListConvectorimport java.util.concurrent.ExecutorServiceimport java.util.concurrent.Executors@Database(    entities = [Audio::class, Participant::class, Image::class, Configuration::class, User::class, ActivityStatus::class, ValidationAudio::class],    version = 36,    autoMigrations = [        AutoMigration(from = 17, to = 18),        AutoMigration(from = 19, to = 20, spec = AppRoomDatabase.Migration19To20::class),        AutoMigration(from = 20, to = 21),        AutoMigration(from = 21, to = 22),        AutoMigration(from = 22, to = 23),        AutoMigration(from = 23, to = 24),        AutoMigration(from = 25, to = 26),        AutoMigration(from = 26, to = 27),        AutoMigration(from = 27, to = 28),        AutoMigration(from = 28, to = 29),        AutoMigration(from = 30, to = 31),        AutoMigration(from = 31, to = 32, spec = AppRoomDatabase.Migration31To32::class),        AutoMigration(from = 33, to = 34),        AutoMigration(from = 34, to = 35),        AutoMigration(from = 35, to = 36),    ])@TypeConverters(StringListConvector::class)abstract class AppRoomDatabase : RoomDatabase() {    abstract fun AudioDao(): AudioDao    abstract fun ParticipantDao(): ParticipantDao    abstract fun ImageDao(): ImageDao    abstract fun ConfigurationDao(): ConfigurationDao    abstract fun UserDao(): UserDao    abstract fun ValidationAudioDao(): ValidationAudioDao    companion object {        private const val LOCAL_LANG_DATABASE = "local_voice_database.sql"        private const val NUMBER_OF_THREADS = 4        private var mContext: Application? = null        var INSTANCE: AppRoomDatabase? = null        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)        private val sRoomDatabaseCallback: Callback = object : Callback() {            override fun onOpen(db: SupportSQLiteDatabase) {                super.onCreate(db)            }        }        fun getDatabase(context: Application): AppRoomDatabase? {            mContext = context            if (INSTANCE == null) {                synchronized(AppRoomDatabase::class.java) {                    if (INSTANCE == null) {                        INSTANCE = Room.databaseBuilder(                            context.applicationContext,                            AppRoomDatabase::class.java,                            LOCAL_LANG_DATABASE                        ).addMigrations(                            migration18To19,                            migration24To25,                            migration29To30,                            migration32To33,                        ).addCallback(sRoomDatabaseCallback).build()                    }                }            }            return INSTANCE        }        // Migrations        val migration18To19 = object : Migration(18, 19) {            override fun migrate(database: SupportSQLiteDatabase) {                database.execSQL("CREATE TABLE IF NOT EXISTS activity_status (id INTEGER NOT NULL PRIMARY KEY)")            }        }        // Migrations        val migration32To33 = object : Migration(32, 33) {            override fun migrate(database: SupportSQLiteDatabase) {                database.execSQL("CREATE TABLE IF NOT EXISTS ${Constants.AUDIO_VALIDATION_TABLE} (id INTEGER NOT NULL PRIMARY KEY)")            }        }        val migration24To25 = object : Migration(24, 25) {            override fun migrate(database: SupportSQLiteDatabase) {                database.execSQL("DELETE FROM configurations")            }        }        val migration29To30 = object : Migration(29, 30) {            override fun migrate(database: SupportSQLiteDatabase) {                try {                    database.execSQL("ALTER TABLE audios  ADD localImageURl TEXT DEFAULT '';")                } catch (ex: SQLiteException) {                    Log.w("TEST", "Altering audios" + ": " + ex.message)                }                val cursor = database.query("SELECT * FROM audios;")                cursor.moveToFirst()                if (cursor.count > 0) {                    do {                        val remoteImageColumnIndex = cursor.getColumnIndex("remoteImageID")                        val idColumnIndex = cursor.getColumnIndex("id")                        val remoteImageID = cursor.getLong(remoteImageColumnIndex)                        val id = cursor.getLong(idColumnIndex)                        val imageCursor =                            database.query("SELECT * FROM images where remoteId='$remoteImageID' LIMIT 1;")                        if (imageCursor.count > 0 && imageCursor.moveToFirst()) {                            val localURlColumnIndex = imageCursor.getColumnIndex("localURl")                            val imageLocalURL = imageCursor.getString(localURlColumnIndex)                            database.execSQL("UPDATE audios SET localImageURl = '$imageLocalURL' WHERE id=$id;")                        }                    } while (cursor.moveToNext())                }            }        }    }    @RenameColumn(        tableName = Constants.CONFIGURATIONS_TABLE,        fromColumnName = "privacyPolicyStatementAudio",        toColumnName = "privacyPolicyStatementAudioRemoteUrl"    )    class Migration19To20 : AutoMigrationSpec    @RenameColumn(        tableName = Constants.AUDIOS_TABLE,        fromColumnName = "updatedAt",        toColumnName = "uploadCount"    )    class Migration31To32 : AutoMigrationSpec}