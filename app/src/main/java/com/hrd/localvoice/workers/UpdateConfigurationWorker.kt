package com.hrd.localvoice.workers

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.network.response_models.ConfigurationResponse
import com.hrd.localvoice.utils.BinaryFileDownloader
import com.hrd.localvoice.utils.Functions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class UpdateConfigurationWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val tag = "UpdateConfigurationWorker"
        val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE
        val apiService = RestApiFactory.create(context)
        apiService?.getConfigurations()?.enqueue(object : Callback<ConfigurationResponse?> {
            override fun onResponse(
                call: Call<ConfigurationResponse?>, response: Response<ConfigurationResponse?>
            ) {
                val remoteConfiguration = response.body()?.configuration
                if (remoteConfiguration != null && database != null) {
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        var configuration = database.ConfigurationDao().getConfiguration()
                        if (configuration != null) {
                            remoteConfiguration.demoVideoLocalUrl = configuration.demoVideoLocalUrl
                            remoteConfiguration.privacyPolicyStatementAudioLocalUrl =
                                configuration.privacyPolicyStatementAudioLocalUrl
                        }

                        // Put new configuration into database / Update
                        database.ConfigurationDao().insertConfiguration(remoteConfiguration)
                        configuration = database.ConfigurationDao().getConfiguration()!!

                        if ((configuration.demoVideoRemoteUrl != remoteConfiguration.demoVideoRemoteUrl) || !File(
                                configuration.demoVideoLocalUrl
                            ).exists()
                        ) {
                            // Downloading new demo video
                            val destinationPath = remoteConfiguration.demoVideoRemoteUrl
                            val extension =
                                destinationPath.split(".")[destinationPath.split(".").size - 1]

                            val title = "demo_video_${System.currentTimeMillis()}.$extension"
                            Thread {
                                val downloader = BinaryFileDownloader()
                                val destinationName =
                                    downloader.download(context, destinationPath, title)

                                if (destinationName != null) {
                                    // Delete old video
                                    if (File(configuration.demoVideoLocalUrl).exists()) {
                                        File(configuration.demoVideoLocalUrl).delete()
                                    }

                                    // Update configuration
                                    configuration.demoVideoLocalUrl = destinationName
                                    configuration.demoVideoRemoteUrl =
                                        remoteConfiguration.demoVideoRemoteUrl
                                    database.ConfigurationDao().updateConfiguration(configuration)
                                }
                            }.start()
                        }

                        // Downloading privacy statement audio
                        val audioDestination =
                            remoteConfiguration.privacyPolicyStatementAudioRemoteUrl
                        if (audioDestination != null) {
                            if ((configuration.privacyPolicyStatementAudioRemoteUrl != remoteConfiguration.privacyPolicyStatementAudioRemoteUrl) || configuration.privacyPolicyStatementAudioLocalUrl?.let {
                                    File(
                                        it
                                    ).exists()
                                } != true) {
                                val audioExtension =
                                    audioDestination.split(".")[audioDestination.split(".").size - 1]
                                val audioTitle =
                                    "privacy_statement_audio_${System.currentTimeMillis()}.$audioExtension"
                                Thread {
                                    val downloader = BinaryFileDownloader()
                                    val destinationName = downloader.download(
                                        context, audioDestination, audioTitle
                                    )

                                    if (destinationName != null) {
                                        // Delete old audio file
                                        if (configuration.privacyPolicyStatementAudioLocalUrl?.let {
                                                File(
                                                    it
                                                ).exists()
                                            } == true) {
                                            File(configuration.privacyPolicyStatementAudioLocalUrl!!).delete()
                                        }

                                        // Update configuration
                                        configuration.privacyPolicyStatementAudioLocalUrl =
                                            destinationName
                                        database.ConfigurationDao()
                                            .insertConfiguration(configuration)
                                    } else {
                                        Log.d(tag, "download fail: ")
                                    }
                                }.start()
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ConfigurationResponse?>, t: Throwable) {
                Toast.makeText(
                    context, "Couldn't update app configurations", Toast.LENGTH_LONG
                ).show()
            }
        })

        // Update user's profile from server: e.g., get latest permissions changes.
        apiService?.getProfile()?.enqueue(object : Callback<AuthenticationResponse?> {
            override fun onResponse(
                call: Call<AuthenticationResponse?>, response: Response<AuthenticationResponse?>
            ) {
                // Store user in the database
                if (response.body()?.user != null) {
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        database?.UserDao()?.insertUser(response.body()!!.user!!)
                    }
                } else if (response.code() == 401) {
                    Functions.removeUserToken(context)
                }
            }

            override fun onFailure(call: Call<AuthenticationResponse?>, t: Throwable) {
                Log.e(tag, "onFailure: ${t.message}")
            }
        })
        return Result.success()
    }
}