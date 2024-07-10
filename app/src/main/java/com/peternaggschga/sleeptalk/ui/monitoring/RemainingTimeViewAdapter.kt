package com.peternaggschga.sleeptalk.ui.monitoring

import android.icu.util.Calendar
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.NumberPicker
import androidx.lifecycle.Observer

class RemainingTimeViewAdapter(
    private val monitoringViewModel: MonitoringViewModel,
    private val hourNumberPicker: NumberPicker,
    private val minuteNumberPicker: NumberPicker,
    updateLooper: Looper = Looper.getMainLooper()
) : Observer<Long> {
    init {
        val onValueChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            run {
                handler.removeCallbacks(updateRunnable)
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = 0
                    add(Calendar.HOUR, hourNumberPicker.value)
                    add(Calendar.MINUTE, minuteNumberPicker.value)
                }
                monitoringViewModel.setEndingTime(SystemClock.elapsedRealtime() + calendar.timeInMillis)
            }
        }

        hourNumberPicker.setFormatter(TIME_FORMATTER)
        hourNumberPicker.setOnValueChangedListener(onValueChangeListener)
        hourNumberPicker.maxValue = 23

        minuteNumberPicker.setFormatter(TIME_FORMATTER)
        minuteNumberPicker.setOnValueChangedListener(onValueChangeListener)
        minuteNumberPicker.maxValue = 59
    }

    companion object {
        private val TIME_FORMATTER = NumberPicker.Formatter { value ->
            if (value < 10) {
                "0$value"
            } else {
                value.toString()
            }
        }

        private val CHANGE_VALUE_BY_ONE_METHOD = try {
            NumberPicker::class.java.getDeclaredMethod("changeValueByOne", Boolean::class.java)
                .apply {
                    isAccessible = true
                }
        } catch (_: Exception) {
            null
        }

        private fun changeValueByOne(picker: NumberPicker, increment: Boolean) {
            try {
                CHANGE_VALUE_BY_ONE_METHOD!!.invoke(picker, increment)
                return
            } catch (e: Exception) {
                Log.e(
                    NumberPicker::class.simpleName,
                    "NumberPicker increment/decrement couldn't be animated!",
                    e
                )
            }
            picker.value += if (increment) 1 else -1
        }

        private fun changeValue(picker: NumberPicker, value: Int) {
            if ((picker.value + 1) % (picker.maxValue + 1) == value) {
                changeValueByOne(picker, true)
                return
            }
            if ((picker.value + picker.maxValue) % (picker.maxValue + 1) == value) {
                changeValueByOne(picker, false)
                return
            }

            picker.value = value
        }
    }

    private val handler = Handler(updateLooper)

    private val updateRunnable = object : Runnable {
        // TODO: move update process to MonitoringViewModel (make time until end a LiveData<Calendar>)
        override fun run() {
            val calendar = Calendar.getInstance().apply {
                timeInMillis =
                    monitoringViewModel.endingTime.value?.minus(SystemClock.elapsedRealtime()) ?: 0
                add(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.HOUR, -1)
                add(Calendar.SECOND, 59)
            }
            changeValue(hourNumberPicker, calendar[Calendar.HOUR])
            changeValue(minuteNumberPicker, calendar[Calendar.MINUTE])

            handler.postDelayed(this, 500)
        }
    }

    override fun onChanged(value: Long) {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }
}
