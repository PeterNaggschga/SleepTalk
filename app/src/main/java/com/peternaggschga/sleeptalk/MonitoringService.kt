package com.peternaggschga.sleeptalk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.nio.ByteBuffer

class MonitoringService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val INTENT_TIME_EXTRA_TAG = "Time"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: make channel attributes more verbose
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Name",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Description"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val notificationPendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dashboard_black_24dp)   // TODO: create icon drawable resource
            .setContentTitle("MonitoringService")               // TODO: create string resource
            .setContentText("MonitoringService running")        // TODO: create string resource
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(notificationPendingIntent)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )

        HandlerThread("worker", Process.THREAD_PRIORITY_AUDIO).run {
            record(intent.getLongExtra(INTENT_TIME_EXTRA_TAG, SystemClock.uptimeMillis()))
        }

        return START_REDELIVER_INTENT
    }

    private fun record(endOfRecording: Long) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT

        val audioRecord = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioRecord.Builder().setContext(this)
        } else {
            AudioRecord.Builder()
        }).setAudioSource(
            if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                    .toBoolean()
            ) {
                MediaRecorder.AudioSource.UNPROCESSED
            } else {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            }
        ).setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .build()
        ).setBufferSizeInBytes(
            64 * AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                encoding
            )
        )
            .build()

        if (audioRecord.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.e("AudioRecord", "AudioRecord could not be initialized!")
            return
        }

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
