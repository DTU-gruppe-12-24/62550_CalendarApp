package com.group4.calendarapplication.models

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import kotlin.random.Random
import java.time.Duration
import java.time.LocalDate


@Serializable
class Event(val start: LocalDateTime, val end: LocalDateTime) : java.io.Serializable {
    fun isDateTimeWithInEvent(date: LocalDateTime) : Boolean {
        return (start.isBefore(date) || start == date) && (end.isAfter(date) || end == date)
    }

    fun isDateTimeWithInEvent(date: LocalDate) : Boolean {
        val endDate = end.minusMinutes(1).toLocalDate()
        val startDate = start.plusMinutes(1).toLocalDate()
        return (startDate.isBefore(date) || startDate == date) && (endDate.isAfter(date) || endDate == date)
    }
    fun getDisplayTextForDate(date: LocalDate): String {
        val startDate = start.toLocalDate()
        val endDate = end.toLocalDate()
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

        // Single-day event
        if (startDate == endDate) {
            if (start.toLocalTime() == java.time.LocalTime.MIDNIGHT &&
                end.toLocalTime() == java.time.LocalTime.MIDNIGHT
            ) {
                return "All day"
            }
            return "${start.format(timeFormat)} - ${end.format(timeFormat)}"
        }

        return when (date) {
            startDate -> "${start.format(timeFormat)} - 24:00"
            endDate -> "00:00 - ${end.format(timeFormat)}"
            else -> "All day"
        }
    }

}

@Serializable
class Calendar(var name: String, var color: Color, var dates: ArrayList<Event>) : java.io.Serializable {}

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
    val inputAsString = String(input.readBytes())

    // Get lines and combine lines if split across multiple lines
    val lines: ArrayList<String> = ArrayList(inputAsString.split("\r\n").toList())
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
    var startDate : LocalDateTime? = null
    var endDate : LocalDateTime? = null
    val repeatedDates = ArrayList<Event>()
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
                    // Start time of event
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
                    // End time of events
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
                    // Repeats of event
                    "RRULE" -> {
                        if (startDate == null || endDate == null) {
                            Log.w("CalendarImport", "Invalid ical: Calendar events must have both a start and an end time before it can be repeated. At line $lineIndex.")
                            continue
                        }

                        val eventDuration = Duration.between(startDate, endDate)

                        val values = value.split(";").associateBy({ it.split("=").first() }, { it.split("=").last() } )

                        val interval = values["INTERVAL"]?.toInt() ?: 1 //throw UnsupportedOperationException("Invalid ical: Can't repeat without a valid INTERVAL key. At line $lineIndex.");
                        val offset = Duration.between(
                            startDate,
                            when (values["FREQ"]) {
                                "YEARLY" -> startDate.plusYears(1)
                                "MONTHLY" -> startDate.plusMonths(1)
                                "WEEKLY" -> startDate.plusWeeks(1)
                                "DAILY" -> startDate.plusDays(1)
                                "HOURLY" -> startDate
                                "MINUTELY" -> startDate
                                "SECONDLY" -> startDate
                                else -> throw UnsupportedOperationException("Invalid ical: Can't repeat without a valid FREQ key. At line $lineIndex.")
                            }
                        ).multipliedBy(interval.toLong())
                        val count = values["COUNT"]?.toInt() ?: -1
                        val until = if (values.contains("UNTIL")) {
                            parseDate(
                                values["UNTIL"] ?: throw UnsupportedOperationException("Invalid ical: Can't repeat without a valid UNTIL key. At line $lineIndex."),
                                "yyyyMMdd'T'HHmmssX"
                            )
                        } else if (count > 0) {
                            startDate + offset.multipliedBy(count.toLong())
                        } else {
                            // No end date, add dates within the next 2 years
                            startDate.plusYears(2)
                        }

                        var date: LocalDateTime = startDate
                        if (values["FREQ"] == "WEEKLY" && values.contains("BYDAY") && !values.contains("INTERVAL")) {
                            while (date <= until || (count > 0 && repeatedDates.size < count)) {
                                // Check if date is filtered out
                                var valid = true
                                if (!(values["BYYEAR"]?.contains(date.year.toString()) ?: true)) valid = false
                                if (!(values["BYMONTH"]?.contains(date.month.toString()) ?: true)) valid = false
                                if (!(values["BYDAY"]?.contains(date.getDayOfWeek2Letters()) ?: true)) valid = false
                                // Add new event
                                if (valid) repeatedDates.add(Event(date, date + eventDuration))
                                // Next date
                                date = date.plusDays(1)
                            }
                        } else {
                            while (date <= until && (count <= 0 || repeatedDates.size >= count)) {
                                // Check if date is filtered out
                                var valid = true
                                if (!(values["BYYEAR"]?.contains(date.year.toString()) ?: true)) valid = false
                                if (!(values["BYMONTH"]?.contains(date.month.toString()) ?: true)) valid = false
                                if (!(values["BYDAY"]?.contains(date.getDayOfWeek2Letters()) ?: true)) valid = false
                                // Add new event
                                if (valid) repeatedDates.add(Event(date, date + eventDuration))
                                // Next date
                                date = date.plus(offset)
                            }
                        }
                    }
                    // Exclude date from repeat
                    "EXDATE" -> {
                        if (repeatedDates.isEmpty()) {
                            Log.w("CalendarImport", "Invalid ical: EXCAL items must come after an RRULE. At line $lineIndex.")
                            continue
                        }
                        // Parse date
                        var date = LocalDateTime.now()
                        if (attribute.contains(";")) {
                            val attributeSecondary = attribute.split(";").last()
                            if (attributeSecondary == "VALUE=DATE") date = parseDate(value, "yyyyMMdd")
                            else if (attributeSecondary.startsWith("TZID")) {
                                date = parseDate(value, "yyyyMMdd'T'HHmmss")
                            }
                        } else {
                            date = parseDate(value, "yyyyMMdd'T'HHmmssX")
                        }
                        // Remove from recurring list
                        repeatedDates.removeAll { d -> d.start == date }
                    }
                    // Repeat event on specific dates
                    "RDATE" -> {
                        if (startDate == null || endDate == null) {
                            Log.w("CalendarImport", "Invalid ical: Calendar events must have both a start and an end time before it can be repeated. At line $lineIndex.")
                            continue
                        }
                        val eventDuration = Duration.between(startDate, endDate)
                        // Parse date
                        value.split(",").forEach { v ->
                            var date = LocalDateTime.now()
                            if (attribute.contains(";")) {
                                val attributeSecondary = attribute.split(";").last()
                                if (attributeSecondary == "VALUE=DATE") date = parseDate(v, "yyyyMMdd")
                                else if (attributeSecondary.startsWith("TZID")) {
                                    date = parseDate(v, "yyyyMMdd'T'HHmmss")
                                }
                            } else {
                                date = parseDate(v, "yyyyMMdd'T'HHmmssX")
                            }
                            repeatedDates.add(Event(date, date + eventDuration))
                        }
                    }
                    // End event and add all found info to list of dates
                    "END" -> {
                        if (startDate == null || endDate == null) {
                            Log.w("CalendarImport", "Calendar events must have both a start and an end time. At line $lineIndex.")
                            continue
                        }
                        // Add event to calendar
                        if (repeatedDates.isEmpty()) dates.add(Event(startDate, endDate))
                        dates.addAll(repeatedDates)
                        // Reset start and end dates
                        startDate = null
                        endDate = null
                        repeatedDates.clear()
                    }
                }
            }
        }

        // End current section?
        if (line.startsWith("END:")) {
            val section = line.replace("END:", "")
            if (section != activeSections.last()) throw UnsupportedOperationException("Invalid ical: Can't end section \"$section\" before it's started. At line $lineIndex.")
            activeSections.removeAt(activeSections.size - 1)
        }
    }

    val color = Color(Random.nextInt(100,200),Random.nextInt(100,200),Random.nextInt(100,200),255)

    return Calendar(name, color, dates)
}

fun parseDate(date: String, format: String) : LocalDateTime {
    val formatter = DateTimeFormatter.ofPattern(format)

    return if (format.contains("T")) {
        LocalDateTime.parse(date, formatter)
    } else {
        LocalDate.parse(date, formatter).atTime(0, 0)
    }

    /*
    val temporal = formatter.parse(date)

    val year = Year.from(temporal)
    val monthDay = MonthDay.from(temporal)

    return LocalDateTime.of(year.value, monthDay.month, monthDay.dayOfMonth) ?: throw DateTimeException("Failed to parse \"$date\" as DateTime")
     */
}

private fun LocalDateTime.getDayOfWeek2Letters() : String = when (this.dayOfWeek) {
    DayOfWeek.MONDAY -> "MO"
    DayOfWeek.TUESDAY -> "TU"
    DayOfWeek.WEDNESDAY -> "WE"
    DayOfWeek.THURSDAY -> "TH"
    DayOfWeek.FRIDAY -> "FR"
    DayOfWeek.SATURDAY -> "SA"
    DayOfWeek.SUNDAY -> "SU"
}