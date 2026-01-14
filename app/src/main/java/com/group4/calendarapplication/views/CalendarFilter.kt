package com.group4.calendarapplication.views

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.domain.filter.FilterQuery
import com.group4.calendarapplication.models.Calendar
import java.time.DayOfWeek

@Composable
fun CalendarFilterBar(
    calendars: List<Calendar>,
    filterQuery: FilterQuery,
    onFilterChange: (FilterQuery) -> Unit
) {
    var showParticipants by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)

                // Show "Clear" only if filters are actually set
                if (filterQuery.isActive) {
                    TextButton(onClick = { onFilterChange(FilterQuery()) }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton("Participants") { showParticipants = true }
                FilterButton("Time & days") { showTime = true }
            }
        }
    }

    if (showParticipants) {
        ParticipantSelectorDialog(
            calendars = calendars,
            selected = filterQuery.requiredCalendars,
            onConfirm = {
                onFilterChange(filterQuery.copy(requiredCalendars = it))
                showParticipants = false
            },
            onDismiss = { showParticipants = false }
        )
    }

    if (showTime) {
        TimeAndWeekdaySelectorDialog(
            filterQuery = filterQuery,
            onApply = {
                onFilterChange(it)
                showTime = false
            },
            onDismiss = { showTime = false }
        )
    }
}

@Composable
private fun FilterButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ParticipantSelectorDialog(
    calendars: List<Calendar>,
    selected: Set<Calendar>, // Changed from Set<String>
    onConfirm: (Set<Calendar>) -> Unit,
    onDismiss: () -> Unit
) {
    var localSelected by remember { mutableStateOf(selected) }
    val allSelected = localSelected.size == calendars.size && calendars.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Participants", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        localSelected = if (allSelected) emptySet() else calendars.toSet()
                    }) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp))

                Column(
                    Modifier.fillMaxWidth()
                        .weight(1f, fill = false) // Allow scrolling if list is long
                        .verticalScroll(rememberScrollState())
                ) {
                    calendars.forEach { calendar ->
                        val isSelected = calendar in localSelected
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    localSelected = if (isSelected) localSelected - calendar
                                    else localSelected + calendar
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Box(Modifier.weight(1f)) {
                                CalendarLegend(calendar, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(localSelected) }) { Text("Confirm") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeAndWeekdaySelectorDialog(
    filterQuery: FilterQuery,
    onApply: (FilterQuery) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localQuery by remember { mutableStateOf(filterQuery) }
    var startMinutes by remember { mutableStateOf(filterQuery.timeWindow?.start ?: (9 * 60)) }
    var endMinutes by remember { mutableStateOf(filterQuery.timeWindow?.endInclusive ?: (17 * 60)) }
    var duration by remember { mutableStateOf(filterQuery.minDurationMinutes?.toString() ?: "") }

    fun showNativePicker(isStart: Boolean) {
        val initialTotal = if (isStart) startMinutes else endMinutes
        TimePickerDialog(context, { _, hour, minute ->
            val total = hour * 60 + minute
            if (isStart) startMinutes = total else endMinutes = total
        }, initialTotal / 60, initialTotal % 60, true).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Availability Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                WeekdaySelector(
                    selected = localQuery.weekdays,
                    onToggle = { day ->
                        val newDays = if (day in localQuery.weekdays)
                            localQuery.weekdays - day else localQuery.weekdays + day
                        localQuery = localQuery.copy(weekdays = newDays)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showNativePicker(true) }, modifier = Modifier.weight(1f)) {
                        Text("From: ${startMinutes.toTimeString()}")
                    }
                    OutlinedButton(onClick = { showNativePicker(false) }, modifier = Modifier.weight(1f)) {
                        Text("To: ${endMinutes.toTimeString()}")
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Min duration (mins)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onApply(localQuery.copy(
                            timeWindow = startMinutes..endMinutes,
                            minDurationMinutes = duration.toIntOrNull()
                        ))
                    }) { Text("Apply") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdaySelector(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(day) },
                label = { Text(day.name.take(3)) }
            )
        }
    }
}

private fun Int.toTimeString(): String = "%02d:%02d".format(this / 60, this % 60)