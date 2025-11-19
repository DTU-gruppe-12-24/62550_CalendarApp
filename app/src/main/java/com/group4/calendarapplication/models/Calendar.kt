package com.group4.calendarapplication.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.random.Random

@Serializable
class Calendar(val name: String, val color: Color, var dates: ArrayList<LocalDate>) : java.io.Serializable {
    // Allow to temporarily randomize calendar dates, until we implement loading from iso
    fun randomize(count: Int) {
        (0..<count).forEach { _ ->
            val month = Random.nextInt(0, 5).toLong()
            val date = Random.nextInt(0, 30).toLong()

            val randomDate = LocalDate.now().plusMonths(month).plusDays(date)
            dates.add(randomDate)
        }
    }
}