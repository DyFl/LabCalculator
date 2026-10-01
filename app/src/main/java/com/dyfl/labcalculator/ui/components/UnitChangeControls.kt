package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun UnitChangeMessage(message: String?) {
    if (message != null) Text(message, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error)
}

/** Changing interpretation is always a visible reset, never a relabel of populated quantities. */
@Composable
internal fun UnitResetDialog(description: String, onReset: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Clear values and change units?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(description) } },
        confirmButton = { TextButton(onClick = onReset) { Text("Clear and change") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
