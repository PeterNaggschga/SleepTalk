package com.peternaggschga.sleeptalk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer

class MonitoringServiceHandler(looper: Looper, context: Context) : Handler(looper) {
    companion object {
        const val MESSAGE_ID_START_RECORDING = 0
        const val MESSAGE_ATTRIBUTE_TIME = "StopTime"
    }

    private val audioRecord: AudioRecord

    init {
        // TODO: move building of audioRecord to BuildManager class
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Handler can only be created if the RECORD_AUDIO permission has been granted!")
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT

        val audioRecordBuilder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioRecord.Builder().setContext(context)
        } else {
            AudioRecord.Builder()
        })

        audioRecordBuilder.setAudioSource(
            if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                    .toBoolean()
            ) {
                MediaRecorder.AudioSource.UNPROCESSED
            } else {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            }
        )

        audioRecordBuilder.setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .build()
        )

        audioRecordBuilder.setBufferSizeInBytes(
            64 * AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                encoding
            )
        )

        audioRecord = audioRecordBuilder.build()

        if (audioRecord.state == AudioRecord.STATE_UNINITIALIZED) {
            throw IllegalStateException("AudioRecord could not be initialized!")
        }
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MESSAGE_ID_START_RECORDING -> {
                val stopTime = msg.data.getLong("StopTime")
                if (stopTime <= 0) {
                    throw IllegalArgumentException("Message must contain non-negative value at $MESSAGE_ATTRIBUTE_TIME!")
                }
                record(stopTime)
            }

            else -> throw IllegalArgumentException("Type of message unknown: " + msg.what)
        }
    }

    private fun record(endOfRecording: Long) {
        Handler(Looper.getMainLooper()).postAtTime({
            Log.d("MonitoringService", "stop")
            audioRecord.stop()
        }, endOfRecording)

        val audioBuffer = ByteBuffer.allocateDirect(4 * audioRecord.bufferSizeInFrames)

        Log.d("MonitoringService", "start $endOfRecording")

        audioRecord.startRecording()
        while (audioRecord.recordingState != AudioRecord.RECORDSTATE_STOPPED) {
            val offset = audioRecord.read(audioBuffer, 4 * 44100, AudioRecord.READ_BLOCKING)
            val byteArray = ByteArray(offset)
            audioBuffer.get(byteArray, audioBuffer.position(), -audioBuffer.position())
            Log.d("Microphone output", SystemClock.uptimeMillis().toString())
        }
        audioRecord.release()
        Log.d("MonitoringService", "done")
    }
}
