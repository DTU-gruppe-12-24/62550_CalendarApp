package com.group4.calendarapplication.views

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.MainActivity
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.views.components.DeleteButton
import com.group4.calendarapplication.views.components.DialogActionRow
import com.group4.calendarapplication.views.components.SuccessButton
import com.group4.calendarapplication.views.components.DismissButton

@Composable
fun HomeView(groups: List<Group>, modifier: Modifier) {
    val mainActivity = LocalActivity.current as MainActivity
    var currentGroup by rememberSaveable { mutableIntStateOf(-1) }

    if(currentGroup == -1 || groups.isEmpty()) {
        GroupOverview(groups, { group -> currentGroup = group }, modifier)
    } else {
        val group = groups[currentGroup]

        BackHandler(enabled = true) {
            currentGroup = -1
        }

        EditGroup(
            group = group,
            modifier = modifier,
            onExit = { currentGroup = -1 },
            onEdit = { updatedGroup ->
                mainActivity.updateGroup(currentGroup, updatedGroup)
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
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = { onSelectGroup(i) })) {
                    Text(text = groups[i].name, modifier = Modifier.align(Alignment.Center))
                    EditIcon(Modifier
                        .size(32.dp)
                        .align(Alignment.CenterEnd)
                        .offset((-8).dp, 0.dp))
                }
            })
        }
        items.add({
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(CornerSize(8.dp))
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
    var editName by remember { mutableStateOf(false) }
    var nameValue by remember(group) { mutableStateOf(group.name) }
    val editCalendarIndex = remember { mutableIntStateOf(-1) }
    val errorMessage = remember { mutableStateOf(null as String?) }
    val openCreateCalendarDialog = remember { mutableStateOf(false) }
    val confirmGroupDelete = remember { mutableStateOf(false) }
    val calendarList = remember(group) { group.calendars.toMutableStateList() }

    if (editCalendarIndex.intValue >= 0) {
        EditCalendar(
            calendar = calendarList[editCalendarIndex.intValue],
            onExit = { editCalendarIndex.intValue = -1 },
            onEdit = { calendar ->
                calendarList[editCalendarIndex.intValue] = calendar
                group.calendars = ArrayList(calendarList)
                onEdit(group)
            },
            onDelete = {
                calendarList.removeAt(editCalendarIndex.intValue)
                group.calendars = ArrayList(calendarList)
                onEdit(group)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp)
        ) {
            Spacer(modifier = Modifier.size(16.dp))

            // Edit group name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(64.dp)
                    .clickable(enabled = !editName) { editName = true },
                contentAlignment = Alignment.Center
            ) {
                if (!editName) {
                    Text(
                        text = nameValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = TextUnit(7.0f, TextUnitType.Em),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    EditIcon(
                        Modifier
                            .size(28.dp)
                            .align(Alignment.CenterEnd)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuccessButton(
                            onClick = {
                                group.name = nameValue
                                onEdit(group)
                                editName = false
                                editMade = false
                            },
                            modifier = Modifier.size(44.dp)
                        )

                        LimitedTextField(
                            value = nameValue,
                            onValueChange = { v ->
                                editMade = (group.name != v)
                                nameValue = v
                            },
                            onDismissRequest = { editName = false },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = TextUnit(
                                    6.5f,
                                    TextUnitType.Em
                                ), // Scaled down for edit mode
                                textAlign = TextAlign.Center
                            )
                        )

                        DismissButton(
                            onClick = {
                                nameValue = group.name
                                editName = false
                                editMade = false
                            },
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(32.dp))

            // Calendars header
            Text(
                text = "Calendars",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.primary,
                fontSize = TextUnit(5.0f, TextUnitType.Em)
            )
            Spacer(modifier = Modifier.size(10.dp))

            if (errorMessage.value != null) ErrorMessage(
                errorMessage.value ?: "Unknown error"
            ) { errorMessage.value = null }

            // Add calendars to list
            val calendarItems = ArrayList<@Composable () -> Unit>()
            for (i in calendarList.indices) {
                calendarItems.add({
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clickable { editCalendarIndex.intValue = i },
                        Arrangement.SpaceBetween
                    ) {
                        CalendarLegend(
                            calendarList[i],
                            Modifier.align(Alignment.CenterVertically)
                        )
                        Box(
                            Modifier
                                .align(Alignment.CenterVertically)
                                .clickable { editCalendarIndex.intValue = i }) {
                            EditIcon(Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(0.66f))
                        }
                    }
                })
            }

            if (openCreateCalendarDialog.value) {
                AddCalendarDialog(
                    onDismissRequest = { openCreateCalendarDialog.value = false },
                    addCalender = { cal ->
                        calendarList.add(cal)
                        group.calendars = ArrayList(calendarList)
                        onEdit(group)
                    }
                )
            }

            // Add calendar button to list
            calendarItems.add({
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            RoundedCornerShape(CornerSize(8.dp))
                        )
                        .clickable(
                            onClick = { openCreateCalendarDialog.value = true }
                        ),
                ) {
                    Text("Add new calendar", Modifier.align(Alignment.Center))
                }
            })

            // List of calendars and add calendar button
            ItemList(calendarItems, Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f))

            }
        Row(
            modifier = Modifier
                .align(BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Delete button
            DeleteButton(onClick = { confirmGroupDelete.value = true })

            // Exit button
            SuccessButton(onClick = { onExit() })

            if (confirmGroupDelete.value) {
                ConfirmDialog(
                    text = "Delete group \"${group.name}\"?\nThis will delete all associated calendars and cannot be undone.",
                    onSuccess = {
                        confirmGroupDelete.value = false
                        deleteGroup()
                    },
                    onFail = { confirmGroupDelete.value = false }
                )
            }
        }
    }
}

@Composable
fun AddCalendarDialog(
    onDismissRequest: () -> Unit,
    addCalender: (cal: Calendar) -> Unit,
) {
    val showHelp = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf(null as String?)}

    if (errorMessage.value != null) {
        ErrorMessage(
            message = errorMessage.value!!,
            onDismiss = { errorMessage.value = null }
        )
    }

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

                UrlCalendarImport(
                    onResult = { calendar ->
                        if (calendar != null) addCalender(calendar)
                        onDismissRequest()
                    },
                    onError = { msg -> errorMessage.value = msg },
                    onClose = onDismissRequest
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Import Section
                FileCalendarImport(
                    onResult = { calendar ->
                        if (calendar != null) addCalender(calendar)
                        onDismissRequest()
                    },
                    onError = { msg -> errorMessage.value = msg }
                )

                Spacer(modifier = Modifier.height(24.dp))

                DialogActionRow(
                    onDismiss = onDismissRequest
                )
            }
        }
    }
}

@Composable
fun EditCalendar(
    calendar: Calendar,
    onExit: () -> Unit,
    onEdit: (calendar: Calendar) -> Unit,
    onDelete: () -> Unit
) {
    var nameValue by remember { mutableStateOf(calendar.name) }
    var editMade by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Confirmations
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUnsavedConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ConfirmDialog(
            text = "Delete \"${calendar.name}\"? This cannot be undone.",
            onSuccess = {
                showDeleteConfirm = false
                onDelete()
                onExit()
            },
            onFail = { showDeleteConfirm = false }
        )
    }

    if (showUnsavedConfirm) {
        ConfirmDialog(
            text = "Exit without saving changes?",
            onSuccess = {
                showUnsavedConfirm = false
                onExit()
            },
            onFail = { showUnsavedConfirm = false }
        )
    }

    Dialog(onDismissRequest = { if (editMade) showUnsavedConfirm = true else onExit() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Edit Calendar", style = MaterialTheme.typography.headlineSmall)

                Spacer(Modifier.height(20.dp))

                // Name Input
                LimitedTextField(
                    value = nameValue,
                    onValueChange = {
                        nameValue = it
                        editMade = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = TextUnit(6.0f, TextUnitType.Em),
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(24.dp))

                // Color Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp), // Vertical padding for breathing room
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp) // Increased size to match your FABs/Buttons
                            .background(calendar.color, CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { showColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Using a standard Icon here to ensure scaling works as expected
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Color",
                            modifier = Modifier.size(24.dp),
                            // Automatic contrast check
                            tint = if (calendar.color.luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                }

                if (showColorPicker) {
                    Dialog(onDismissRequest = { showColorPicker = false }) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            ColorPicker(
                                calendar.color,
                                onEdit = { color ->
                                    calendar.color = color
                                    editMade = true
                                },
                                onExit = { showColorPicker = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                DialogActionRow(
                    onDismiss = { if (editMade) showUnsavedConfirm = true else onExit() },
                    onDelete = { showDeleteConfirm = true },
                    onConfirm = {
                        // Create a new object instead of modifying the old one to get compose to notice the change
                        val updatedCalendar = Calendar(
                            name = nameValue,
                            color = calendar.color,
                            dates = calendar.dates
                        )
                        onEdit(updatedCalendar)
                        onExit()
                    }
                )
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
fun ConfirmDialog(text: String, onSuccess: () -> Unit, onFail: () -> Unit) {
    Dialog(onDismissRequest = { onFail() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                DialogActionRow(
                    onDismiss = onFail,
                    onDelete = onSuccess
                )
            }
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
