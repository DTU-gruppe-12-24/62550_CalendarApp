package com.group4.calendarapplication.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun CalendarView(modifier: Modifier) {
    CalendarComponent({}, modifier = modifier.fillMaxHeight())
}

@Composable
fun CalendarComponent(
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var current by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Column(modifier = modifier) {
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
        Spacer(modifier = Modifier.size(16.dp))
        CalendarGrid(
            date = getDatesInMonth(current).map { date -> Pair(date, date.dayOfMonth % 3 == 0) }.toList(),
            onClick = onDateClick,
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier.weight(1f))
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
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    signal: Boolean,
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
                color = if (signal) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            )
            .clip(RoundedCornerShape(CornerSize(8.dp)))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.Center)
        )
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
    date: List<Pair<LocalDate, Boolean>>,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalGap = with(LocalDensity.current) { 2.dp.roundToPx() }
    val verticalGap = with(LocalDensity.current) { 2.dp.roundToPx() }

    val weekdayFirstDay = date.first().first.dayOfWeek.ordinal
    val weekdays = (0..6).toList()

    Layout(
        content = {
            weekdays.forEach {
                WeekdayCell(weekday = it)
            }
            // Align the first day of the month to the correct weekday
            repeat(weekdayFirstDay) {
                Spacer(modifier = Modifier)
            }
            // Add all calendar cells
            date.forEach {
                CalendarCell(date = it.first, signal = it.second, onClick = { onClick(it.first) })
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val totalWidthWithoutGap = constraints.maxWidth - (horizontalGap * 6)
        val singleWidth = totalWidthWithoutGap / 7

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
            measurable.measure(constraints.copy(maxHeight = singleWidth, maxWidth = singleWidth))
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