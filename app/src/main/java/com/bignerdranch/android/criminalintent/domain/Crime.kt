package com.bignerdranch.android.criminalintent.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZoneId
import java.util.*

@Entity
data class Crime(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var suspect: String = "",
    var suspectPhoneNumber: String = ""
){
    val photoFileName
    get() = "IMG_$id.jpg"
}