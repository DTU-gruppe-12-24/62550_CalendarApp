package com.group4.calendarapplication.views

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.group4.calendarapplication.MainActivity
import com.group4.calendarapplication.models.Group

@Composable
fun HomeView(groups: List<Group>, modifier: Modifier) {
    var currentGroup by rememberSaveable { mutableIntStateOf(-1) }

    if(currentGroup == -1) {
        GroupOverview(groups, { group -> currentGroup = group }, modifier)
    } else {
        EditGroup(groups[currentGroup], modifier)
    }
}

@Composable
fun GroupOverview(groups: List<Group>, onSelectGroup: (group: Int) -> Unit, modifier: Modifier) {
    val openCreateGroupDialog = remember { mutableStateOf(false) }

    if (openCreateGroupDialog.value) {
        AddGroupDialog( onDismissRequest = { openCreateGroupDialog.value = false } )
    }

    Column(modifier = modifier) {
        Text(text = "Manage calendar groups", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.size(16.dp))
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
            Button(onClick = { openCreateGroupDialog.value = true }, modifier = Modifier.align(Alignment.Center)) {
                Text("Add new group")
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        for (i in 0..<groups.size) {
            Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { onSelectGroup(i) })) {
                Text(text = groups[i].name)
            }
            Spacer(modifier = Modifier.size(8.dp))
        }
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
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        )  {
            TextField(
                value = name.value,
                onValueChange = { v -> name.value = v },
                placeholder = { Text("Name of group") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Button(onClick = {
                mainActivity.addGroup(Group(name.value, ArrayList()))
                onDismissRequest()
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Create group")
            }
        }
    }
}

@Composable
fun EditGroup(group: Group, modifier: Modifier) {
    Column(modifier = modifier) {
        Text("Editing group " + group.name)
    }
}