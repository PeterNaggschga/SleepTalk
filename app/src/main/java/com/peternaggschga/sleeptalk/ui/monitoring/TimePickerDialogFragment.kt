package com.peternaggschga.sleeptalk.ui.monitoring

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

class TimePickerDialogFragment : DialogFragment(), OnTimeSetListener {
    companion object {
        const val TAG = "TimePicker"
        private const val ADDED_MINUTES = 8 * 60
    }

    private val monitoringViewModel: MonitoringViewModel by activityViewModels { MonitoringViewModel.Factory }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, ADDED_MINUTES)

        return TimePickerDialog(
            activity,
            this,
            calendar[Calendar.HOUR_OF_DAY],
            calendar[Calendar.MINUTE],
            DateFormat.is24HourFormat(activity)
        )
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val currentTime = Calendar.getInstance()

        val endCalendar = Calendar.getInstance()
        endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        endCalendar.set(Calendar.MINUTE, minute)
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)

        if (endCalendar <= currentTime) {
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        monitoringViewModel.setEndingTime(endCalendar.time)
    }
}
