package com.peternaggschga.sleeptalk.domain.monitoring

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.NOTIFICATION_SERVICE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.peternaggschga.sleeptalk.MainActivity
import com.peternaggschga.sleeptalk.R

object AudioRecordFactory {
    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.ENCODING_PCM_FLOAT
    private const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT

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

object MonitoringServiceNotificationFactory {
    const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "MonitoringServiceChannel"

    fun getNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: make channel attributes more verbose
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Name",               // TODO: create string resource
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Description" // TODO: create string resource
            }
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val notificationPendingIntent =
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dashboard_black_24dp)   // TODO: create icon drawable resource
            .setContentTitle("MonitoringService")               // TODO: create string resource
            .setContentText("MonitoringService running")        // TODO: create string resource
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(notificationPendingIntent)
            .build()
    }
}
