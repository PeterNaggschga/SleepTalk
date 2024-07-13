package com.peternaggschga.sleeptalk.domain.monitoring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.icu.util.Calendar
import android.os.Binder
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Process
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peternaggschga.sleeptalk.domain.soundfiles.Codec
import java.util.Date

class MonitoringService : LifecycleService() {

    companion object {
        const val INTENT_TIME_EXTRA_TAG = "Time"
    }

    private lateinit var handler: MonitoringServiceHandler

    var stopTime: Date? = null
        private set

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

            handler = MonitoringServiceHandler(
                this@MonitoringService,
                looper,
                codec,
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

        val stopTimeMillis = intent?.getLongExtra(INTENT_TIME_EXTRA_TAG, -1) ?: -1
        if (stopTimeMillis <= 0) {
            stopMessage.sendToTarget()
        } else {
            handler.sendMessageAtTime(stopMessage, stopTimeMillis)
            stopTime = Calendar.getInstance().apply {
                add(Calendar.MILLISECOND, (stopTimeMillis - SystemClock.uptimeMillis()).toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
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

    private val binder = MonitoringBinder()

    inner class MonitoringBinder : Binder() {
        fun getService() = this@MonitoringService
    }
}
