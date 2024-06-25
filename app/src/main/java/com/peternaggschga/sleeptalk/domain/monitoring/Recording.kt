package com.peternaggschga.sleeptalk.domain.monitoring

import kotlin.math.abs
import kotlin.math.max

data class Recording(
    val frame: FloatArray,
    val timestamp: Long // TODO: get actual Timestamp when this second was recorded
) {
    val maxValue = max(
        abs(frame.max()),
        abs(frame.min())
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Recording) return false

        if (timestamp != other.timestamp) return false
        if (maxValue != other.maxValue) return false
        if (!frame.contentEquals(other.frame)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frame.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + maxValue.hashCode()
        return result
    }
}
