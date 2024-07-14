package com.peternaggschga.sleeptalk.domain.monitoring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.icu.util.Calendar
import android.os.Binder
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
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

    private lateinit var wakeLock: WakeLock

    private var stopTime: Date? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

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

        val stopDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(INTENT_TIME_EXTRA_TAG, Date::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.extras?.getSerializable(INTENT_TIME_EXTRA_TAG) as Date
        }
        if (stopDate?.before(Calendar.getInstance().time) != false) {
            stopMessage.sendToTarget()
        } else {
            stopTime = stopDate

            val millisUntilStop = stopDate.time - Calendar.getInstance().time.time
            handler.sendMessageAtTime(stopMessage, SystemClock.uptimeMillis() + millisUntilStop)

            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    this@MonitoringService.javaClass.name
                ).apply {
                    acquire(millisUntilStop + 1000)
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return MonitoringBinder()
    }

    override fun onDestroy() {
        onStopCleanup()
        super.onDestroy()
    }

    override fun stopService(name: Intent?): Boolean {
        onStopCleanup()
        return super.stopService(name)
    }

    private fun onStopCleanup() {
        if (::handler.isInitialized) {
            handler.sendEmptyMessage(MonitoringServiceHandler.MESSAGE_ID_STOP_RECORDING)
        }
        if (::wakeLock.isInitialized) {
            wakeLock.release()
        }
        stopTime = null
    }

    inner class MonitoringBinder : Binder() {
        val stopTime get() = this@MonitoringService.stopTime
    }
}
