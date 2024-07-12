package com.peternaggschga.sleeptalk.ui.monitoring

import android.util.Log
import android.widget.NumberPicker
import androidx.lifecycle.Observer

class RemainingTimeViewAdapter(
    private val hourNumberPicker: NumberPicker,
    private val minuteNumberPicker: NumberPicker
) : Observer<Pair<Int, Int>> {
    init {
        hourNumberPicker.setFormatter(TIME_FORMATTER)
        hourNumberPicker.maxValue = 23

        minuteNumberPicker.setFormatter(TIME_FORMATTER)
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

    override fun onChanged(value: Pair<Int, Int>) {
        changeValue(hourNumberPicker, value.first)
        changeValue(minuteNumberPicker, value.second)
    }
}
