package com.peternaggschga.sleeptalk.ui.monitoring

import android.app.Application
import android.icu.util.Calendar
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.peternaggschga.sleeptalk.domain.monitoring.SignalDetection
import java.time.Duration
import java.util.Date

class MonitoringViewModel(
    application: Application,
    state: SavedStateHandle,
    updateLooper: Looper = Looper.getMainLooper()
) : AndroidViewModel(application) {
    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras) =
                MonitoringViewModel(
                    checkNotNull(extras[APPLICATION_KEY]),
                    extras.createSavedStateHandle()
                ) as T
        }

        private const val STATE_ENDING_TIME_KEY = "endingTime"

        private fun isInFuture(time: Date) = time.toInstant().isBefore(
            Calendar.getInstance().time.toInstant()
                .plusSeconds(SignalDetection.LAG_SECONDS.toLong())
        )
    }

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

    // TODO: check if this works as expected
    private val _endingTime = state.getLiveData<Date>(STATE_ENDING_TIME_KEY)
    val endingTime: LiveData<Date> = _endingTime

    private val _timeTillEnd = MutableLiveData<Pair<Int, Int>>()
    val timeTillEnd: LiveData<Pair<Int, Int>> = _timeTillEnd

    init {
        if (endingTime.value?.let { isInFuture(it) } == true) {
            updateHandler.post(updateRunnable)
        }
    }

    fun setEndingTime(time: Date) {
        if (isInFuture(time)) {
            Toast.makeText(
                getApplication(),
                "The timeframe you chose is too short!", // TODO: use string resource
                Toast.LENGTH_LONG
            ).show()
            return
        }
        _endingTime.value = time

        updateHandler.removeCallbacks(updateRunnable)
        updateHandler.post(updateRunnable)
    }
}
