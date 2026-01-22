package com.group4.calendarapplication.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogActionRow(
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    confirmEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DismissButton(onClick = onDismiss)

        if (onDelete != null) {
            DeleteButton(onClick = onDelete)
        }

        if (onConfirm != null) {
            SuccessButton(
                onClick = onConfirm,
                enabled = confirmEnabled
            )
        }
    }
}