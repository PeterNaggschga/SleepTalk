package com.peternaggschga.sleeptalk.ui.monitoring

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MonitoringViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is monitoring Fragment"
    }
    val text: LiveData<String> = _text

    private val _endingTime = MutableLiveData<Long>()
    val endingTime: LiveData<Long> = _endingTime

    fun setEndingTime(time: Long) {
        _endingTime.value = time
    }
}
