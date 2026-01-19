package com.group4.calendarapplication.views

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.MainActivity
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.ui.theme.LocalCalendarColors

@Composable
fun HomeView(groups: List<Group>, modifier: Modifier) {
    val mainActivity = LocalActivity.current as MainActivity
    var currentGroup by rememberSaveable { mutableIntStateOf(-1) }

    if(currentGroup == -1 || groups.isEmpty()) {
        GroupOverview(groups, { group -> currentGroup = group }, modifier)
    } else {
        val (group, setGroup) = rememberSaveable { mutableStateOf(groups[currentGroup]) }

        BackHandler(enabled = true) {
            currentGroup = -1
        }

        EditGroup(
            Group(group.name, group.calendars),
            modifier,
            onExit = { currentGroup = -1 },
            onEdit = { g ->
                setGroup(g)
                mainActivity.updateGroup(currentGroup, g)
                currentGroup--
                currentGroup++
            },
            deleteGroup = {
                mainActivity.removeGroup(currentGroup)
                currentGroup = -1
            }
        )
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.tertiaryContainer,RoundedCornerShape(CornerSize(8.dp)))
                    .clickable(onClick = { openCreateGroupDialog.value = true }),

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
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
        )  {
            Text(text = "Add new group", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(6.0f,TextUnitType.Em))
            Spacer(modifier = Modifier.size(5.dp))
            LimitedTextField(
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
    }
}

@Composable
fun EditGroup(group: Group, modifier: Modifier, onExit: () -> Unit, onEdit: (group: Group) -> Unit, deleteGroup: () -> Unit) {
    val editMade = remember { mutableStateOf(false) }

    val editCalendarIndex = remember { mutableIntStateOf(-1) }
    if (editCalendarIndex.intValue >= 0) {
        return EditCalendar(
            calendar = group.calendars[editCalendarIndex.intValue],
            modifier = modifier,
            onExit = { editCalendarIndex.intValue = -1; },
            onEdit = { calendar ->
                group.calendars[editCalendarIndex.intValue] = calendar; editMade.value = true
            }
        )
    }

    var editName by remember { mutableStateOf(false) }
    val colors = LocalCalendarColors.current
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(16.dp))
        val nameValue = remember { mutableStateOf(group.name) }
        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { editName = true })) {
            if (!editName) {
                Text(text = group.name, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(8.0f,TextUnitType.Em))
                EditIcon(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp))
            } else {
                LimitedTextField(
                    value = nameValue.value,
                    onValueChange = { v ->
                        editMade.value = (group.name != v)
                        nameValue.value = v
                        group.name = v
                    },
                    onDismissRequest = {
                        editName = false
                    },
                    modifier = Modifier.align(Alignment.Center),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(8.0f,TextUnitType.Em))
                )
                CloseIconButton(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp), onClick = { editName = false })
            }
        }
        Spacer(modifier = Modifier.size(32.dp))

        Text(text = "Calendars", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(5.0f,TextUnitType.Em))
        Spacer(modifier = Modifier.size(10.dp))

        val confirmCalendarDelete = remember { mutableStateOf(false) }
        val calendarDeleteIndex = remember { mutableIntStateOf(-1) }
        if (confirmCalendarDelete.value) ConfirmDialog(text = "Do you want to delete calendar \"${group.calendars[calendarDeleteIndex.intValue].name}\" from \"${group.name}\"? (This cannot be undone)", onSuccess = { group.calendars.removeAt(calendarDeleteIndex.intValue); confirmCalendarDelete.value = false } , onFail = { confirmCalendarDelete.value = false })

        val items = ArrayList<@Composable () -> Unit>()

        val errorMessage = remember { mutableStateOf(null as String?)}
        if (errorMessage.value != null) ErrorMessage(errorMessage.value ?: "Unknown error") { errorMessage.value = null }

        @Composable
        fun AddCalendarDialog(
            onDismissRequest: () -> Unit,
            addCalender: (cal: Calendar) -> Unit,
        ) {
            Dialog(onDismissRequest = onDismissRequest) {

                val showHelp = remember { mutableStateOf(false) }

                if (showHelp.value) {
                    AddCalendarHelpDialog { showHelp.value = false }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    Box {
                        CloseIconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(40.dp)
                        )

                        HelpIconButton(
                            onClick = { showHelp.value = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(40.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            UrlCalendarImport(
                                onResult = { calendar ->
                                    if (calendar != null) addCalender(calendar)
                                    onDismissRequest()
                                },
                                onError = { msg -> errorMessage.value = msg },
                                onClose = onDismissRequest
                            )

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))

                            FileCalendarImport(
                                onResult = { calendar ->
                                    if (calendar != null) addCalender(calendar)
                                    onDismissRequest()
                                },
                                onError = { msg -> errorMessage.value = msg }
                            )
                        }
                    }
                }
            }
        }
        for (i in 0..<group.calendars.size) {
            items.add({
                Row(
                    Modifier.fillMaxSize().padding(2.dp).clickable{ editCalendarIndex.intValue = i },
                    Arrangement.SpaceBetween
                ) {
                    CalendarLegend(group.calendars[i], Modifier.align(Alignment.CenterVertically))
                    Box(Modifier.align(Alignment.CenterVertically).clickable { calendarDeleteIndex.intValue = i; confirmCalendarDelete.value = true }) {
                        DeleteIcon(Modifier.align(Alignment.CenterEnd))
                    }
                }
            })
        }
        val openCreateCalendarDialog = remember { mutableStateOf(false) }
        if (openCreateCalendarDialog.value)
            AddCalendarDialog(
                onDismissRequest = { openCreateCalendarDialog.value = false },
                addCalender = { cal ->
                    group.calendars.add(cal)
                    editMade.value = true
                    onEdit(group)
                }
            )
        items.add({
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(CornerSize(8.dp))).clickable(
                    onClick = { openCreateCalendarDialog.value = true }
                ),
            ) {
                Text("Add new calendar", Modifier.align(Alignment.Center))
            }
        })
        ItemList(items, Modifier.fillMaxWidth().fillMaxHeight(0.90f))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    group.name = nameValue.value
                    onEdit(group)
                    onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                SaveIcon(Modifier.size(24.dp))
                Text("Save")
            }

            val confirmCancel = remember { mutableStateOf(false) }
            if (confirmCancel.value) ConfirmDialog(text = "Are you sure you want to exit without saving your changes?", onSuccess = { onExit(); confirmCancel.value = false } , onFail = { confirmCancel.value = false })
            Button(
                onClick = {
                    if (editMade.value) confirmCancel.value = true
                    else onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                CloseIcon(Modifier.size(24.dp))
                Text("Cancel")
            }
            val confirmGroupDelete = remember { mutableStateOf(false) }
            if (confirmGroupDelete.value)
                ConfirmDialog(
                    text = "Do you want to delete group \"${group.name}\"? (This cannot be undone)",
                    onSuccess = { confirmGroupDelete.value = false; deleteGroup() },
                    onFail = { confirmGroupDelete.value = false }
                )
            Button(
                onClick = {
                    confirmGroupDelete.value = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.calendarred,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer

                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                DeleteIcon(Modifier.size(24.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
fun EditCalendar(calendar: Calendar, modifier: Modifier, onExit: () -> Unit, onEdit: (calendar: Calendar) -> Unit) {
    val editMade = remember { mutableStateOf(false) }

    val confirmCancel = remember { mutableStateOf(false) }
    if (confirmCancel.value) ConfirmDialog(text = "Are you sure you want to exit without saving your changes?", onSuccess = { onExit(); confirmCancel.value = false } , onFail = { confirmCancel.value = false })
    fun close() {
        if (editMade.value) confirmCancel.value = true
        else onExit()
    }

    var editName by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(16.dp))
        val nameValue = remember { mutableStateOf(calendar.name) }
        Box(Modifier.fillMaxWidth()) {
            CloseIconButton(
                Modifier.size(32.dp).align(Alignment.TopStart).offset((-8).dp, 0.dp),
                onClick = { close() }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { editName = true })) {
            if (!editName) {
                Text(text = calendar.name, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(6.0f,TextUnitType.Em))
                EditIcon(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp))
            } else {
                LimitedTextField(
                    value = nameValue.value,
                    onValueChange = { v ->
                        editMade.value = (calendar.name != v)
                        nameValue.value = v
                        calendar.name = v
                    },
                    onDismissRequest = {
                        editName = false
                    },
                    modifier = Modifier.align(Alignment.Center),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(6.0f,TextUnitType.Em))
                )
                CloseIconButton(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp), onClick = { editName = false })
            }
        }
        Spacer(modifier = Modifier.size(32.dp))

        val editColorDialog = remember { mutableStateOf(false) }
        Row(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.8f).height(64.dp), horizontalArrangement = Arrangement.Center) {
            Text(text = "Color: ", modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(5.0f,TextUnitType.Em))
            Box(Modifier.fillMaxHeight().aspectRatio(1.0f).background(color = calendar.color, shape = CircleShape).padding(16.dp, 2.dp).clickable{ editColorDialog.value = true }) {}
        }
        if (editColorDialog.value) {
            Dialog({ editColorDialog.value = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    ColorPicker(calendar.color, onEdit = { color -> calendar.color = color; editMade.value = true }, onExit = { editColorDialog.value = false })
                }
            }
        }


        Spacer(modifier = Modifier.size(10.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    calendar.name = nameValue.value
                    onEdit(calendar)
                    onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                SaveIcon(Modifier.size(24.dp))
                Text("Save")
            }

            Button(
                onClick = { close() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                CloseIcon(Modifier.size(24.dp))
                Text("Cancel")
            }

        }
    }
}


@Composable
fun ItemList(items: List<@Composable () -> Unit>, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        for (item in items) {
            Box(
                Modifier
                    .requiredWidth(350.dp)
                    .requiredHeight(50.dp)
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
fun EditIcon(modifier: Modifier = Modifier) {
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
            MaterialTheme.colorScheme.secondary

        )
    }
}
@Composable
fun HelpIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)

    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}


@Composable
fun DeleteIcon(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(0.8f)
            .padding(5.dp),
        Alignment.Center
    ) {
        Icon(
            Icons.Default.Delete,
            "Delete",
            Modifier.fillMaxSize(),
            MaterialTheme.colorScheme.secondary

        )
    }
}

@Composable
fun SaveIcon(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp),
        Alignment.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            "Save",
            Modifier.fillMaxSize(),
            MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp),
        Alignment.Center
    ) {
        Icon(
            Icons.Default.Close,
            "Cancel",
            Modifier.fillMaxSize(),
            MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun CloseIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier) {
        Icon(
            Icons.Default.Close,
            "Cancel",

        )
    }
}

@Composable
fun ConfirmDialog(text: String, onSuccess: () -> Unit, onFail: () -> Unit) {
    val colors = LocalCalendarColors.current
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
        )  {
            Spacer(modifier = Modifier.size(16.dp))
            Box(Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally)) {
                Text(
                    text = text,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    fontSize = TextUnit(4.0f, TextUnitType.Em)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))

            Row(modifier = Modifier.fillMaxWidth(0.9f).align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .background(
                            color = colors.calendarred,
                            shape = RoundedCornerShape(CornerSize(16.dp)),
                        )
                        .padding(10.dp, 4.dp)
                        .clickable { onSuccess() }
                ) {
                    Text("Yes")
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(CornerSize(16.dp)),
                        )
                        .padding(10.dp, 4.dp)
                        .clickable { onFail() }
                ) {
                    Text("No")
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

const val TEXT_LIMIT = 30
@Composable
fun LimitedTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder:  @Composable (() -> Unit)? = null, textStyle: TextStyle = LocalTextStyle.current, onDismissRequest: (() -> Unit)? = null) {
    TextField(
        value = value,
        onValueChange = { v ->
            // Limit value and remove new lines
            val updatedValue = v.take(TEXT_LIMIT).replace("\n", "")
            onValueChange(updatedValue)
            // Dismiss on enter
            if (v.contains("\n")) {
                if(onDismissRequest != null) onDismissRequest()
            }
        },
        modifier = modifier,
        placeholder = placeholder,
        textStyle = textStyle,
    )
}

@Composable
fun AddCalendarHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box {

                CloseIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(top = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        "How to add a calendar",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "You can add calendars using a URL, a ZIP file, or an .ics file.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Apple Calendar (URL)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        """
                        1. Go to iCloud.com and sign in  
                        2. Open the Calendar app  
                        3. Click the information icon
                        4. Enable Public Calendar  
                        5. Copy the provided URL
                        """.trimIndent(),

                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Google Calendar (file)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        """
                        1. Open Google Calendar on a computer
                        2. Open "Settings"
                        3. Export individual or combined calendars
                            3.1 Under "Import & Export" all your calendars can be exported combined as one
                            3.2 Under each individual calendar, the button "Export Calendar" will export the individual calendar
                        """.trimIndent(),

                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Google Calendar (URL)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        """
                        1. Open Google Calendar on a computer  
                        2. Under “My calendars”, select the calendar  
                        3. Open “Settings and sharing”
                        4. Under "Integrate calendar" find “Public address in iCal format” or "Secret address in iCal format" 
                        5. Copy the provided url
                        """.trimIndent(),

                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }
            }
        }
    }
}
