package com.peternaggschga.sleeptalk.domain.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AudioAccumulator(
    private val inputChannel: Channel<FloatArray>,
    private val outputChannel: Channel<List<Recording>>,
    @OptIn(DelicateCoroutinesApi::class) private val calculationScope: CoroutineScope = GlobalScope
) {
    companion object {
        private const val FRAME_CONNECTION_THRESHOLD =
            20 / MonitoringServiceHandler.SECONDS_PER_FRAME
        private const val INPUT_CHANNEL_BUFFER_SIZE = 60
        private const val OUTPUT_CHANNEL_BUFFER_SIZE = 5

        fun getInputChannel() = Channel<FloatArray>(INPUT_CHANNEL_BUFFER_SIZE)
        fun getOutputChannel() = Channel<List<Recording>>(OUTPUT_CHANNEL_BUFFER_SIZE)
    }

    private lateinit var signalDetection: SignalDetection

    suspend fun accumulate(recordingStartTime: Long) = calculationScope.launch {
        signalDetection = SignalDetection()
        val signalRecording = mutableListOf<Recording>()
        val noSignalRecording = mutableListOf<Recording>()
        var receivedValues = 0L

        for (element in inputChannel) {
            ensureActive()

            val recording = Recording(
                element,
                recordingStartTime + receivedValues++ * 1000 / MonitoringServiceHandler.SECONDS_PER_FRAME
            )
            signalDetection.addValue(recording.maxValue.toDouble())

            if (signalDetection.currentSignal) {
                // current recording is interesting
                if (noSignalRecording.isNotEmpty()) {
                    // last interesting recording is less than FRAME_CONNECTION_THRESHOLD seconds old
                    // last interesting recording and this one are merged
                    signalRecording.addAll(noSignalRecording)
                    noSignalRecording.clear()
                }
                signalRecording.add(recording)
            } else if (signalRecording.isNotEmpty()) {
                // signal stopped but last interesting record was not saved yet
                if (noSignalRecording.size < FRAME_CONNECTION_THRESHOLD) {
                    // last interesting recording was recent
                    // audio in between is saved
                    noSignalRecording.add(recording)
                } else {
                    // last interesting recording was long ago
                    // last interesting recording is saved
                    outputChannel.send(signalRecording.toList())
                    signalRecording.clear()
                    // audio since last interesting recording is deleted
                    noSignalRecording.clear()
                }
            }
        }

        if (signalRecording.isNotEmpty()) {
            outputChannel.send(signalRecording)
        }
        outputChannel.close()
    }
}
