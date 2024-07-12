package com.peternaggschga.sleeptalk.ui.monitoring

import android.icu.util.Calendar
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.Duration
import java.util.Date

class MonitoringViewModel(
    updateLooper: Looper = Looper.getMainLooper()
) : ViewModel() {
    private val updateHandler = Handler(updateLooper)

    private val updateRunnable = object : Runnable {
        override fun run() {
            val currentTime = Calendar.getInstance().time.toInstant()
            if ((endingTime.value?.toInstant() ?: currentTime) <= currentTime) {
                _timeTillEnd.postValue(Pair(0, 0))
                return
            }

            val duration = Duration.between(
                currentTime,
                endingTime.value?.toInstant() ?: currentTime
            ).plusMinutes(1)

            val newVal = Pair(duration.toHours().toInt() % 24, duration.toMinutes().toInt() % 60)

            if (newVal != timeTillEnd.value) {
                _timeTillEnd.postValue(newVal)
            }

            updateHandler.postDelayed(this, 1000)
        }
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is monitoring Fragment"
    }
    val text: LiveData<String> = _text

    private val _endingTime = MutableLiveData<Date>()
    val endingTime: LiveData<Date> = _endingTime

    private val _timeTillEnd = MutableLiveData<Pair<Int, Int>>()
    val timeTillEnd: LiveData<Pair<Int, Int>> = _timeTillEnd

    fun setEndingTime(time: Date) {
        if (time <= (endingTime.value ?: Calendar.getInstance().time)) {
            return
        }
        _endingTime.value = time

        updateHandler.removeCallbacks(updateRunnable)
        updateHandler.post(updateRunnable)
    }
}
