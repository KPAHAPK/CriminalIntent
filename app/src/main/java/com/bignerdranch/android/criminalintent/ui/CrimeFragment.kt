package com.bignerdranch.android.criminalintent.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bignerdranch.android.criminalintent.R
import com.bignerdranch.android.criminalintent.domain.Crime
import com.bignerdranch.android.criminalintent.viewmodel.CrimeDetailViewModel
import java.util.*

private lateinit var titleField: EditText
private lateinit var dateButton: Button
private lateinit var solvedCheckBox: CheckBox

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"


class CrimeFragment : Fragment() {

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    companion object{
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
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

        dateButton.apply {
            text = crime.date.toString()
            isEnabled = false
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner,
            {
                crime ->
                crime?.let{
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
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }


    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
    }


}