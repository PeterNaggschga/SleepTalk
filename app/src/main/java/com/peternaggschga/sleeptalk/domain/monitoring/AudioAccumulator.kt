package com.peternaggschga.sleeptalk.domain.monitoring

import android.util.Log
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

        private const val PURGE_THRESHOLD = 60 * 60
    }

    private lateinit var distribution: Distribution
    private lateinit var frameList: MutableList<Recording>

    suspend fun accumulate() = calculationScope.launch {
        distribution = Distribution()
        frameList = mutableListOf()
        for (element in inputChannel) {
            ensureActive()
            processElement(element)
        }
        purgeFrameList()
    }

    private fun processElement(element: FloatArray) {
        val recording = Recording(element, distribution.numberOfValues + 1)
        distribution.addValue(recording.maxValue.toDouble())
        frameList.add(recording)

        if (distribution.numberOfValues > PURGE_THRESHOLD) {
            purgeFrameList()
        }

        Log.d(
            "AudioAccumulator",
            "Distribution: new = ${recording.maxValue}, max = ${distribution.max}, n = ${distribution.numberOfValues}, mean = ${distribution.mean}, stddev = ${distribution.stddev}"
        )
    }

    private fun purgeFrameList() {
        TODO()
    }
}
