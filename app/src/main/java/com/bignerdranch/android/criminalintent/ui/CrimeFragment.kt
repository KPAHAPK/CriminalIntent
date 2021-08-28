package com.bignerdranch.android.criminalintent.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.bignerdranch.android.criminalintent.R
import com.bignerdranch.android.criminalintent.domain.Crime
import com.bignerdranch.android.criminalintent.domain.DatePickerFragment
import com.bignerdranch.android.criminalintent.domain.TimePickerFragment
import com.bignerdranch.android.criminalintent.viewmodel.CrimeDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

private lateinit var titleField: EditText
private lateinit var dateButton: Button
private lateinit var solvedCheckBox: CheckBox
private lateinit var timeButton: Button

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_DATE = "RequestDate"
private const val REQUEST_TIME = "RequestTime"


class CrimeFragment : Fragment(), FragmentResultListener {

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                this.putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                this.arguments = args
            }
        }
    }


    private lateinit var crime: Crime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        timeButton = view.findViewById(R.id.crime_time) as Button
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner,
            { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
            }
        }
        titleField.addTextChangedListener((titleWatcher))

        solvedCheckBox.apply {
            this.setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date, REQUEST_DATE).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_DATE)
            }
        }
        childFragmentManager.setFragmentResultListener(REQUEST_DATE, viewLifecycleOwner, this)

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date, REQUEST_TIME).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_TIME)
            }
        }
        childFragmentManager.setFragmentResultListener(REQUEST_TIME, viewLifecycleOwner, this)
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }


    private fun updateUI() {
        val datePattern = "EEEE, MM dd yyyy"
        val timePattern = "HH:mm"
        timeButton.apply {
            text = setCalendarButtonText(timePattern)
            jumpDrawablesToCurrentState()
        }
        dateButton.apply {
            text = setCalendarButtonText(datePattern)
            jumpDrawablesToCurrentState()
        }
        titleField.setText(crime.title)
        solvedCheckBox.apply {
            this.isChecked = crime.isSolved
            this.jumpDrawablesToCurrentState()
        }

    }

    fun setCalendarButtonText(pattern: String): String {
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return simpleDateFormat.format(crime.date)

    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_DATE -> crime.date = DatePickerFragment.getSelectedDate(result)
            REQUEST_TIME -> crime.date = TimePickerFragment.getSelectedTime(result)
        }
        updateUI()
    }


}