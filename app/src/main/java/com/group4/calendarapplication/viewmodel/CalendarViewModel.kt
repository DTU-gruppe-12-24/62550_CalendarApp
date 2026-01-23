package com.group4.calendarapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group4.calendarapplication.domain.filter.AvailabilityEngine
import com.group4.calendarapplication.domain.filter.FilterQuery
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.views.getDatesInMonth
import kotlinx.coroutines.flow.*
import java.time.LocalDate

class CalendarViewModel(
    val engine: AvailabilityEngine = AvailabilityEngine()
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    private val _filterQuery = MutableStateFlow(FilterQuery())
    val filterQuery: StateFlow<FilterQuery> = _filterQuery.asStateFlow()
    private val _activeGroupIndex = MutableStateFlow(0)
    val activeGroupIndex = _activeGroupIndex.asStateFlow()

    private val _selectedMonth = MutableStateFlow(LocalDate.now())
    val selectedMonth = _selectedMonth.asStateFlow()


    val dayAvailability: StateFlow<Map<LocalDate, Boolean>> =
        combine(_groups, _activeGroupIndex, _selectedMonth, _filterQuery) { groups, index, month, query ->

            val group = groups.getOrNull(index) ?: return@combine emptyMap()

            if (!query.isActive) {
                return@combine emptyMap()
            }

            val currentCalendars = group.calendars.toList()

            val days = getDatesInMonth(month)

            days.associateWith { day ->
                engine.isDayAvailable(
                    calendars = currentCalendars,
                    date = day,
                    query = query
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun updateFilter(newQuery: FilterQuery) {
        _filterQuery.value = newQuery
    }

    fun setGroups(groups: List<Group>) {
        val isFirstLoad = _groups.value.isEmpty()
        _groups.value = groups

        // Only reset filters on the very first load to prevent
        // wiping user filters during a background data refresh.
        if (isFirstLoad) {
            resetFiltersForCurrentGroup()
        }
    }

    fun updateActiveGroup(index: Int) {
        if (_activeGroupIndex.value == index) return
        _activeGroupIndex.value = index
        resetFiltersForCurrentGroup()
    }
    fun updateMonth(month: LocalDate) { _selectedMonth.value = month }

    private fun resetFiltersForCurrentGroup() {
        val group = _groups.value.getOrNull(_activeGroupIndex.value)
        val calendars = group?.calendars ?: emptyList()

        _filterQuery.value = FilterQuery(
            requiredCalendars = calendars.toSet(),
            totalAvailableParticipants = calendars.size
        )
    }
}

// Helper
enum class DurationUnit { MINUTES, HOURS }

