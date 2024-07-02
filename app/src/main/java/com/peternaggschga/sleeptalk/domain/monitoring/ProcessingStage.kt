package com.peternaggschga.sleeptalk.domain.monitoring

fun interface ProcessingStage {
    fun process(recordings: List<Recording>)
}
