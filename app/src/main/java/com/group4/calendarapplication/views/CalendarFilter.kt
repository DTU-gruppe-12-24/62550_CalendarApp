package com.group4.calendarapplication.views

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.domain.filter.FilterQuery
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.viewmodel.DurationUnit
import java.time.DayOfWeek
import java.util.Locale

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
            },
            onDismiss = { }
        )
    }

    if (showTime) {
        TimeAndWeekdaySelectorDialog(
            filterQuery = filterQuery,
            onApply = {
                onFilterChange(it)
            },
            onDismiss = { }
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

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

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
    var startMinutes by remember { mutableIntStateOf(filterQuery.timeWindow?.start ?: (9 * 60)) }
    var endMinutes by remember { mutableIntStateOf(filterQuery.timeWindow?.endInclusive ?: (17 * 60)) }
    var durationAmount by remember { mutableStateOf(filterQuery.minDurationMinutes?.let {
        if (it % 1440 == 0) (it / 1440).toString()
        else if (it % 60 == 0) (it / 60).toString()
        else it.toString()
    } ?: "") }

    var selectedUnit by remember { mutableStateOf(DurationUnit.MINUTES) }

    fun showNativePicker(isStart: Boolean) {
        val initialTotal = if (isStart) startMinutes else endMinutes

        TimePickerDialog(context, { _, hour, minute ->
            val total = hour * 60 + minute
            if (isStart) {
                startMinutes = total
            } else {
                endMinutes = total
            }
        }, initialTotal / 60, initialTotal % 60, true).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Search Parameters", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))

                // Section 1: Days
                Text("Available Days of the Week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                WeekdaySelector(
                    selected = localQuery.weekdays,
                    onToggle = { day ->
                        val newDays = if (day in localQuery.weekdays)
                            localQuery.weekdays - day else localQuery.weekdays + day
                        localQuery = localQuery.copy(weekdays = newDays)
                    }
                )

                Spacer(Modifier.height(24.dp))

                // Section 2: Time Window
                TimeIntervalSection(
                    label = "Daily Time Window Availability",
                    start = startMinutes.toTimeString(),
                    end = endMinutes.toTimeString(),
                    onStartClick = { showNativePicker(isStart = true) },
                    onEndClick = { showNativePicker(isStart = false) }
                )
                Text(
                    "Only suggests slots between these hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Section 3: Duration
                DurationInputSection(
                    amount = durationAmount,
                    onAmountChange = { durationAmount = it },
                    selectedUnit = selectedUnit,
                    onUnitSelection = { selectedUnit = it }
                )

                Spacer(Modifier.height(32.dp))

                // Buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val totalMinutes = when (selectedUnit) {
                                DurationUnit.MINUTES -> durationAmount.toDoubleOrNull()?.toInt()
                                DurationUnit.HOURS -> durationAmount.toDoubleOrNull()?.let { (it * 60).toInt() }
                            }?.coerceAtMost(1440) // Hard cap at 24 hours
                            onApply(localQuery.copy(
                                timeWindow = startMinutes..endMinutes,
                                minDurationMinutes = totalMinutes
                            ))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Apply Filters") }
                }
            }
        }
    }
}

@Composable
fun TimeIntervalSection(
    label: String,
    start: String,
    end: String,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(vertical = 4.dp), // Less vertical padding because children have padding
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start Time Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStartClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = start, style = MaterialTheme.typography.bodyLarge)
            }

            Text(
                text = "–",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            )

            // End Time Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEndClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = end, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun DurationInputSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedUnit: DurationUnit,
    onUnitSelection: (DurationUnit) -> Unit
) {
    Column {
        Text(
            "Minimum Event Duration",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "How long should the available slot be?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The Number Input
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("0") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // The Unit Selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp)
            ) {
                DurationUnit.entries.forEach { unit ->
                    val isSelected = unit == selectedUnit
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onUnitSelection(unit) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = unit.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
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