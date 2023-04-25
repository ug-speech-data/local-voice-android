package com.hrd.localvoice.utils

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.models.Audio
import java.io.File


class AudioUtil {
    companion object {
        val tag = "AudioUtil"
        fun convert(audio: Audio, application: Application) {
            val inputFile = audio.localFileURl
            var outputFile = inputFile.split(".wav")[0] + ".mp3"
            outputFile = outputFile.replace("u${audio.remoteImageID}", "u${audio.userId}")
            when (val rc = FFmpeg.execute("-y -i $inputFile $outputFile")) {
                RETURN_CODE_SUCCESS -> {
                    val uri: Uri = Uri.parse(outputFile)
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(application, uri)
                    val durationStr =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val convertedDuration = durationStr?.toInt()?.div(1000)?.toLong()
                    val rawDuration = audio.duration

                    if (convertedDuration != null && rawDuration != null && convertedDuration == rawDuration) {
                        audio.localFileURl = outputFile
                        audio.conversionStatus = ConversionStatus.CONVERTED
                        AppRoomDatabase.databaseWriteExecutor.execute {
                            val id =
                                AppRoomDatabase.getDatabase(application)?.AudioDao()
                                    ?.updateAudio(audio)
                            if (id != null) {
                                // Delete wave file
                                val waveFile = File(inputFile)
                                if (waveFile.isFile) {
                                    waveFile.delete()
                                }
                            }
                        }
                    } else {
                        updateConversionStatus(audio, application)
                    }
                }
                RETURN_CODE_CANCEL -> {
                    Log.i(Config.TAG, "Command execution cancelled by user.");
                    updateConversionStatus(audio, application)
                }
                else -> {
                    Log.e(
                        Config.TAG, String.format(
                            "Command execution failed with rc=%d and the output below.", rc
                        )
                    );
                    Config.printLastCommandOutput(Log.INFO);
                    updateConversionStatus(audio, application)
                }
            }
        }

        private fun updateConversionStatus(audio: Audio, application: Application) {
            if (audio.conversionStatus == ConversionStatus.RETRY) {
                audio.conversionStatus = ConversionStatus.FAILED
            } else {
                audio.conversionStatus = ConversionStatus.RETRY
            }
            AppRoomDatabase.databaseWriteExecutor.execute {
                AppRoomDatabase.getDatabase(application)?.AudioDao()
                    ?.updateAudio(audio)
            }
        }
    }
}