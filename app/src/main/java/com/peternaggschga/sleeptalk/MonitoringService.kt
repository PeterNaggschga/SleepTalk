package com.peternaggschga.sleeptalk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class MonitoringService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MonitoringServiceChannel"
        const val INTENT_TIME_EXTRA_TAG = "Time"
    }

    private lateinit var looper: Looper
    private lateinit var handler: MonitoringServiceHandler

    override fun onCreate() {
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
            handler = MonitoringServiceHandler(looper, this@MonitoringService)
        }
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

        TODO("call handler")

        return START_REDELIVER_INTENT
    }
}
