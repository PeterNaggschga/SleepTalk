package com.peternaggschga.sleeptalk.domain.monitoring

import kotlin.math.sqrt

/**
 * Implementation of Welford's online algorithm.
 */
class Distribution {
    var numberOfValues = 0L
    var mean = 0.0
    val variance get() = if (numberOfValues < 2) 0.0 else m2 / (numberOfValues - 1)
    val stddev get() = sqrt(variance)

    private var m2 = 0.0

    fun addValue(value: Double) {
        numberOfValues++
        val delta = value - mean
        mean += delta / numberOfValues
        m2 += delta * (value - mean)
    }
}
