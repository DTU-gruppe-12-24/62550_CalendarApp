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
    private val engine: AvailabilityEngine = AvailabilityEngine()
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
        _groups.value = groups
    }

    fun updateActiveGroup(index: Int) {
        _activeGroupIndex.value = index
        // When we switch group we should also clear filters to avoid problems with
        // searching for participants that don't exist.
        clearFilters()
    }
    fun updateMonth(month: LocalDate) { _selectedMonth.value = month }

    fun clearFilters() {
        _filterQuery.value = FilterQuery()
    }
}

// Helper
enum class DurationUnit { MINUTES, HOURS }

