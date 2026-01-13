package com.group4.calendarapplication.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.ui.theme.LocalCalendarColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun CalendarView(groups: List<Group>, modifier: Modifier) {
    var activeGroup by rememberSaveable { mutableIntStateOf(0) }
    if(activeGroup >= groups.size) activeGroup = -1

    val calendars = if (activeGroup >= 0) groups[activeGroup].calendars else ArrayList()

    // Dialog popup
    val isDialogOpen = remember { mutableStateOf(false) }
    val dialogDate = remember { mutableStateOf(LocalDate.now()) }
    if(isDialogOpen.value && groups.size > activeGroup) {
        Dialog(onDismissRequest = { isDialogOpen.value = false }) {
            Card(
                modifier = Modifier
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
            )  {
                Text(dialogDate.value.toString(), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp))
                Text("Occupied by:", modifier = Modifier.padding(10.dp))
                calendars.forEach { calendar ->
                    if (calendar.dates.any { event -> event.isDateTimeWithInEvent(dialogDate.value) }) {
                        CalendarLegend(calendar, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    // Calendar component
    Column(modifier = modifier, verticalArrangement = Arrangement.Top) {

        Column(modifier = Modifier.fillMaxHeight()) {
            // Legend
            if (activeGroup != -1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    //verticalArrangement = Arrangement.Bottom
                ) {
                    calendars.forEach { calendar ->
                        CalendarLegend(calendar, Modifier.fillMaxWidth())
                    }
                }
            }

            // Calendar
            CalendarComponent((if (activeGroup < 0) null else groups[activeGroup]), { date ->
                isDialogOpen.value = true
                dialogDate.value = date
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))


            // Filters
            CalendarFilterBar(
                calendars = calendars
            )

            Spacer(modifier = Modifier.size(10.dp).weight(1f))

            // Group selector
            if(groups.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        )
                        .padding(12.dp)

                ) {
                    val expanded = remember { mutableStateOf(false) }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .clickable(onClick = { expanded.value = true })
                    ) {
                        Text(
                            text = groups[activeGroup].name,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            "Select active calendar group",
                        )
                    }

                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .background(color = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        for (i in 0..<groups.size) {
                            DropdownMenuItem(
                                text = { Text(text = groups[i].name) },
                                onClick = {
                                    activeGroup = i
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarLegend(calendar: Calendar, modifier: Modifier = Modifier) {
    Row (modifier = modifier) {
        Card(
            shape = CircleShape,
            modifier = Modifier
                .size(25.dp, 25.dp)
                .padding(start = 10.dp, bottom = 5.dp, top = 5.dp),

            colors = CardColors(
                calendar.color,
                ButtonDefaults.buttonColors().contentColor,
                ButtonDefaults.buttonColors().disabledContainerColor,
                ButtonDefaults.buttonColors().disabledContentColor,
            )
        ) {
        }
        Text(
            text = calendar.name,
            modifier = Modifier.padding(5.dp, 0.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun CalendarComponent(
    group: Group?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var current by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val datesInCurrentMonth = getDatesInMonth(current)
    val occupiedDates = datesInCurrentMonth.map { date ->
        val colors = ArrayList<Color>()
        if(group != null)
            for (calendar in group.calendars) {
                if(calendar.dates.any { event -> event.isDateTimeWithInEvent(date) })
                    colors.add(calendar.color)
            }
        Pair(date, colors)
    }

    val combinedSwipeDelta = remember { mutableFloatStateOf(0.0f) }
    Column(verticalArrangement = Arrangement.Top, modifier = modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            combinedSwipeDelta.floatValue += delta
            if(combinedSwipeDelta.floatValue > 250.0f) {
                current = current.minusMonths(1) // Go to previous month
                combinedSwipeDelta.floatValue = -100.0f
            }
            else if(combinedSwipeDelta.floatValue < -250.0f) {
                current = current.plusMonths(1) // Go to next month
                combinedSwipeDelta.floatValue = 100.0f
            }
        },

    )) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { current = current.minusMonths(1) },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Previous month",
                )
            }
            IconButton(
                onClick = { current = current.plusMonths(1) },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    "Next month",
                )
            }

            Text(
                text = current.format(DateTimeFormatter.ofPattern("MMMM y")),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { current = LocalDate.now() },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    "Go to current date",
                )
            }
        }
        CalendarGrid(
            date = occupiedDates.toList(),
            calendarCount = group?.calendars?.size ?: 0,
            onClick = onDateClick,
            modifier = Modifier
                .wrapContentHeight()
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    status: Color,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = date.formatToCalendarDay()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .padding(2.dp)
            .background(
                shape = RoundedCornerShape(CornerSize(8.dp)),
                color = status,
            )
            .clip(RoundedCornerShape(CornerSize(8.dp)))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(modifier = Modifier
            .fillMaxWidth()
            .offset(0.dp, 15.dp)
            .align(Alignment.Center), horizontalArrangement = Arrangement.SpaceAround) {
            for (color in colors) {
                Card(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(10.dp, 10.dp)
                        .align(Alignment.CenterVertically),
                    colors = CardColors(
                        color,
                        ButtonDefaults.buttonColors().contentColor,
                        ButtonDefaults.buttonColors().disabledContainerColor,
                        ButtonDefaults.buttonColors().disabledContentColor,
                    )
                ) {}
            }
        }
    }
}


@Composable
private fun WeekdayCell(weekday: Int, modifier: Modifier = Modifier) {
    val text = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(weekday.toLong()).getDayOfWeek3Letters()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CalendarGrid(
    date: List<Pair<LocalDate, List<Color>>>,
    calendarCount: Int,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalGap = with(LocalDensity.current) { 2.dp.roundToPx() }
    val verticalGap = with(LocalDensity.current) { 2.dp.roundToPx() }

    val weekdayFirstDay = date.first().first.dayOfWeek.ordinal
    val weekdays = (0..6).toList()

    Layout(
        content = {
            Box(modifier = modifier.aspectRatio(1f)) {
                Text(
                    text = "#",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            weekdays.forEach {
                WeekdayCell(weekday = it)
            }
            // Display first week number (if month doesn't start on a monday)
            if(weekdayFirstDay > 0) {
                Box(modifier = modifier.aspectRatio(1f).padding(end = 3.dp)) {
                    val day = date.first().first.minusDays(date.first().first.dayOfWeek.ordinal.toLong())
                    var num = (day.dayOfYear / 7) + (if(((day.dayOfYear - 1) % 7) > 0) 1 else 0) + (if(day.withDayOfYear(1).dayOfWeek.ordinal <= 3) 1 else 0)
                    if(date.first().first.dayOfYear <= 7) num = (if(date.first().first.withDayOfYear(1).dayOfWeek.ordinal <= 3) 1 else 0)
                    Text(
                        text = num.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
            // Align the first day of the month to the correct weekday
            repeat(weekdayFirstDay) {
                Spacer(modifier = Modifier)
            }
            // Add all calendar cells
            date.forEach {
                // Display week numbers
                if(it.first.dayOfWeek == DayOfWeek.MONDAY) {
                    Box(modifier = modifier.aspectRatio(1f).padding(end = 3.dp)) {
                        Text(
                            text = ((it.first.dayOfYear / 7 ) + (if(((it.first.dayOfYear - 1) % 7) > 0) 1 else 0) + (if(it.first.withDayOfYear(1).dayOfWeek.ordinal <= 3) 1 else 0)).toString(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
                val colors = LocalCalendarColors.current
                var status = colors.calendargreen
                if(it.second.isNotEmpty()) {
                    if(it.second.size >= calendarCount * 0.9f) status = colors.calendarred
                    else status = colors.calendaryellow
                }
                CalendarCell(date = it.first, status = status, colors = it.second, onClick = { onClick(it.first) })
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val totalWidthWithoutGap = constraints.maxWidth - (horizontalGap * 7)
        val singleWidth = totalWidthWithoutGap / 8

        val xPos: MutableList<Int> = mutableListOf()
        val yPos: MutableList<Int> = mutableListOf()
        var currentX = 0
        var currentY = 0
        measurables.forEach { _ ->
            xPos.add(currentX)
            yPos.add(currentY)
            if (currentX + singleWidth + horizontalGap > totalWidthWithoutGap) {
                currentX = 0
                currentY += singleWidth + verticalGap
            } else {
                currentX += singleWidth + horizontalGap
            }
        }

        val placeables: List<Placeable> = measurables.map { measurable ->
            measurable.measure(constraints.copy(minHeight = 0, maxHeight = singleWidth, minWidth = 0, maxWidth = singleWidth))
        }

        layout(
            width = constraints.maxWidth,
            height = currentY + singleWidth + verticalGap,
        ) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = xPos[index],
                    y = yPos[index],
                )
            }
        }
    }
}

private fun LocalDate.formatToCalendarDay(): String = this.format(DateTimeFormatter.ofPattern("d"))
private fun LocalDate.getDayOfWeek3Letters() : String = this.format(DateTimeFormatter.ofPattern("EEE"))
private fun getDatesInMonth(month: LocalDate): List<LocalDate> {
    val firstDayOfMonth = LocalDate.of(month.year, month.month, 1)
    val lastDayOfMonth = firstDayOfMonth.plusMonths(1)
    return List<LocalDate>(lastDayOfMonth.minusDays(1).dayOfMonth, init = { i -> firstDayOfMonth.plusDays(i.toLong())})
}