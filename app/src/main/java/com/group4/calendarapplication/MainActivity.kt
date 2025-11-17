package com.group4.calendarapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.ui.theme.CalendarApplicationTheme
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class MainActivity : ComponentActivity() {
    private val groups = mutableStateOf<List<Group>>(ArrayList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadStateFromFile()

        enableEdgeToEdge()
        setContent {
            CalendarApplicationTheme {
                App(groups.value)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadStateFromFile()
    }

    override fun onStop() {
        saveStateToFile()
        super.onStop()
    }

    override fun onDestroy() {
        saveStateToFile()
        super.onDestroy()
    }

    private fun saveStateToFile() {
        val file = this.openFileOutput("state.data", MODE_PRIVATE)
        val os = ObjectOutputStream(file)
        os.writeObject(groups.value)
        os.close()
        file.close()
    }

    private fun loadStateFromFile() {
        try {
            val file = this.openFileInput("state.data")
            val input = ObjectInputStream(file)
            groups.value = input.readObject() as ArrayList<Group>
            input.close()
            file.close()
        } catch (e: Exception) {
            groups.value = ArrayList()
        }
    }

    fun addGroup(group: Group) {
        groups.value = { v: ArrayList<Group> ->
            v.add(group)
            v
        }(ArrayList(groups.value))
    }

    fun updateGroup(index: Int, group: Group) {
        groups.value = { v: ArrayList<Group> ->
            v[index] = group
            v
        }(ArrayList(groups.value))
    }

    fun removeGroup(group: Group) {
        groups.value = { v: ArrayList<Group> ->
            v.remove(group)
            v
        }(ArrayList(groups.value))
    }
}