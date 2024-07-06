package com.peternaggschga.sleeptalk.ui.monitoring

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.icu.util.Calendar
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.peternaggschga.sleeptalk.domain.monitoring.SignalDetection

class TimePickerDialogFragment : DialogFragment(), OnTimeSetListener {
    companion object {
        const val TAG = "TimePicker"
        private const val ADDED_MINUTES = 8 * 60
    }

    private val monitoringViewModel: MonitoringViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, ADDED_MINUTES)

        return TimePickerDialog(
            activity,
            this,
            calendar[Calendar.HOUR],
            calendar[Calendar.MINUTE],
            DateFormat.is24HourFormat(activity)
        )
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val endCalendar = Calendar.getInstance()
        endCalendar[Calendar.HOUR] = hourOfDay
        endCalendar[Calendar.MINUTE] = minute

        val startCalendar = Calendar.getInstance()
        startCalendar.add(Calendar.SECOND, SignalDetection.LAG_SECONDS)

        if (endCalendar <= Calendar.getInstance()) {
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        monitoringViewModel.setEndingTime(
            SystemClock.elapsedRealtime() +
                    (endCalendar.timeInMillis - Calendar.getInstance().timeInMillis)
        )
    }
}
