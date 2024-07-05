package com.peternaggschga.sleeptalk.domain.monitoring

import android.app.Service
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.peternaggschga.sleeptalk.domain.soundfiles.Codec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MonitoringServiceHandler(
    private val caller: MonitoringService,
    looper: Looper = Looper.getMainLooper(),
    private val codec: Codec,
    @OptIn(DelicateCoroutinesApi::class) private val recordingScope: CoroutineScope = GlobalScope,
    private val blockingRecordingDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Handler(looper) {
    companion object {
        const val MESSAGE_ID_START_RECORDING = 0
        const val MESSAGE_ID_STOP_RECORDING = 1
        const val SECONDS_PER_FRAME = 1
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
        val accumulatorInputChannel = AudioAccumulator.getInputChannel()
        val accumulatorOutputChannel = AudioAccumulator.getOutputChannel()
        val accumulator =
            AudioAccumulator(accumulatorInputChannel, accumulatorOutputChannel, recordingScope)

        val saveAudioJob = saveAudioFiles(accumulatorOutputChannel)

        val recordingStart = Calendar.getInstance().timeInMillis
        audioRecord.startRecording()

        accumulator.accumulate(recordingStart)
        recordAudio(accumulatorInputChannel)

        saveAudioJob.join()
    }

    private suspend fun recordAudio(channel: Channel<FloatArray>) {
        while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            // create buffer array for SECONDS_PER_FRAME seconds of PCM float values
            val audioFrameBuffer = FloatArray(AudioRecordFactory.SAMPLE_RATE * SECONDS_PER_FRAME)

            withContext(blockingRecordingDispatcher) {
                // blocking read SECONDS_PER_FRAME seconds of PCM float values into the audioFrameBuffer
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
    }

    private fun saveAudioFiles(channel: Channel<List<Recording>>) = recordingScope.launch {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH)

        for (element in channel) {
            ensureActive()
            val recordingArray = element.map { recording -> recording.frame }
                .reduce { acc, rec -> acc.plus(rec) }

            calendar.timeInMillis = element.first().start
            codec.savePcmToFile(
                recordingArray,
                dateFormat.format(calendar.time)
            )
        }
    }
}
