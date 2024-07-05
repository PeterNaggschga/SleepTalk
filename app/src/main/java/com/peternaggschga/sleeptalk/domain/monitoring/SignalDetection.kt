package com.peternaggschga.sleeptalk.domain.monitoring

import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import kotlin.math.abs

/**
 * https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data
 */
class SignalDetection(
    private val lag: Int = 60 * 10 / MonitoringServiceHandler.SECONDS_PER_FRAME,
    private val threshold: Double = 3.0,
    private val influence: Double = 0.5
) {
    private val stats = SummaryStatistics()
    var currentSignal = false
        private set
    private val lagValues: ArrayDeque<Double> = ArrayDeque(lag + 1)

    fun addValue(value: Double) {
        if (lagValues.size < lag) {
            addLagValue(value)
            return
        }

        currentSignal = abs(value - stats.mean) > threshold * stats.standardDeviation
        addLagValue(
            if (currentSignal)
                influence * value + (1 - influence) * lagValues.last()
            else
                value
        )
    }

    private fun addLagValue(value: Double) {
        lagValues.addLast(value)
        if (lagValues.size > lag) {
            lagValues.removeFirst()
            stats.clear()
            lagValues.forEach { lagValue -> stats.addValue(lagValue) }
        } else {
            stats.addValue(value)
        }
    }
}
