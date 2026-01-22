package com.group4.calendarapplication.domain.filter

import com.group4.calendarapplication.models.Calendar
import java.time.DayOfWeek

data class FilterQuery(
    val requiredCalendars: Set<Calendar> = emptySet(),
    val optionalCalendars: Set<Calendar> = emptySet(),
    val weekdays: Set<DayOfWeek> = emptySet(),
    val timeWindow: ClosedRange<Int>? = null, // minutes since midnight
    val minDurationMinutes: Int? = null
) {
    // Returns true if any filter criteria have been set
    val isActive: Boolean
        get() = requiredCalendars.isNotEmpty() ||
                optionalCalendars.isNotEmpty() ||
                weekdays.isNotEmpty() ||
                timeWindow != null ||
                minDurationMinutes != null
}