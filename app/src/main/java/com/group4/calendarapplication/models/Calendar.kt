package com.group4.calendarapplication.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
class Calendar(val name: String, val color: Color, var dates: ArrayList<LocalDate>) : java.io.Serializable {}