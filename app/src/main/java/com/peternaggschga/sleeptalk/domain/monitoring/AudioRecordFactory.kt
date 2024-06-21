package com.peternaggschga.sleeptalk.domain.monitoring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ActivityCompat

class AudioRecordFactory {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.ENCODING_PCM_FLOAT
        const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT

        fun getAudioRecord(context: Context): AudioRecord {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Handler can only be created if the RECORD_AUDIO permission has been granted!")
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING)
                    .build()
            )

            audioRecordBuilder.setBufferSizeInBytes(
                64 * AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    ENCODING
                )
            )

            val audioRecord = audioRecordBuilder.build()

            if (audioRecord.state == AudioRecord.STATE_UNINITIALIZED) {
                throw IllegalStateException("AudioRecord could not be initialized!")
            }
            return audioRecord
        }
    }
}
