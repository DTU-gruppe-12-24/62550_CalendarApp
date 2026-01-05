package com.group4.calendarapplication.models

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.io.IOException
import java.io.InputStream
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.time.temporal.TemporalField
import java.time.temporal.TemporalQueries
import java.time.temporal.WeekFields
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.random.Random


@Serializable
class Event(val start: LocalDate, val end: LocalDate) : java.io.Serializable {
    fun isDateTimeWithInEvent(date: LocalDate) : Boolean {
        return (start.isBefore(date) || start == date) && (end.isAfter(date) || end == date)
    }
}

@Serializable
class Calendar(val name: String, val color: Color, var dates: ArrayList<Event>) : java.io.Serializable {}

fun importZippedIcal(input: InputStream) : ArrayList<Calendar> {
    val calendars = ArrayList<Calendar>()
    // unzip file
    val zip = ZipInputStream(input)

    generateSequence { zip.nextEntry }.filterNot { it.isDirectory }
        .forEach { entry ->
            Log.i("CalendarImport", entry.name)
            // Add this entry as own ical calendar
            calendars.add(importIcal(zip))
        }
    // Return full list of calendars
    return calendars
}

fun importIcal(input: InputStream) : Calendar {
    val inputAsString = String(input.readAllBytes())

    // Get lines and combine lines if split across multiple lines
    val lines: ArrayList<String> = ArrayList<String>(inputAsString.split("\r\n").toList())
    lines.indices
        .filter{index -> lines[index].startsWith(' ') || lines[index].startsWith('\t')}
        .forEach{index -> {
            lines[index - 1] = lines[index - 1] + lines[index]
        }.run { }
    }

    var name = "temp"
    val dates = ArrayList<Event>()
    // Go through each line and parse ical
    val activeSections: ArrayList<String> = ArrayList()
    var startDate : LocalDate? = null
    var endDate : LocalDate? = null
    var lineIndex = 0
    for (line in lines) {
        lineIndex++

        // Begin section?
        if (line.startsWith("BEGIN:")) {
            activeSections.add(line.replace("BEGIN:", ""))
            continue
        }

        if (activeSections.isEmpty()) continue

        // Only care about VCALENDAR and VEVENT sections
        val activeSection = activeSections.last()
        when (activeSection) {
            "VCALENDAR" -> {
                // VERSION check
                if (line.startsWith("VERSION:") && line != "VERSION:2.0") throw UnsupportedOperationException("Invalid ical: Parser only supports version 2.0. At line $lineIndex.")
                // CALSCALE check
                else if (line.startsWith("CALSCALE:") && line != "CALSCALE:GREGORIAN") throw UnsupportedOperationException("Invalid ical: Calendar scale is no gregorian. At line $lineIndex.")
                // Get name
                else if (line.startsWith("X-WR-CALNAME:")) name = line.replace("X-WR-CALNAME:", "")
            }
            "VEVENT" -> {
                val attribute = line.split(":").first()
                val attributeMain = attribute.split(";").first()
                val value = line.split(":").last()

                when (attributeMain) {
                    "DTSTART" -> {
                        if (attribute.contains(";")) {
                            val attributeSecondary = attribute.split(";").last()
                            if (attributeSecondary == "VALUE=DATE") startDate = parseDate(value, "yyyyMMdd")
                            else if (attributeSecondary.startsWith("TZID")) {
                                startDate = parseDate(value, "yyyyMMdd'T'HHmmss")
                            }
                        } else {
                            startDate = parseDate(value, "yyyyMMdd'T'HHmmssX")
                        }
                    }
                    "DTEND" -> {
                        if (attribute.contains(";")) {
                            val attributeSecondary = attribute.split(";").last()
                            if (attributeSecondary == "VALUE=DATE") endDate = parseDate(value, "yyyyMMdd")
                            else if (attributeSecondary.startsWith("TZID")) {
                                endDate = parseDate(value, "yyyyMMdd'T'HHmmss")
                            }
                        } else {
                            endDate = parseDate(value, "yyyyMMdd'T'HHmmssX")
                        }
                    }
                    // TODO: Handle recurring events (events scheduled weekly, monthly etc. only show up once in file)
                    "END" -> {
                        if (startDate == null || endDate == null) {
                            Log.w("CalendarImport", "Calendar events must have both a start and an end time. At line $lineIndex.")
                            //throw UnsupportedOperationException("Invalid ical: Calendar events must have both a start and an end time. At line $lineIndex.")
                            continue
                        }
                        // Add event to calendar
                        dates.add(Event(startDate, endDate))
                        // Reset start and end dates
                        startDate = null
                        endDate = null
                    }
                }
            }
        }

        // End section?
        if (line.startsWith("END:")) {
            val section = line.replace("END:", "")
            if (section != activeSections.last()) throw UnsupportedOperationException("Invalid ical: Can't end section \"$section\" before it's started. At line $lineIndex.")
            activeSections.removeAt(activeSections.size - 1)
        }
    }

    val color = Color(Random.nextInt(100,200),Random.nextInt(100,200),Random.nextInt(100,200),255)

    return Calendar(name, color, dates)
}

fun parseDate(date: String, format: String) : LocalDate {
    val formatter = DateTimeFormatter.ofPattern(format)
    val temporal = formatter.parse(date)

    val year = Year.from(temporal)
    val monthDay = MonthDay.from(temporal)

    return LocalDate.of(year.value, monthDay.month, monthDay.dayOfMonth) ?: throw DateTimeException("Failed to parse \"$date\" as DateTime")
}