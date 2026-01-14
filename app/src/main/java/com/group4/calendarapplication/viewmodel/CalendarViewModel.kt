package com.group4.calendarapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group4.calendarapplication.domain.filter.AvailabilityEngine
import com.group4.calendarapplication.domain.filter.FilterQuery
import com.group4.calendarapplication.models.Group
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val engine: AvailabilityEngine = AvailabilityEngine()
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    private val _activeGroupIndex = MutableStateFlow(0)
    private val _filterQuery = MutableStateFlow(FilterQuery())

    val filterQuery: StateFlow<FilterQuery> = _filterQuery.asStateFlow()

    val dayAvailability: StateFlow<Map<LocalDate, Boolean>> =
        combine(_groups, _activeGroupIndex, _filterQuery) { groups, index, query ->
            val group = groups.getOrNull(index) ?: return@combine emptyMap()

            if (!query.isActive) {
                return@combine emptyMap<LocalDate, Boolean>()
            }

            val month = LocalDate.now()
            val days = getDatesInMonth(month)

            days.associateWith { day ->
                engine.isDayAvailable(
                    calendars = group.calendars,
                    date = day,
                    query = query
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun updateFilter(newQuery: FilterQuery) {
        _filterQuery.value = newQuery
    }

    fun setGroups(groups: List<Group>) {
        _groups.value = groups
    }

    fun setActiveGroup(index: Int) {
        _activeGroupIndex.value = index
    }

    fun clearFilters() {
        _filterQuery.value = FilterQuery() // Resets to empty sets and nulls
    }
    // helper (does not belong here but oh well)
    private fun getDatesInMonth(date: LocalDate): List<LocalDate> {
        val yearMonth = YearMonth.from(date)
        return (1..yearMonth.lengthOfMonth()).map {
            yearMonth.atDay(it)
        }
    }
}
