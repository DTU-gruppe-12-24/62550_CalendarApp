package com.group4.calendarapplication.views

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.domain.filter.FilterQuery
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.viewmodel.DurationUnit
import com.group4.calendarapplication.domain.filter.AvailabilityEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarFilterBar(
    calendars: List<Calendar>,
    filterQuery: FilterQuery,
    onFilterChange: (FilterQuery) -> Unit,
    onJumpToDate: (LocalDateTime) -> Unit
) {
    // State for controlling which dialog is open
    var showParticipants by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showNextSlotsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Top Row: Title, Find Slot, and Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (filterQuery.isActive) {
                        Button(
                            onClick = { showNextSlotsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Find Slot")
                        }

                        Spacer(Modifier.width(8.dp))

                        TextButton(onClick = { onFilterChange(FilterQuery(requiredCalendars = calendars.toSet())) }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
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

    if (showNextSlotsDialog) {
        NextSlotsDialog(
            calendars = calendars,
            filterQuery = filterQuery,
            onDismiss = { showNextSlotsDialog = false },
            onSlotClick = { dateTime ->
                onJumpToDate(dateTime)
                showNextSlotsDialog = false
            }
        )
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
    selected: Set<Calendar>,
    onConfirm: (Set<Calendar>) -> Unit,
    onDismiss: () -> Unit
) {
    var localSelected by remember {
        // If the passed selection is empty, default to selecting everyone
        mutableStateOf(selected.ifEmpty { calendars.toSet() })
    }
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
                Text(
                    "Which Participants must be available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )

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
    var startMinutes by remember { mutableIntStateOf(filterQuery.timeWindow?.start ?: (8 * 60)) }
    var endMinutes by remember { mutableIntStateOf(filterQuery.timeWindow?.endInclusive ?: (24 * 60)) }
    val totalMinutes = filterQuery.minDurationMinutes ?: 0

    // Determine unit and display amount based on stored totalMinutes
    var selectedUnit by remember {
        mutableStateOf(
            if (totalMinutes > 0 && totalMinutes % 60 == 0) DurationUnit.HOURS
            else DurationUnit.MINUTES
        )
    }

    var durationAmount by remember {
        mutableStateOf(
            if (totalMinutes == 0) ""
            else if (selectedUnit == DurationUnit.HOURS) (totalMinutes / 60).toString()
            else totalMinutes.toString()
        )
    }

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
                Text("Filter Parameters", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))

                // Days
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

                // Time Window
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

                // Duration
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
                            val amountDouble = durationAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val calculatedMinutes = when (selectedUnit) {
                                DurationUnit.MINUTES -> amountDouble.toInt()
                                DurationUnit.HOURS -> (amountDouble * 60).toInt()
                            }.coerceAtMost(1440) // Hard cap at 24 hours

                            onApply(
                                localQuery.copy(
                                    timeWindow = startMinutes..endMinutes,
                                    minDurationMinutes = if (calculatedMinutes > 0) calculatedMinutes else null
                                )
                            )
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
            // Number Input
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("0") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Unit Selector
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextSlotsDialog(
    calendars: List<Calendar>,
    filterQuery: FilterQuery,
    onSlotClick: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    val engine = remember { AvailabilityEngine() }
    val context = LocalContext.current

    var searchStartDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedLimit by remember { mutableIntStateOf(5) }
    var expandedLimit by remember { mutableStateOf(false) }

    val slots = remember(searchStartDate, selectedLimit, filterQuery) {
        engine.findNextSlots(calendars, searchStartDate, filterQuery, selectedLimit)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Find Next Timeslot", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Easily find the next free timeslot for your group according to the filters applied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
                Spacer(Modifier.height(8.dp))

                // Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val picker = DatePickerDialog(context, { _, y, m, d ->
                                searchStartDate = LocalDate.of(y, m + 1, d)
                            }, searchStartDate.year, searchStartDate.monthValue - 1, searchStartDate.dayOfMonth)
                            picker.show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(searchStartDate.format(DateTimeFormatter.ofPattern("MMM d")))
                    }

                    Box(modifier = Modifier.weight(0.8f)) {
                        OutlinedButton(
                            onClick = { expandedLimit = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Show $selectedLimit")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = expandedLimit,
                            onDismissRequest = { expandedLimit = false }
                        ) {
                            listOf(3, 5, 10, 20, 50).forEach { limit ->
                                DropdownMenuItem(
                                    text = { Text(limit.toString()) },
                                    onClick = {
                                        selectedLimit = limit
                                        expandedLimit = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.height(0.3.dp))
                Spacer(Modifier.height(16.dp))

                // Results
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    if (slots.isEmpty()) {
                        Text(
                            "No slots found.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(slots) { slot ->
                                SlotItem(slot = slot, onClick = { onSlotClick(slot) })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun SlotItem(slot: LocalDateTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = slot.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = slot.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
    }
}

private fun Int.toTimeString(): String = "%02d:%02d".format(this / 60, this % 60)