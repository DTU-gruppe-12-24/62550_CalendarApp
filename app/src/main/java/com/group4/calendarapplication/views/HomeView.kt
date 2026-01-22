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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
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
import com.group4.calendarapplication.views.components.DialogActionRow

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

        val maxNameLength = 20
        val items = ArrayList<@Composable () -> Unit>()
        for (i in 0..<groups.size) {
            items.add({
                Box(modifier = Modifier.fillMaxSize().clickable(onClick = { onSelectGroup(i) })) {
                    Text(
                        text = groups[i].name.take(maxNameLength) + (if (groups[i].name.length > maxNameLength) "..." else ""),
                        modifier = Modifier.align(Alignment.Center)
                    )
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

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = "New Group",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.size(16.dp))

                LimitedTextField(
                    value = name.value,
                    onValueChange = { v -> name.value = v },
                    placeholder = { Text("Name of group") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(24.dp))

                DialogActionRow(
                    onDismiss = onDismissRequest,
                    onConfirm = {
                        if (name.value.isNotBlank()) {
                            mainActivity.addGroup(Group(name.value, ArrayList()))
                            onDismissRequest()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EditGroup(group: Group, modifier: Modifier, onExit: () -> Unit, onEdit: (group: Group) -> Unit, deleteGroup: () -> Unit) {
    var editMade by remember { mutableStateOf(false) }

    val editCalendarIndex = remember { mutableIntStateOf(-1) }
    if (editCalendarIndex.intValue >= 0) {
        return EditCalendar(
            calendar = group.calendars[editCalendarIndex.intValue],
            modifier = modifier,
            onExit = { editCalendarIndex.intValue = -1; },
            onEdit = { calendar ->
                group.calendars[editCalendarIndex.intValue] = calendar
                onEdit(group)
            }
        )
    }

    var editName by remember { mutableStateOf(false) }
    val colors = LocalCalendarColors.current
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(16.dp))
        var nameValue by remember { mutableStateOf(group.name) }
        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { editName = true })) {
            if (!editName) {
                Text(text = nameValue, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(8.0f,TextUnitType.Em))
                EditIcon(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp))
            } else {
                Box(Modifier.fillMaxWidth()) {
                    LimitedTextField(
                        value = nameValue,
                        onValueChange = { v ->
                            editMade = (nameValue != v)
                            nameValue = v
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
        }
        if (editMade) Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            Button(
                onClick = {
                    group.name = nameValue
                    onEdit(group)
                    editName = false
                    editMade = false
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
                onClick = {
                    nameValue = group.name
                    editName = false
                    editMade = false
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
            // State managed within the dialog
            val showHelp = remember { mutableStateOf(false) }

            // Logic for help dialog
            if (showHelp.value) {
                AddCalendarHelpDialog { showHelp.value = false }
            }

            Dialog(onDismissRequest = onDismissRequest) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Calendar",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HelpIconButton(onClick = { showHelp.value = true })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Import via URL or select a local file (.ics, .zip)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Unified Import UI
                        UrlCalendarImport(
                            onResult = { calendar ->
                                if (calendar != null) addCalender(calendar)
                                onDismissRequest()
                            },
                            onError = { _ -> /* Handle error here if needed */ },
                            onClose = onDismissRequest
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        // File Import Section
                        FileCalendarImport(
                            onResult = { calendar ->
                                if (calendar != null) addCalender(calendar)
                                onDismissRequest()
                            },
                            onError = { _ -> /* Handle error here if needed */ }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        DialogActionRow(
                            onDismiss = onDismissRequest
                        )
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
                    CalendarLegend(group.calendars[i], Modifier.align(Alignment.CenterVertically), 25)
                    Row {
                        Box(
                            Modifier.align(Alignment.CenterVertically)
                                .clickable { editCalendarIndex.intValue = i }
                        ) {
                            EditIcon(Modifier.align(Alignment.CenterStart).fillMaxHeight(0.66f))
                        }
                        Box(
                            Modifier.align(Alignment.CenterVertically).clickable {
                                calendarDeleteIndex.intValue = i; confirmCalendarDelete.value = true
                            }
                        ) {
                            DeleteIcon(
                                Modifier.align(Alignment.CenterEnd),
                                LocalCalendarColors.current.calendarred
                            )
                        }
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
            val confirmCancel = remember { mutableStateOf(false) }
            if (confirmCancel.value) ConfirmDialog(
                text = "Do you want to save your changes?",
                onSuccess = { group.name = nameValue; onEdit(group); confirmCancel.value = false; onExit() },
                onFail = { confirmCancel.value = false; onExit() })
            Button(
                onClick = {
                    if (editMade) confirmCancel.value = true
                    else onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                BackIcon(Modifier.size(24.dp))
                Text("Exit")
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
                Text("Delete group")
            }
        }
    }
}

@Composable
fun EditCalendar(calendar: Calendar, modifier: Modifier, onExit: () -> Unit, onEdit: (calendar: Calendar) -> Unit) {
    var editMade by remember { mutableStateOf(false) }

    val confirmCancel = remember { mutableStateOf(false) }
    if (confirmCancel.value) ConfirmDialog(text = "Are you sure you want to exit without saving your changes?", onSuccess = { onExit(); confirmCancel.value = false } , onFail = { confirmCancel.value = false })
    fun close() {
        if (editMade) confirmCancel.value = true
        else onExit()
    }

    var editName by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(16.dp))
        var nameValue by remember { mutableStateOf(calendar.name) }
        Box(Modifier.fillMaxWidth()) {
            CloseIconButton(
                Modifier.size(32.dp).align(Alignment.TopStart).offset((-8).dp, 0.dp),
                onClick = { close() }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { editName = true })) {
            if (!editName) {
                Text(text = nameValue, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(6.0f,TextUnitType.Em))
                EditIcon(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp))
            } else {
                LimitedTextField(
                    value = nameValue,
                    onValueChange = { v ->
                        editMade = (nameValue != v)
                        nameValue = v
                    },
                    onDismissRequest = {
                        editName = false
                    },
                    modifier = Modifier.align(Alignment.Center),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(6.0f,TextUnitType.Em))
                )
                SaveIconButton(Modifier.size(32.dp).align(Alignment.CenterEnd).offset((-8).dp, 0.dp), onClick = { editName = false })
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
                    ColorPicker(calendar.color, onEdit = { color -> calendar.color = color; editMade = true }, onExit = { editColorDialog.value = false })
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))

        var confirmCancel by remember { mutableStateOf(false) }
        if (confirmCancel) ConfirmDialog(
            text = "Do you want to save your changes?",
            onSuccess = { calendar.name = nameValue; onEdit(calendar); confirmCancel = false; onExit() },
            onFail = { confirmCancel = false; onExit() }
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    calendar.name = nameValue
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
                onClick = {
                    if (editMade) confirmCancel = true
                    else close()
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
fun DeleteIcon(modifier: Modifier = Modifier, color: Color? = null) {
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
            color ?: MaterialTheme.colorScheme.secondary
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
fun SaveIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier) {
        Icon(
            Icons.Default.Check,
            "Save",
        )
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp),
        Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Default.ExitToApp,
            "Back",
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
                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Outlook calendar (URL)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        """
                        1. Open Outlook on a computer
                        2. Navigate to the settings (Top right corner on web, in the view header in the app)  
                        3. Under "Calendar" go into "Shared calendars"
                        4. Then select your calendar under "Publish calendars" and "Can view when I'm busy" under "Select permissions"
                        5. Press publish and copy the second provided url
                        """.trimIndent(),

                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                    DialogActionRow(
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}
