package com.group4.calendarapplication.domain.filter

import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.Event
import java.time.LocalDate

class AvailabilityEngine {

    fun isDayAvailable(
        calendars: List<Calendar>,
        date: LocalDate,
        query: FilterQuery
    ): Boolean {
        // Weekday Filter
        if (query.weekdays.isNotEmpty() && date.dayOfWeek !in query.weekdays) {
            return false
        }

        val window = query.timeWindow ?: return true
        val minDuration = query.minDurationMinutes ?: 0

        // Filter calendars to only those required by the user
        // If query.requiredCalendars is empty, we assume all calendars in the group are required
        val relevantCalendars = if (query.requiredCalendars.isNotEmpty()) {
            query.requiredCalendars
        } else {
            calendars.toSet()
        }

        // Get blocking events ONLY for the relevant people
        val blockingEvents = relevantCalendars
            .flatMap { cal -> cal.dates }
            .filter { event ->
                // Ensure the event actually occurs on the requested date
                event.start.toLocalDate() == date || event.end.toLocalDate() == date
            }
            .map { it.toMinuteRange() }
            .filter { it.start < window.endInclusive && it.endInclusive > window.start }

        // Calculate gaps
        val freeIntervals = subtractIntervals(window, blockingEvents)

        // Check if any gap is large enough
        return freeIntervals.any { (it.endInclusive - it.start) >= minDuration }
    }

    private fun Event.toMinuteRange(): IntRange {
        val s = start.hour * 60 + start.minute
        val e = end.hour * 60 + end.minute
        return s..e
    }

    private fun subtractIntervals(
        window: ClosedRange<Int>,
        occupied: List<ClosedRange<Int>>
    ): List<IntRange> {
        val result = mutableListOf<IntRange>()

        val sortedOccupied = occupied
            .map { maxOf(it.start, window.start)..minOf(it.endInclusive, window.endInclusive) }
            .filter { it.start < it.endInclusive }
            .sortedBy { it.start }

        var currentStart = window.start

        for (block in sortedOccupied) {
            if (block.start > currentStart) {
                result.add(currentStart..block.start)
            }
            currentStart = maxOf(currentStart, block.endInclusive)
        }

        if (currentStart < window.endInclusive) {
            result.add(currentStart..window.endInclusive)
        }

        return result
    }
}