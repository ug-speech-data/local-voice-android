package com.hrd.localvoice.utils

import android.app.Application
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.models.Audio
import java.io.File

//import java.io.File

class AudioUtil {
    companion object {
        fun convert(audio: Audio, application: Application) {
            val inputFile = audio.localFileURl
            val outputFile = inputFile.split(".wav")[0] + ".mp3"
            when (val rc = FFmpeg.execute("-i $inputFile $outputFile")) {
                RETURN_CODE_SUCCESS -> {
                    audio.localFileURl = outputFile
                    AppRoomDatabase.databaseWriteExecutor.execute {
                        val id =
                            AppRoomDatabase.getDatabase(application)?.AudioDao()?.updateAudio(audio)
                        if (id != null) {
                            // Delete wave file
                            val waveFile = File(inputFile)
                            if (waveFile.isFile) {
                                waveFile.delete()
                            }
                        }
                    }
                }
                RETURN_CODE_CANCEL -> {
                    Log.i(Config.TAG, "Command execution cancelled by user.");
                }
                else -> {
                    Log.i(
                        Config.TAG, String.format(
                            "Command execution failed with rc=%d and the output below.", rc
                        )
                    );
                    Config.printLastCommandOutput(Log.INFO);
                }
            }
        }
    }
}