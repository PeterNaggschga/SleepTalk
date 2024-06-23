package com.peternaggschga.sleeptalk.domain.monitoring

import android.app.Service
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitoringServiceHandler(
    private val caller: MonitoringService,
    looper: Looper = Looper.getMainLooper(),
    @OptIn(DelicateCoroutinesApi::class) private val recordingScope: CoroutineScope = GlobalScope,
    private val blockingRecordingDispatcher: CoroutineDispatcher = Dispatchers.IO
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
        val channel = AudioAccumulator.getInputChannel()
        val accumulator = AudioAccumulator(channel)

        val accumulationJob = recordingScope.launch {
            accumulator.accumulate()
        }
        audioRecord.startRecording()

        while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            // create buffer array for one second of PCM float values
            val audioFrameBuffer = FloatArray(AudioRecordFactory.SAMPLE_RATE)

            withContext(blockingRecordingDispatcher) {
                // blocking read one second of PCM float values into the audioFrameBuffer
                audioRecord.read(
                    audioFrameBuffer,
                    0,
                    audioFrameBuffer.size,
                    AudioRecord.READ_BLOCKING
                )
            }

            channel.send(audioFrameBuffer)
        }

        channel.close()
        accumulationJob.join()
    }
}
