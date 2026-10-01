package com.dyfl.labcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.MAX_NUMBER_INPUT_LENGTH
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabControlShape
import com.dyfl.labcalculator.ui.theme.LabError
import com.dyfl.labcalculator.ui.theme.LabInputBackground
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabText
import com.dyfl.labcalculator.ui.theme.LabTouchTarget

private const val REJECTED_NUMBER_INPUT = "Input too long"

@Composable
internal fun LabScreenHeading(title: String, subtitle: String? = null) {
    Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = LabBlue)
    if (subtitle != null) {
        Text(subtitle, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
            color = LabMutedText)
    }
}

/** All input types share one outline and a full-width supporting-text slot. */
@Composable
internal fun UnitField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    unitControl: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    error: String? = null,
    supportingText: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    fieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().semantics { contentDescription = label },
            // Let long labels wrap and the outline grow at larger font scales.
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            placeholder = placeholder?.let { text -> { Text(text) } },
            trailingIcon = unitControl ?: unit?.let { suffix ->
                {
                    Text(suffix, modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            readOnly = readOnly,
            singleLine = singleLine,
            shape = LabControlShape,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LabText, textAlign = TextAlign.Start),
            isError = error != null,
            supportingText = (error ?: supportingText)?.let { message ->
                { Text(message, style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) LabError else MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LabInputBackground,
                unfocusedContainerColor = LabInputBackground,
                disabledContainerColor = LabInputBackground,
                errorContainerColor = LabInputBackground
            )
        )
    }
}

@Composable
internal fun LabNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Value",
    placeholder: String? = null,
    suffix: String? = null,
    error: String? = null,
    supportingText: String? = null,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    unitControl: @Composable (() -> Unit)? = null
) {
    val calculate = LocalCalculate.current
    val focusManager = LocalFocusManager.current
    val fields = LocalNumberFieldFocus.current
    val focusRequester = remember { FocusRequester() }
    DisposableEffect(fields, readOnly) {
        if (!readOnly) fields?.add(focusRequester)
        onDispose { fields?.remove(focusRequester) }
    }
    val displayedError = if (value == REJECTED_NUMBER_INPUT) {
        "Input was not accepted. Use at most $MAX_NUMBER_INPUT_LENGTH characters."
    } else error
    UnitField(
        value = value,
        onValueChange = { proposed ->
            // Preserve the existing whole-edit rejection; never truncate a number.
            onValueChange(if (proposed.length > MAX_NUMBER_INPUT_LENGTH) REJECTED_NUMBER_INPUT else proposed)
        },
        label = label, modifier = modifier, unit = suffix, unitControl = unitControl,
        placeholder = placeholder, error = displayedError,
        supportingText = supportingText, readOnly = readOnly,
        fieldModifier = Modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = {
                val next = fields?.let { it.getOrNull(it.indexOf(focusRequester) + 1) }
                if (next != null) next.requestFocus() else focusManager.moveFocus(FocusDirection.Next)
            },
            onDone = { calculate() }
        )
    )
}

@Composable
internal fun <T> LabNumberWithUnit(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: T,
    options: List<T>,
    unitText: (T) -> String,
    onUnitChange: (T) -> Unit,
    menuText: (T) -> String = unitText,
    error: String? = null,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    LabNumberTextField(value, onValueChange, label = label, error = error,
        supportingText = supportingText, imeAction = imeAction,
        unitControl = {
            Box {
                Row(
                    modifier = Modifier.clickable(role = Role.Button) { expanded = !expanded }
                        .semantics {
                            contentDescription = "Unit for $label"
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        }.sizeIn(minWidth = LabTouchTarget, minHeight = LabTouchTarget)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(unitText(unit), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(LabIcons.Chevron, contentDescription = null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    containerColor = LabInputBackground) {
                    options.forEach { option ->
                        DropdownMenuItem(text = { Text(menuText(option)) }, onClick = {
                            onUnitChange(option)
                            expanded = false
                        })
                    }
                }
            }
        })
}

/** Read-only selectors use the same field outline, with a floating label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> LabDropdown(
    label: String,
    selected: T,
    options: List<T>,
    buttonText: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    menuText: (T) -> String = buttonText,
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        UnitField(value = buttonText(selected), onValueChange = {}, label = label,
            readOnly = true, singleLine = false,
            fieldModifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            unitControl = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = supportingText)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            containerColor = LabInputBackground) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(menuText(option)) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
internal fun LabConcentrationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: ConcentrationUnit,
    onUnitChange: (ConcentrationUnit) -> Unit,
    error: String?,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next
) = LabNumberWithUnit(label, value, onValueChange, unit, ConcentrationUnit.entries,
    unitText = { it.label }, onUnitChange = onUnitChange,
    menuText = { it.description }, error = error, supportingText = supportingText, imeAction = imeAction)

@Composable
internal fun LabInfoRow(text: String) {
    val density = LocalDensity.current
    // Reserve icon space on the first line only, leaving the full width for wrapping.
    Text(buildAnnotatedString { appendInlineContent("info"); append(text) },
        modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        inlineContent = mapOf("info" to InlineTextContent(
            Placeholder(with(density) { 22.dp.toSp() }, with(density) { 18.dp.toSp() },
                PlaceholderVerticalAlign.TextCenter)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                Icon(LabIcons.Info, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }))
}

@Composable
internal fun LabSwapUnitsButton(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(LabTouchTarget),
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )) {
            Icon(LabIcons.SwapVert, contentDescription = "Swap units")
        }
    }
}
