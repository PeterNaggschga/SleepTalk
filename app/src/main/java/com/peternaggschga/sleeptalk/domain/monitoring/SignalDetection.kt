package com.peternaggschga.sleeptalk.domain.monitoring

import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import kotlin.math.abs

/**
 * https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data
 */
class SignalDetection {
    companion object {
        const val LAG_SECONDS = 60 * 10
        const val LAG_FRAMES = LAG_SECONDS / MonitoringServiceHandler.SECONDS_PER_FRAME
        const val THRESHOLD = 3.0
        const val INFLUENCE = 0.5
    }

    private val stats = SummaryStatistics()
    var currentSignal = false
        private set
    private val lagValues: ArrayDeque<Double> = ArrayDeque(LAG_FRAMES + 1)

    fun addValue(value: Double) {
        if (lagValues.size < LAG_FRAMES) {
            addLagValue(value)
            return
        }

        currentSignal = abs(value - stats.mean) > THRESHOLD * stats.standardDeviation
        addLagValue(
            if (currentSignal)
                INFLUENCE * value + (1 - INFLUENCE) * lagValues.last()
            else
                value
        )
    }

    private fun addLagValue(value: Double) {
        lagValues.addLast(value)
        if (lagValues.size > LAG_FRAMES) {
            lagValues.removeFirst()
            stats.clear()
            lagValues.forEach { lagValue -> stats.addValue(lagValue) }
        } else {
            stats.addValue(value)
        }
    }
}
