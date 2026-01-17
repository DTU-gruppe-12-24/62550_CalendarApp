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

        // Setup Window and Duration
        // Cap duration at 24 hours (1440 mins) to prevent multi-day logic bugs
        val minDuration = query.minDurationMinutes?.coerceAtMost(1440) ?: 1
        val window = query.timeWindow ?: (0..1440)

        // Filter Participants
        // If query.requiredCalendars is empty, we assume all calendars in the group are required
        val relevantCalendars = if (query.requiredCalendars.isNotEmpty()) {
            query.requiredCalendars
        } else {
            calendars.toSet()
        }

        // Collect blocks for this date
        val blockingEvents = relevantCalendars
            .flatMap { it.dates }
            .mapNotNull { event -> getMinuteRangeForDate(event, date) }
            .filter { it.first < window.endInclusive && it.last > window.start }

        // Calculate free gaps within the user's specific Time Window
        val freeIntervals = calculateFreeGaps(window, blockingEvents)

        // Check if any gap is large enough
        return freeIntervals.any { (it.last - it.first) >= minDuration }
    }

    private fun getMinuteRangeForDate(event: Event, date: LocalDate): IntRange? {
        val eventStart = event.start.toLocalDate()
        val eventEnd = event.end.toLocalDate()

        if (eventStart > date || eventEnd < date) return null

        // Start minute: 0 if started before today, else actual time
        val startMin = if (eventStart < date) 0
        else event.start.hour * 60 + event.start.minute

        // End minute: 1440 if ends after today, else actual time
        val endMin = if (eventEnd > date) 1440
        else event.end.hour * 60 + event.end.minute

        return if (startMin < endMin) startMin..endMin else null
    }

    private fun calculateFreeGaps(
        window: ClosedRange<Int>,
        occupied: List<IntRange>
    ): List<IntRange> {
        val gaps = mutableListOf<IntRange>()
        val sortedOccupied = occupied.sortedBy { it.first }
        var currentPointer = window.start

        for (block in sortedOccupied) {
            if (block.last <= currentPointer) continue
            if (block.first > currentPointer) {
                gaps.add(currentPointer..block.first)
            }
            currentPointer = maxOf(currentPointer, block.last)
            if (currentPointer >= window.endInclusive) break
        }

        if (currentPointer < window.endInclusive) {
            gaps.add(currentPointer..window.endInclusive)
        }
        return gaps
    }
}