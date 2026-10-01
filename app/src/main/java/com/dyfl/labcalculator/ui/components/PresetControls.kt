package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.DeletePresetResult
import com.dyfl.labcalculator.presets.MAX_PRESET_NAME_LENGTH
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.presets.SavePresetResult
import com.dyfl.labcalculator.presets.PresetLoadResult
import com.dyfl.labcalculator.presets.message
import com.dyfl.labcalculator.presets.validationError
import com.dyfl.labcalculator.ui.theme.LabError
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabControlShape
import com.dyfl.labcalculator.ui.theme.LabFieldSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val LocalPresetStore = staticCompositionLocalOf<CalculatorPresetStore?> { null }

@Composable
internal fun PresetControls(
    kind: PresetKind,
    description: String,
    currentSettings: () -> PresetSettings,
    onApply: (PresetSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val providedStore = LocalPresetStore.current
    val inspectionMode = LocalInspectionMode.current
    val store = remember(context, providedStore, inspectionMode) {
        providedStore ?: if (inspectionMode) null else CalculatorPresetStore(context)
    }
    var loadResult by remember(store, kind) {
        mutableStateOf(store?.load(kind) ?: PresetLoadResult.Empty)
    }
    val presets = loadResult.presets
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showLoadDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsSnapshot = currentSettings()
    val presetButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LaunchedEffect(settingsSnapshot) { error = null }
    DisposableEffect(store, kind) {
        val unsubscribe = store?.let { observedStore ->
            observedStore.observe(kind) { loadResult = observedStore.load(kind) }
        }
        onDispose { unsubscribe?.invoke() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    error = null
                    loadResult = store?.load(kind) ?: PresetLoadResult.Empty
                    showLoadDialog = true
                },
                enabled = store != null && presets.isNotEmpty(),
                colors = presetButtonColors,
                shape = LabControlShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(LabIcons.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load preset", modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = {
                    error = currentSettings().validationError()
                    if (error == null) {
                        name = ""
                        nameError = null
                        showSaveDialog = true
                    }
                },
                enabled = store != null,
                colors = presetButtonColors,
                shape = LabControlShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(LabIcons.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save preset", modifier = Modifier.weight(1f))
            }
        }
        loadResult.message?.let { message ->
            Text(message, modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = LabMutedText)
        }
        if (error != null && !showLoadDialog) {
            Text(checkNotNull(error), color = LabError, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(LabFieldSpacing))
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showSaveDialog = false },
            title = { Text("Save preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = LabMutedText)
                    OutlinedTextField(
                        shape = LabControlShape,
                        value = name,
                        onValueChange = {
                            name = it.take(MAX_PRESET_NAME_LENGTH)
                            nameError = null
                        },
                        label = { Text("Preset name") },
                        singleLine = true,
                        enabled = !busy,
                        isError = nameError != null,
                        supportingText = { nameError?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = LabError)
                        } }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    shape = LabControlShape,
                    enabled = !busy && store != null,
                    onClick = {
                        val settings = currentSettings()
                        busy = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                checkNotNull(store).save(name, settings)
                            }
                            busy = false
                            when (result) {
                                is SavePresetResult.Saved -> {
                                    loadResult = store?.load(kind) ?: PresetLoadResult.Empty
                                    showSaveDialog = false
                                    error = null
                                }
                                is SavePresetResult.Invalid -> nameError = result.message
                            }
                        }
                    }
                ) { Text(if (busy) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(shape = LabControlShape, enabled = !busy,
                    onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showLoadDialog = false },
            title = { Text("Saved presets") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())) {
                    loadResult.message?.let { Text(it) }
                    presets.forEach { preset ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                shape = LabControlShape,
                                enabled = !busy,
                                onClick = {
                                    onApply(preset.settings)
                                    showLoadDialog = false
                                    error = null
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(preset.name) }
                            TextButton(
                                shape = LabControlShape,
                                enabled = !busy,
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            checkNotNull(store).delete(preset)
                                        }
                                        busy = false
                                        when (result) {
                                            DeletePresetResult.Deleted -> {
                                                loadResult = checkNotNull(store).load(kind)
                                                error = null
                                            }
                                            is DeletePresetResult.Invalid -> error = result.message
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Delete ${preset.name}"
                                }
                            ) { Text("Delete") }
                        }
                    }
                    error?.let { Text(it, color = LabError) }
                }
            },
            confirmButton = {
                TextButton(shape = LabControlShape, enabled = !busy,
                    onClick = { showLoadDialog = false }) { Text("Close") }
            }
        )
    }
}
