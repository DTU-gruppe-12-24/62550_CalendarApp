package com.group4.calendarapplication.views

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.MainActivity
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.Group
import kotlin.random.Random

@Composable
fun HomeView(groups: List<Group>, modifier: Modifier) {
    val mainActivity = LocalActivity.current as MainActivity
    var currentGroup by rememberSaveable { mutableIntStateOf(-1) }

    if(currentGroup == -1 || groups.isEmpty()) {
        GroupOverview(groups, { group -> currentGroup = group }, modifier)
    } else {
        val (group, setGroup) = rememberSaveable { mutableStateOf(groups[currentGroup]) }

        EditGroup(group, modifier,
            onExit = { currentGroup = -1 },
            onEdit = { g ->
                setGroup(g)
                mainActivity.updateGroup(currentGroup, g)
                currentGroup--
                currentGroup++
            })
    }
}

@Composable
fun GroupOverview(groups: List<Group>, onSelectGroup: (group: Int) -> Unit, modifier: Modifier) {
    val openCreateGroupDialog = remember { mutableStateOf(false) }

    if (openCreateGroupDialog.value) {
        AddGroupDialog( onDismissRequest = { openCreateGroupDialog.value = false } )
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = "Calendar groups", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(8.0f,TextUnitType.Em))
        Spacer(modifier = Modifier.size(32.dp))

        val items = ArrayList<@Composable () -> Unit>()
        for (i in 0..<groups.size) {
            items.add({
                Box(modifier = Modifier.fillMaxSize().clickable(onClick = { onSelectGroup(i) })) {
                    Text(text = groups[i].name, modifier = Modifier.align(Alignment.Center))
                    EditIcon(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp))
                }
            })
        }
        items.add({
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(CornerSize(8.dp))).clickable(
                    onClick = { openCreateGroupDialog.value = true }
                ),
            ) {
                Text("Add new group", Modifier.align(Alignment.Center))
            }
        })
        ItemList(items, Modifier.fillMaxSize())
    }
}

@Composable
fun AddGroupDialog(onDismissRequest: () -> Unit) {
    val mainActivity = LocalActivity.current as MainActivity
    val name = remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        )  {
            Spacer(modifier = Modifier.size(16.dp))
            Text(text = "Add new group", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(8.0f,TextUnitType.Em))
            Spacer(modifier = Modifier.size(16.dp))
            TextField(
                value = name.value,
                onValueChange = { v -> name.value = v },
                placeholder = { Text("Name of group") },
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = {
                mainActivity.addGroup(Group(name.value, ArrayList()))
                onDismissRequest()
            }, modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.7f)) {
                Text("Create")
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
        CloseIcon(modifier = Modifier, onClick = onDismissRequest)
    }
}

@Composable
fun EditGroup(group: Group, modifier: Modifier, onExit: () -> Unit, onEdit: (group: Group) -> Unit) {
    val mainActivity = LocalActivity.current as MainActivity
    Column(modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onExit, Modifier.align(Alignment.End)) {
            Icon(
                Icons.Default.Close,
                "Cancel edit",
            )
        }

        Text(text = "Editing group " + group.name, modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth())

        val nameValue = remember { mutableStateOf(group.name) }
        TextField(
            value = nameValue.value,
            onValueChange = { v ->
                nameValue.value = v
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Calendars", modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.size(16.dp))

            val openCreateCalendarDialog = remember { mutableStateOf(false) }
            if (openCreateCalendarDialog.value)
                AddCalendarDialog(
                    onDismissRequest = { openCreateCalendarDialog.value = false },
                    addCalender = { cal ->
                        group.calendars.add(cal)
                        onEdit(group)
                    }
                )
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                Button(onClick = { openCreateCalendarDialog.value = true }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Add new calendar")
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            EditGroupCalendarList(group, onEdit = onEdit)
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    group.name = nameValue.value
                    onEdit(group)
                    onExit()
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Save group")
            }

            Button(
                onClick = {
                    mainActivity.removeGroup(group)
                    onExit()
                },
                colors = ButtonColors(
                    Color.Red,
                    ButtonDefaults.buttonColors().contentColor,
                    ButtonDefaults.buttonColors().disabledContainerColor,
                    ButtonDefaults.buttonColors().disabledContentColor,
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Delete group")
            }
        }
    }
}

@Composable
fun EditGroupCalendarList(group: Group, onEdit: (group: Group) -> Unit) {
    for (i in 0..<group.calendars.size) {
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    CalendarLegend(group.calendars[i], Modifier.align(Alignment.CenterVertically))

                    Button(
                        onClick = {
                            group.calendars.removeAt(i)
                            onEdit(group)
                        },
                        colors = ButtonColors(
                            Color.Red,
                            ButtonDefaults.buttonColors().contentColor,
                            ButtonDefaults.buttonColors().disabledContainerColor,
                            ButtonDefaults.buttonColors().disabledContentColor,
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Text("Delete") }
                }
            },
            onClick = {}
        )

        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
fun AddCalendarDialog(onDismissRequest: () -> Unit, addCalender: (cal: Calendar) -> Unit) {
    CalendarImport { calendar ->
        if (calendar != null) addCalender(calendar)
        onDismissRequest()
    }
    /*
    val name = remember { mutableStateOf("") }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        )  {
            TextField(
                value = name.value,
                onValueChange = { v -> name.value = v },
                placeholder = { Text("Name of calendar") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Button(onClick = {
                // Temp: Randomize calendar dates and color
                val calendar = Calendar(name.value, color = Color(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255),255), ArrayList())
                calendar.randomize(50)
                addCalender(calendar)
                onDismissRequest()
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Create calendar")
            }
        }
    }
     */
}

@Composable
fun ItemList(items: List<@Composable () -> Unit>, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        for (item in items) {
            Box(
                Modifier
                    .requiredWidth(256.dp)
                    .requiredHeight(48.dp)
                    .padding(2.dp)
                    .background(
                        shape = RoundedCornerShape(CornerSize(8.dp)),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                Alignment.Center
            ) {
                item()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun EditIcon(modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                shape = RoundedCornerShape(CornerSize(8.dp)),
                color = Color.Transparent, //MaterialTheme.colorScheme.primaryContainer,
            ),
        Alignment.Center
    ) {
        Icon(
            Icons.Default.Edit,
            "Edit",
            Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun DeleteIcon(modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                shape = RoundedCornerShape(CornerSize(2.dp)),
                color = MaterialTheme.colorScheme.errorContainer,
            ),
        Alignment.Center
    ) {
        Icon(
            Icons.Default.Delete,
            "Delete",
            Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun CloseIcon(modifier: Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier) {
        Icon(
            Icons.Default.Close,
            "Cancel",
        )
    }
}