package com.peternaggschga.sleeptalk.domain.monitoring

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class AudioAccumulator(private val inputChannel: Channel<FloatArray>) {
    companion object {
        private const val INPUT_CHANNEL_BUFFER_SIZE = 60

        fun getInputChannel() = Channel<FloatArray>(INPUT_CHANNEL_BUFFER_SIZE)
    }

    suspend fun accumulate(scope: CoroutineScope) = scope.launch {
        for (element in inputChannel) {
            ensureActive()
            processElement(element)
        }
    }

    private fun processElement(element: FloatArray) {
        Log.d("AudioAccumulator", "element $element received!")
    }
}
