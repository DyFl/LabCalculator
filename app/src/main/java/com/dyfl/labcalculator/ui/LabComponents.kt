package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabError
import com.dyfl.labcalculator.ui.theme.LabInputBackground
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabOutline
import com.dyfl.labcalculator.ui.theme.LabText

private const val CALCULATION_STEP_SEPARATOR = "\u001F"
private const val CALCULATION_SECTION_SEPARATOR = "\u001D"
private const val CALCULATION_SECTION_TITLE_SEPARATOR = "\u001E"

internal data class CalculationStepsSection(
    val title: String,
    val steps: List<String>
)

internal fun encodeCalculationSteps(steps: List<String>): String =
    steps.joinToString(CALCULATION_STEP_SEPARATOR)

internal fun decodeCalculationSteps(encodedSteps: String): List<String> =
    if (encodedSteps.isEmpty()) emptyList() else encodedSteps.split(CALCULATION_STEP_SEPARATOR)

internal fun encodeCalculationStepSections(
    sections: List<CalculationStepsSection>
): String = sections.joinToString(CALCULATION_SECTION_SEPARATOR) { section ->
    section.title + CALCULATION_SECTION_TITLE_SEPARATOR + encodeCalculationSteps(section.steps)
}

internal fun decodeCalculationStepSections(
    encodedSections: String
): List<CalculationStepsSection> {
    if (encodedSections.isEmpty()) return emptyList()

    return encodedSections.split(CALCULATION_SECTION_SEPARATOR).map { encodedSection ->
        val parts = encodedSection.split(CALCULATION_SECTION_TITLE_SEPARATOR, limit = 2)
        CalculationStepsSection(
            title = parts.first(),
            steps = decodeCalculationSteps(parts.getOrElse(1) { "" })
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
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        readOnly = readOnly,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        suffix = suffix?.let { unit ->
            {
                Text(
                    text = unit,
                    fontWeight = FontWeight.Bold,
                    color = LabBlue
                )
            }
        },
        isError = error != null,
        supportingText = error?.let { message ->
            {
                Text(
                    text = message,
                    color = LabError
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = LabMutedText,
            errorTextColor = Color.Black,
            cursorColor = LabBlue,
            errorCursorColor = LabError,
            focusedContainerColor = LabInputBackground,
            unfocusedContainerColor = LabInputBackground,
            disabledContainerColor = LabInputBackground,
            errorContainerColor = LabInputBackground,
            focusedBorderColor = LabBlue,
            unfocusedBorderColor = LabOutline,
            errorBorderColor = LabError,
            focusedLabelColor = LabBlue,
            unfocusedLabelColor = LabMutedText,
            disabledLabelColor = LabMutedText,
            errorLabelColor = LabError,
            focusedPlaceholderColor = LabMutedText,
            unfocusedPlaceholderColor = LabMutedText,
            disabledPlaceholderColor = LabMutedText,
            errorPlaceholderColor = LabMutedText,
            focusedSupportingTextColor = LabMutedText,
            unfocusedSupportingTextColor = LabMutedText,
            disabledSupportingTextColor = LabMutedText,
            errorSupportingTextColor = LabError,
            focusedSuffixColor = LabBlue,
            unfocusedSuffixColor = LabBlue,
            disabledSuffixColor = LabMutedText,
            errorSuffixColor = LabBlue
        )
    )
}

@Composable
internal fun <T> LabDropdown(
    selected: T,
    options: List<T>,
    buttonText: (T) -> String,
    menuText: (T) -> String = buttonText,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = LabInputBackground,
                contentColor = LabBlue
            )
        ) {
            Text(
                text = "${buttonText(selected)} ▾",
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = LabInputBackground
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = menuText(option), color = LabText) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Shared selectable presentation for calculation work from any calculator. */
@Composable
internal fun CalculationStepsCard(
    steps: List<String> = emptyList(),
    sections: List<CalculationStepsSection> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty() && sections.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LabEquationCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Calculation Steps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LabBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Column {
                    if (sections.isEmpty()) {
                        NumberedCalculationSteps(steps)
                    } else {
                        sections.forEachIndexed { sectionIndex, section ->
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LabBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            NumberedCalculationSteps(section.steps)
                            if (sectionIndex != sections.lastIndex) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberedCalculationSteps(steps: List<String>) {
    Column {
        steps.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                style = MaterialTheme.typography.bodyMedium,
                color = LabText
            )
            if (index != steps.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
