package com.peternaggschga.sleeptalk.domain.monitoring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peternaggschga.sleeptalk.domain.soundfiles.Codec
import java.util.Locale

class MonitoringService : LifecycleService() {

    companion object {
        const val INTENT_TIME_EXTRA_TAG = "Time"
    }

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

            val codec = Codec.getCodec(
                getExternalFilesDir(null)
                    ?: throw IllegalStateException("No shared storage available for recording!")
            )
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH)

            handler = MonitoringServiceHandler(
                this@MonitoringService,
                looper,
                { recordings ->
                    val recordingArray = recordings.map { element -> element.frame }
                        .reduce { acc, rec -> acc.plus(rec) }

                    calendar.timeInMillis = recordings.first().start
                    codec.savePcmToFile(
                        recordingArray,
                        dateFormat.format(calendar.time)
                    )
                },
                lifecycleScope
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        ServiceCompat.startForeground(
            this,
            MonitoringServiceNotificationFactory.NOTIFICATION_ID,
            MonitoringServiceNotificationFactory.getNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )

        handler.sendEmptyMessage(MonitoringServiceHandler.MESSAGE_ID_START_RECORDING)

        val stopMessage = Message.obtain(
            handler,
            MonitoringServiceHandler.MESSAGE_ID_STOP_RECORDING,
            startId,
            startId
        )

        val stopTime = intent?.getLongExtra(INTENT_TIME_EXTRA_TAG, -1) ?: -1
        if (stopTime <= 0) {
            stopMessage.sendToTarget()
        } else {
            handler.sendMessageAtTime(stopMessage, stopTime)
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.sendEmptyMessage(MonitoringServiceHandler.MESSAGE_ID_STOP_RECORDING)
        handler.looper.quitSafely()
    }

    override fun stopService(name: Intent?): Boolean {
        handler.sendEmptyMessage(MonitoringServiceHandler.MESSAGE_ID_STOP_RECORDING)
        return super.stopService(name)
    }
}
