package com.peternaggschga.sleeptalk.domain.monitoring

import android.util.Log
import kotlinx.coroutines.channels.Channel

class AudioAccumulator(private val inputChannel: Channel<FloatArray>) {
    companion object {
        private const val INPUT_CHANNEL_BUFFER_SIZE = 60

        fun getInputChannel() = Channel<FloatArray>(INPUT_CHANNEL_BUFFER_SIZE)
    }

    suspend fun accumulate() {
        for (element in inputChannel) {
            processElement(element)
        }
    }

    private fun processElement(element: FloatArray) {
        Log.d("AudioAccumulator", "element $element received!")
    }
}
