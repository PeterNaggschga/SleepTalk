package com.peternaggschga.sleeptalk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope

class MonitoringService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val INTENT_TIME_EXTRA_TAG = "Time"
    }

    private lateinit var looper: Looper
    private lateinit var handler: MonitoringServiceHandler

    override fun onCreate() {
        super.onCreate()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Inform user of insufficient permissions
            throw SecurityException("Service can only be created if the RECORD_AUDIO permission has been granted!")
        }

        HandlerThread("MonitoringServiceThread", Process.THREAD_PRIORITY_AUDIO).apply {
            start()
            this@MonitoringService.looper = looper
            handler = MonitoringServiceHandler(this@MonitoringService, looper, lifecycleScope)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

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

        Message.obtain(
            handler,
            MonitoringServiceHandler.MESSAGE_ID_START_RECORDING,
        ).sendToTarget()

        val stopMessage = Message.obtain(
            handler,
            MonitoringServiceHandler.MESSAGE_ID_STOP_RECORDING
        )

        val stopTime = intent?.getLongExtra(INTENT_TIME_EXTRA_TAG, -1) ?: -1
        if (stopTime <= 0) {
            stopMessage.sendToTarget()
        } else {
            handler.sendMessageAtTime(stopMessage, stopTime)
        }

        return START_REDELIVER_INTENT
    }

    override fun stopService(name: Intent?): Boolean {
        TODO("send Stop-Message")
        return super.stopService(name)
    }
}
