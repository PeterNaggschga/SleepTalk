package com.peternaggschga.sleeptalk.domain.monitoring

import android.app.Service
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MonitoringServiceHandler(
    private val caller: MonitoringService,
    looper: Looper = Looper.getMainLooper(),
    @OptIn(DelicateCoroutinesApi::class) private val recordingScope: CoroutineScope = GlobalScope
) : Handler(looper) {
    companion object {
        const val MESSAGE_ID_START_RECORDING = 0
        const val MESSAGE_ID_STOP_RECORDING = 1
    }

    private val audioRecord = AudioRecordFactory.getAudioRecord(caller)

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
                val startId = msg.arg1
                if (recordingJob?.isActive == true) {
                    recordingScope.launch {
                        audioRecord.stop()
                        recordingJob?.join()
                        audioRecord.release()
                        recordingJob = null
                        stopCaller(startId)
                    }
                } else {
                    stopCaller(startId)
                }
            }
            else -> throw IllegalArgumentException("Type of message unknown: " + msg.what)
        }
    }

    private fun stopCaller(startId: Int) {
        if (startId > 0 && caller.stopSelfResult(startId)) {
            caller.stopForeground(Service.STOP_FOREGROUND_REMOVE)
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
