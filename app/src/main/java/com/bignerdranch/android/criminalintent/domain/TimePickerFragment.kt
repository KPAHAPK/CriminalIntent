package com.bignerdranch.android.criminalintent.domain

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_TIME = "argTime"
private const val ARG_REQUEST_KEY = "argRequestKey"
private const val RESULT_TIME_KEY = "resultTimeKey"

class TimePickerFragment : DialogFragment() {

    companion object {
        fun newInstance(date: Date, requestKey: String): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, date)
                putString(ARG_REQUEST_KEY, requestKey)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }

        fun getSelectedTime(result: Bundle) = result.getSerializable(RESULT_TIME_KEY) as Date
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_TIME) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialYear = calendar[Calendar.YEAR]
        val initialMonth = calendar[Calendar.MONTH]
        val initialDay = calendar[Calendar.DAY_OF_MONTH]
        val initialHour = calendar[Calendar.HOUR_OF_DAY]
        val initialMinute = calendar[Calendar.MINUTE]
        val timeListener =
            TimePickerDialog.OnTimeSetListener { _: TimePicker, hourOfDay: Int, minute: Int ->
                val resultTimePicker =
                    GregorianCalendar(initialYear, initialMonth, initialDay, hourOfDay, minute).time
                val result = Bundle().apply {
                    putSerializable(RESULT_TIME_KEY, resultTimePicker)
                }
                val resultKey = arguments?.getString(ARG_REQUEST_KEY, "") as String
                parentFragmentManager.setFragmentResult(resultKey, result)
            }
        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinute,
            true)
    }
}