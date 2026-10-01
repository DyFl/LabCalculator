package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.RpdCalculator
import com.dyfl.labcalculator.calculation.RpdField
import com.dyfl.labcalculator.calculation.RpdResult
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabError
import com.dyfl.labcalculator.ui.theme.LabMutedText

@Composable
fun RpdCalculatorScreen(modifier: Modifier = Modifier) {
    var originalResult by rememberSaveable { mutableStateOf("") }
    var replicateResult by rememberSaveable { mutableStateOf("") }
    var relativePercentDifference by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var generalError by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldErrors by rememberSaveable { mutableStateOf(emptyMap<RpdField, String>()) }

    fun clearResultAndError(field: RpdField) {
        relativePercentDifference = ""
        calculationStepsEncoded = ""
        generalError = null
        fieldErrors = fieldErrors - field
    }

    fun calculate() {
        when (val result = RpdCalculator.calculate(originalResult, replicateResult)) {
            is RpdResult.Success -> {
                relativePercentDifference = result.formattedPercent
                calculationStepsEncoded = encodeCalculationSteps(result.calculationSteps)
                generalError = null
                fieldErrors = emptyMap()
            }

            is RpdResult.Invalid -> {
                relativePercentDifference = ""
                calculationStepsEncoded = ""
                generalError = null
                fieldErrors = result.errors.associate { it.field to it.message }
            }

            RpdResult.ZeroAverage -> {
                relativePercentDifference = ""
                calculationStepsEncoded = ""
                generalError = RpdResult.ZeroAverage.MESSAGE
                fieldErrors = emptyMap()
            }
        }
    }

    LabScreen(modifier = modifier, onCalculate = ::calculate) {
        LabScreenHeading("Relative Percent Difference",
            subtitle = "Use results with the same units and dilution basis.")

        Spacer(modifier = Modifier.height(14.dp))

        LabFormulaCard {
            Text(
                text = "RPD (%) = |Original − Replicate|",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LabBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = "÷ |(Original + Replicate) ÷ 2| × 100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LabBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Both results must use the same units and dilution basis. Signed values use an absolute average; check your SOP for near-zero or non-detect results.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Only the final percentage is rounded, to two decimal places.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LabInputsCard {
            RpdInput(
                label = "Original Sample Result",
                value = originalResult,
                onValueChange = {
                    originalResult = it
                    clearResultAndError(RpdField.ORIGINAL_SAMPLE)
                },
                error = fieldErrors[RpdField.ORIGINAL_SAMPLE]
            )

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))

            RpdInput(
                label = "Replicate Sample Result",
                value = replicateResult,
                onValueChange = {
                    replicateResult = it
                    clearResultAndError(RpdField.REPLICATE_SAMPLE)
                },
                error = fieldErrors[RpdField.REPLICATE_SAMPLE],
                imeAction = ImeAction.Done
            )

            generalError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = LabError, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabCalculateActions(onClear = {
                originalResult = ""
                replicateResult = ""
                relativePercentDifference = ""
                calculationStepsEncoded = ""
                generalError = null
                fieldErrors = emptyMap()
            })
        }

        if (relativePercentDifference.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabResultCard(label = "Relative Percent Difference", value = relativePercentDifference)
        }

        if (calculationStepsEncoded.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CalculationStepsCard(
                steps = decodeCalculationSteps(calculationStepsEncoded)
            )
        }
    }
}

@Composable
private fun RpdInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    imeAction: ImeAction = ImeAction.Next
) {
    LabNumberTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        error = error,
        imeAction = imeAction
    )
}
