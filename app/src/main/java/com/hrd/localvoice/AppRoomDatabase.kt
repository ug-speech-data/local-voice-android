package com.hrd.localvoiceimport android.app.Applicationimport androidx.room.Databaseimport androidx.room.Roomimport androidx.room.RoomDatabaseimport androidx.sqlite.db.SupportSQLiteDatabaseimport com.hrd.localvoice.dao.AudioDaoimport com.hrd.localvoice.dao.ConfigurationDaoimport com.hrd.localvoice.dao.ImageDaoimport com.hrd.localvoice.dao.ParticipantDaoimport com.hrd.localvoice.models.Audioimport com.hrd.localvoice.models.Configurationimport com.hrd.localvoice.models.Imageimport com.hrd.localvoice.models.Participantimport java.util.concurrent.ExecutorServiceimport java.util.concurrent.Executors@Database(    entities = [Audio::class, Participant::class, Image::class, Configuration::class],    version = 7,    exportSchema = false)abstract class AppRoomDatabase : RoomDatabase() {    abstract fun AudioDao(): AudioDao    abstract fun ParticipantDao(): ParticipantDao    abstract fun ImageDao(): ImageDao    abstract fun ConfigurationDao(): ConfigurationDao    companion object {        private const val LOCAL_LANG_DATABASE = "local_voice_database.sql"        private const val NUMBER_OF_THREADS = 4        private var mContext: Application? = null        var INSTANCE: AppRoomDatabase? = null        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)        private val sRoomDatabaseCallback: Callback = object : Callback() {            override fun onCreate(db: SupportSQLiteDatabase) {                super.onCreate(db)                databaseWriteExecutor.execute {//                    val audioDao: AudioDao = INSTANCE!!.AudioDao()//                    val audios: List<Audio> = DummyData().getDummyAudios()//                    audioDao.insertAudios(audios)////                    val images: List<Image> = DummyData().getDummyImages()//                    val imageDao = INSTANCE!!.ImageDao()//                    imageDao.deleteAll()//                    imageDao.insertImages(images)                }            }        }        fun getDatabase(context: Application): AppRoomDatabase? {            mContext = context            if (INSTANCE == null) {                synchronized(AppRoomDatabase::class.java) {                    if (INSTANCE == null) {                        INSTANCE = Room.databaseBuilder(                            context.applicationContext,                            AppRoomDatabase::class.java,                            LOCAL_LANG_DATABASE                        ).addCallback(sRoomDatabaseCallback)                            //@TODO: Remove this                            .fallbackToDestructiveMigration()                            .build()                    }                }            }            return INSTANCE        }    }}