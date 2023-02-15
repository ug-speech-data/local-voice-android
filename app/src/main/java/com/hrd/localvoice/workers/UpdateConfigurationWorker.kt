package com.hrd.localvoice.workers

import android.content.Context
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.network.response_models.ConfigurationResponse
import com.hrd.localvoice.utils.BinaryFileDownloader
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class UpdateConfigurationWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val tag = "TESTCONF"
        val database: AppRoomDatabase? = AppRoomDatabase.INSTANCE
        val apiService = RestApiFactory.create(context)

        apiService?.getConfigurations()?.enqueue(object : Callback<ConfigurationResponse?> {
            override fun onResponse(
                call: Call<ConfigurationResponse?>, response: Response<ConfigurationResponse?>
            ) {
                val remoteConfiguration = response.body()?.configuration
                if (remoteConfiguration != null) {
                    remoteConfiguration.demoVideoLocalUrl = ""
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        var configuration = database?.ConfigurationDao()?.getConfiguration()

                        if (configuration == null) {
                            // Put new configuration into database
                            database?.ConfigurationDao()?.insertConfiguration(remoteConfiguration)
                            configuration = remoteConfiguration
                        }

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
                                    database?.ConfigurationDao()?.updateConfiguration(configuration)
                                }
                            }.start()
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Configurations are not found: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ConfigurationResponse?>, t: Throwable) {
                Toast.makeText(
                    context, "Couldn't update app configurations: ${t.message}", Toast.LENGTH_LONG
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
                }
            }
            override fun onFailure(call: Call<AuthenticationResponse?>, t: Throwable) {
                Toast.makeText(
                    context, "Couldn't update profile: ${t.message}", Toast.LENGTH_LONG
                ).show()
            }
        })
        return Result.success()
    }
}