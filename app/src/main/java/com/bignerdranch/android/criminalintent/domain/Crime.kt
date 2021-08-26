package com.bignerdranch.android.criminalintent.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
class Crime(@PrimaryKey
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
)