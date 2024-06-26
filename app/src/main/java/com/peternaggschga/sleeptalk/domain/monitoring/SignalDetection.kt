package com.peternaggschga.sleeptalk.domain.monitoring

import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import kotlin.math.abs

/**
 * https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data
 */
class SignalDetection(
    private val lag: Long = 60L * 10 / MonitoringServiceHandler.SECONDS_PER_FRAME,
    private val threshold: Double = 3.0,
    private val influence: Double = 0.5
) {
    private val stats = SummaryStatistics()
    var currentSignal = false
        private set
    val n get() = stats.n

    fun addValue(value: Double) {
        if (stats.n < lag) {
            stats.addValue(value)
            return
        }

        val signal = abs(value - stats.mean) > threshold * stats.standardDeviation
        stats.addValue(
            if (signal)
                value * influence
            else
                value
        )

        if (signal != currentSignal) {
            currentSignal = signal
        }
    }
}
