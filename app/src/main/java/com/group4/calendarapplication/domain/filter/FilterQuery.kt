package com.group4.calendarapplication.domain.filter

import com.group4.calendarapplication.models.Calendar
import java.time.DayOfWeek

data class FilterQuery(
    val requiredCalendars: Set<Calendar> = emptySet(),
    val optionalCalendars: Set<Calendar> = emptySet(),
    val weekdays: Set<DayOfWeek> = emptySet(),
    val timeWindow: ClosedRange<Int>? = null,
    val minDurationMinutes: Int? = null,
    val totalAvailableParticipants: Int = 0
) {
    // Returns true if any filter criteria have been set
    val isActive: Boolean
        get() {
            // A participant filter is only "Active" if it restricts the group
            val participantFilterActive = requiredCalendars.isNotEmpty() &&
                    requiredCalendars.size < totalAvailableParticipants

            return participantFilterActive ||
                    optionalCalendars.isNotEmpty() ||
                    weekdays.isNotEmpty() ||
                    timeWindow != null ||
                    minDurationMinutes != null
        }
}