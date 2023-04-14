package com.hrd.localvoice.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs


class WaveRecorder2(private var context: Context, private var saveData: Boolean = false) {
    private val audioSampleRate = 44100
    private val recorderChannel = AudioFormat.CHANNEL_IN_STEREO
    private val channelCount = 2;
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val bitsPerSample = 16
    private val TAG = "WaveRecorder"
    var averageAmplitude = 0.0f

    private var mediaPlayer: MediaPlayer? = MediaPlayer()
    private var audioRecorder: AudioRecord? = null
    private var audioDurationInSeconds = 0.0f
    var maxContinuousSilentDurationInSeconds = 0.0f
    private val voiceAmplitude = 350.0f
    private lateinit var finalAudioBuffer: ByteArray
    var recorderState: Int? = null

    init {
        startRecordingThread()
    }

    private fun startRecordingThread() {
        val recordingThread = Thread {
            // Get the minimum buffer size required for the successful creation of an AudioRecord object.
            val minBufferSize = AudioRecord.getMinBufferSize(
                audioSampleRate, recorderChannel, audioEncoding
            )

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@Thread
            }

            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                audioSampleRate,
                recorderChannel,
                audioEncoding,
                minBufferSize
            )
            recorderState = audioRecorder?.state
            if (recorderState != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Can't initialise recorder.")
                return@Thread
            }

            // Start Recording.
            audioRecorder?.startRecording()
            maxContinuousSilentDurationInSeconds = 0F
            averageAmplitude = 0F

            val totalAllocatedAudioByte = 15000000 // 15MB max
            val readAudioBuffer = ByteArray(minBufferSize)
            var totalAudioData = ByteArray(totalAllocatedAudioByte)
            var totalReadBytes = 0
            var isVoiceDetected = false
            var currentContinuousSilentPeriods = 0f

            // Used for sample analysis.
            val tempFloatBuffer = FloatArray(3)
            var tempIndex = 0

            // While data come from microphone.
            while (audioRecorder != null) {
                val numberOfReadBytes = audioRecorder?.read(readAudioBuffer, 0, minBufferSize)
                if (numberOfReadBytes == null) {
                    Log.d(TAG, "startRecordingThread: Couldn't initialise recorder.")
                    break
                }

                // Analyse audio and if a good candidate, do these
                var totalAbsValue = 0.0f
                var i = 0
                while (i < numberOfReadBytes) {
                    val sample =
                        (readAudioBuffer[i].toInt() or (readAudioBuffer[i + 1].toInt() shl 8)).toShort()
                    totalAbsValue += abs(sample.toInt()) / (numberOfReadBytes / 2.0f)
                    i += 2
                }

                // Analyze temp buffer.
                tempFloatBuffer[tempIndex % tempFloatBuffer.size] = totalAbsValue
                var averageAmp = 0.0f
                for (v in tempFloatBuffer) {
                    averageAmp += v
                }
                // Update recorder's current average amplitude
                averageAmplitude = averageAmp

                tempIndex++
                audioDurationInSeconds =
                    (totalReadBytes / (audioSampleRate * channelCount * bitsPerSample / 8f))

                if (saveData) {
                    isVoiceDetected = averageAmp >= voiceAmplitude || isVoiceDetected
                    // Calculate the duration of silent periods
                    if (isVoiceDetected && averageAmp < voiceAmplitude) {
                        currentContinuousSilentPeriods += numberOfReadBytes / (audioSampleRate * channelCount * bitsPerSample / 8f)
                    } else {
                        currentContinuousSilentPeriods = 0f
                    }

                    // Compare duration of silent periods
                    maxContinuousSilentDurationInSeconds =
                        maxContinuousSilentDurationInSeconds.coerceAtLeast(
                            currentContinuousSilentPeriods
                        )

                    // Save read buffer into RAM
                    if (isVoiceDetected && numberOfReadBytes >= 0) {
                        System.arraycopy(
                            readAudioBuffer, 0, totalAudioData, totalReadBytes, numberOfReadBytes
                        )
                        totalReadBytes += numberOfReadBytes
                    }
                    if (totalReadBytes >= totalAllocatedAudioByte) {
                        break
                    }
                } else if (totalReadBytes > 0) {
                    finalAudioBuffer = ByteArray(44 + totalReadBytes)
                    val byteRate = (bitsPerSample * audioSampleRate * recorderChannel / 8).toLong()
                    constructWaveHeader(
                        totalReadBytes.toLong(),
                        (totalReadBytes + 36).toLong(),
                        audioSampleRate.toLong(),
                        channelCount.toByte(),
                        byteRate,
                        finalAudioBuffer
                    )
                    System.arraycopy(totalAudioData, 0, finalAudioBuffer, 44, totalReadBytes)
                    saveIntoSaveFile(finalAudioBuffer, null)

                    // Reset values
                    totalReadBytes = 0
                    totalAudioData = ByteArray(totalAllocatedAudioByte)
                    maxContinuousSilentDurationInSeconds = 0F
                    isVoiceDetected = false
                }
            }
        }
        recordingThread.start()
    }


    fun playBackRecording() {
        val fileName = File(context.filesDir, "temp.wav").absolutePath

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(fileName)
            mediaPlayer?.setOnPreparedListener { mp: MediaPlayer ->
                mp.start()
            }
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            Log.d(TAG, "onCreate: Can't open file: $fileName")
        }
    }

    fun audioDuration(): Long {
        return audioDurationInSeconds.toLong()
    }

    fun silentDuration(): Long {
        return maxContinuousSilentDurationInSeconds.toLong()
    }

    fun stopRecording() {
        saveData = false
        maxContinuousSilentDurationInSeconds = 0.0f
    }

    fun reset() {
        audioDurationInSeconds = 0.0f
        maxContinuousSilentDurationInSeconds = 0.0f
    }

    fun isRecording(): Boolean {
        return saveData
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
    }

    fun startRecording() {
        saveData = true
        maxContinuousSilentDurationInSeconds = 0.0f
    }

    private fun constructWaveHeader(
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Byte,
        byteRate: Long,
        finalBuffer: ByteArray
    ) {
        // RIFF/WAVE header
        finalBuffer[0] = 'R'.code.toByte()
        finalBuffer[1] = 'I'.code.toByte()
        finalBuffer[2] = 'F'.code.toByte()
        finalBuffer[3] = 'F'.code.toByte()

        // Total file size (4 bytes): totalAudioLen + 36
        finalBuffer[4] = (totalDataLen and 0xff).toByte()
        finalBuffer[5] = ((totalDataLen shr 8) and 0xff).toByte()
        finalBuffer[6] = ((totalDataLen shr 16) and 0xff).toByte()
        finalBuffer[7] = ((totalDataLen shr 24) and 0xff).toByte()
        finalBuffer[8] = 'W'.code.toByte()
        finalBuffer[9] = 'A'.code.toByte()
        finalBuffer[10] = 'V'.code.toByte()
        finalBuffer[11] = 'E'.code.toByte()
        finalBuffer[12] = 'f'.code.toByte() // 'fmt ' chunk
        finalBuffer[13] = 'm'.code.toByte()
        finalBuffer[14] = 't'.code.toByte()
        finalBuffer[15] = ' '.code.toByte()

        // Size of format chunk (4 bytes)
        finalBuffer[16] = (16 and 0xff).toByte()
        finalBuffer[17] = ((16 shr 8) and 0xff).toByte()
        finalBuffer[18] = ((16 shr 16) and 0xff).toByte()
        finalBuffer[19] = ((16 shr 24) and 0xff).toByte()

        // Compression code (2 bytes)
        finalBuffer[20] = 1
        finalBuffer[21] = 0

        // Number of channels (2 bytes)
        finalBuffer[22] = channels
        finalBuffer[23] = 0

        // Sample rate (4 bytes)
        finalBuffer[24] = (longSampleRate and 0xff).toByte()
        finalBuffer[25] = ((longSampleRate shr 8) and 0xff).toByte()
        finalBuffer[26] = ((longSampleRate shr 16) and 0xff).toByte()
        finalBuffer[27] = ((longSampleRate shr 24) and 0xff).toByte()

        //Bit rate (4 bytes)
        finalBuffer[28] = (byteRate and 0xff).toByte()
        finalBuffer[29] = ((byteRate shr 8) and 0xff).toByte()
        finalBuffer[30] = ((byteRate shr 16) and 0xff).toByte()
        finalBuffer[31] = ((byteRate shr 24) and 0xff).toByte()

        // Block align (2 bytes)
        // i.e., channel * bit-depth / 8
        finalBuffer[32] = (2 * 16 / 8).toByte()
        finalBuffer[33] = 0

        // Bit depth  (2 bytes)
        finalBuffer[34] = bitsPerSample.toByte()
        finalBuffer[35] = 0
        finalBuffer[36] = 'd'.code.toByte()
        finalBuffer[37] = 'a'.code.toByte()
        finalBuffer[38] = 't'.code.toByte()
        finalBuffer[39] = 'a'.code.toByte()
        finalBuffer[40] = (totalAudioLen and 0xff).toByte()
        finalBuffer[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        finalBuffer[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        finalBuffer[43] = ((totalAudioLen shr 24) and 0xff).toByte()
    }

    private fun saveIntoSaveFile(finalAudioData: ByteArray, fileName: String?): String? {
        val saveToFile = if (fileName == null) {
            File(context.filesDir, "temp.wav").absolutePath
        } else {
            File(context.filesDir, fileName).absolutePath
        }

        val out: FileOutputStream
        try {
            out = FileOutputStream(saveToFile)
            try {
                out.write(finalAudioData)
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e1: FileNotFoundException) {
            e1.printStackTrace()
        }
        return saveToFile
    }

    fun saveAudioIntoFile(fileName: String): String? {
        return saveIntoSaveFile(finalAudioBuffer, fileName)
    }

    fun isAudioPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun release() {
        mediaPlayer?.release()
        audioRecorder?.release()
        audioRecorder = null
        mediaPlayer = null
    }
}