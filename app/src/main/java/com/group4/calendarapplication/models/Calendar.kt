package com.group4.calendarapplication.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.io.InputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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

fun importZippedIcal(input: InputStream) : ArrayList<Calendar> {
    val calendars = ArrayList<Calendar>()
    // unzip file
    val zip = ZipInputStream(input)
    var entry: ZipEntry? = zip.nextEntry
    while (entry != null) {
        // Add this entry as own ical calendar
        calendars.add(importIcal(zip))
        // Goto next entry in zip file
        entry = zip.nextEntry
    }
    // Return full list of calendars
    return calendars
}

fun importIcal(input: InputStream) : Calendar {
    val inputAsString = input.bufferedReader().use { it.readText() }
    // TODO: Actually parse and import ical file
    val name = "temp"
    val color = Color.Red
    val dates = ArrayList<LocalDate>()

    return Calendar(name, color, dates)
}