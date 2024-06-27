package com.peternaggschga.sleeptalk.domain.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AudioAccumulator(
    private val inputChannel: Channel<FloatArray>,
    @OptIn(DelicateCoroutinesApi::class) private val calculationScope: CoroutineScope = GlobalScope
) {
    companion object {
        private const val INPUT_CHANNEL_BUFFER_SIZE = 60

        fun getInputChannel() = Channel<FloatArray>(INPUT_CHANNEL_BUFFER_SIZE)
    }

    private lateinit var signalDetection: SignalDetection

    suspend fun accumulate(recordingStartTime: Long) = calculationScope.launch {
        signalDetection = SignalDetection()
        val currentRecordingsList = mutableListOf<Recording>()
        var receivedValues = 0L

        for (element in inputChannel) {
            ensureActive()

            val recording = Recording(
                element,
                recordingStartTime + receivedValues++ * 1000 / MonitoringServiceHandler.SECONDS_PER_FRAME
            )
            signalDetection.addValue(recording.maxValue.toDouble())

            if (signalDetection.currentSignal) {
                currentRecordingsList.add(recording)
            } else if (currentRecordingsList.isNotEmpty()) {
                saveRecordings(currentRecordingsList)
                currentRecordingsList.clear()
            }
        }

        if (currentRecordingsList.isNotEmpty()) {
            saveRecordings(currentRecordingsList)
        }
    }

    private fun saveRecordings(recordingList: List<Recording>) {
        TODO()
    }
}
