package com.bignerdranch.android.criminalintent.viewmodel

import androidx.lifecycle.ViewModel
import com.bignerdranch.android.criminalintent.domain.CrimeRepository

class CrimeListViewModel : ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()
}
