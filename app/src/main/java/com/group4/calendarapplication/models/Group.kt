package com.group4.calendarapplication.models

import kotlinx.serialization.Serializable

@Serializable
class Group(var name: String, var calendars: ArrayList<Calendar>) : java.io.Serializable {}