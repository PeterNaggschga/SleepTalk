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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MonitoringServiceHandler(
    context: Context,
    looper: Looper = Looper.getMainLooper(),
    @OptIn(DelicateCoroutinesApi::class) private val recordingScope: CoroutineScope = GlobalScope
) : Handler(looper) {
    companion object {
        const val MESSAGE_ID_START_RECORDING = 0
        const val MESSAGE_ID_STOP_RECORDING = 1
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

    private var recordingJob: Job? = null

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MESSAGE_ID_START_RECORDING -> {
                if (recordingJob != null) {
                    throw IllegalStateException("Recording is already running!")
                }
                recordingJob = recordingScope.launch { record() }
            }

            MESSAGE_ID_STOP_RECORDING -> {
                if (recordingJob?.isActive == true) {
                    recordingScope.launch {
                        audioRecord.stop()
                        recordingJob?.join()
                        audioRecord.release()
                        recordingJob = null
                    }
                }
            }
            else -> throw IllegalArgumentException("Type of message unknown: " + msg.what)
        }
    }

    private suspend fun record() {
        val audioBuffer = ByteBuffer.allocateDirect(4 * audioRecord.bufferSizeInFrames)
        Log.d("MonitoringService", "start")

        audioRecord.startRecording()
        while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val offset = audioRecord.read(audioBuffer, 4 * 44100, AudioRecord.READ_BLOCKING)
            val byteArray = ByteArray(offset)
            audioBuffer.get(byteArray, audioBuffer.position(), -audioBuffer.position())
            Log.d("Microphone output", SystemClock.uptimeMillis().toString())
            delay(800)
        }
        Log.d("MonitoringService", "done")
    }
}
