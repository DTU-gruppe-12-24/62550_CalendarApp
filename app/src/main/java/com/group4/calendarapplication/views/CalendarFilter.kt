package com.group4.calendarapplication.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.models.Calendar
import java.time.DayOfWeek


@Composable
fun CalendarFilterBar(
    calendars: List<Calendar>,
    modifier: Modifier = Modifier
) {
    var showParticipants by remember { mutableStateOf(false) }
    var showTimeFilter by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    label = "Participants",
                    onClick = { showParticipants = true }
                )

                FilterChip(
                    label = "Time & days",
                    onClick = { showTimeFilter = true }
                )
            }
        }
    }

    if (showParticipants) {
        ParticipantSelectorDialog(
            calendars = calendars,
            onDismiss = { showParticipants = false }
        )
    }

    if (showTimeFilter) {
        TimeAndWeekdaySelectorDialog(
            onDismiss = { showTimeFilter = false }
        )
    }
}



@Composable
fun FilterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(50),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium
        )
    }
}


@Composable
fun ParticipantSelectorDialog(
    calendars: List<Calendar>,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(calendars.toSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                Text(
                    text = "Participants",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                calendars.forEach { calendar ->
                    ParticipantRow(
                        calendar = calendar,
                        selected = calendar in selected,
                        onClick = {
                            selected =
                                if (calendar in selected)
                                    selected - calendar
                                else
                                    selected + calendar
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}



@Composable
private fun ParticipantRow(
    calendar: Calendar,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = CircleShape,
            modifier = Modifier.size(14.dp),
            colors = CardDefaults.cardColors(calendar.color),
        ) {}

        Text(
            text = calendar.name,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )

        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}






@Composable
fun TimeAndWeekdaySelectorDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Availability filter",
                    style = MaterialTheme.typography.titleMedium
                )

                WeekdaySelector()

                TimeIntervalSection(
                    label = "Time interval",
                    start = "18:00",
                    end = "24:00"
                )

                DurationSection(
                    duration = "2 hours"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun DurationSection(
    duration: String,
    onClick: () -> Unit = { /* Implementation */ }
) {
    Column {
        Text("Minimum duration", style = MaterialTheme.typography.labelMedium)

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onClick() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(duration)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}


@Composable
fun WeekdaySelector(
    selected: Set<DayOfWeek> = emptySet(),
    onToggle: (DayOfWeek) -> Unit = { /* Implementation */}
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = {
                    Text(
                        text = day.name.take(3).lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                }
            )
        }
    }
}
@Composable
fun TimeIntervalSection(
    label: String,
    start: String,
    end: String,
    onClick: () -> Unit =  { /* Implementation */ }
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onClick() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(start)
            Text("–")
            Text(end)
        }
    }
}