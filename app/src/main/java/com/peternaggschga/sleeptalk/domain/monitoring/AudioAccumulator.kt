package com.peternaggschga.sleeptalk.domain.monitoring

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

class AudioAccumulator(
    private val inputChannel: Channel<FloatArray>,
    @OptIn(DelicateCoroutinesApi::class) private val calculationScope: CoroutineScope = GlobalScope
) {
    companion object {
        private const val INPUT_CHANNEL_BUFFER_SIZE = 60

        fun getInputChannel() = Channel<FloatArray>(INPUT_CHANNEL_BUFFER_SIZE)
    }

    private lateinit var distribution: Distribution

    suspend fun accumulate() = calculationScope.launch {
        distribution = Distribution()
        for (element in inputChannel) {
            ensureActive()
            processElement(element)
        }
    }

    private fun processElement(element: FloatArray) {
        val maxValue = max(
            abs(element.max()),
            abs(element.min())
        )
        distribution.addValue(maxValue.toDouble())

        Log.d(
            "AudioAccumulator",
            "Distribution: maxValue = $maxValue, n = ${distribution.numberOfValues}, mean = ${distribution.mean}, stddev = ${distribution.stddev}"
        )
    }
}
