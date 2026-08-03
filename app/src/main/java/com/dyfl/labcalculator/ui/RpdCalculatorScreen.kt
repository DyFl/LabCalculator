package com.dyfl.labcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyfl.labcalculator.calculation.RpdCalculator
import com.dyfl.labcalculator.calculation.RpdField
import com.dyfl.labcalculator.calculation.RpdResult
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabFormCard
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabScreenBackground

@Composable
fun RpdCalculatorScreen(modifier: Modifier = Modifier) {
    var originalResult by rememberSaveable { mutableStateOf("") }
    var replicateResult by rememberSaveable { mutableStateOf("") }
    var relativePercentDifference by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var generalError by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldErrors by remember { mutableStateOf(emptyMap<RpdField, String>()) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LabScreenBackground)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Relative Percent Difference",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )
        Text(
            text = "Compare a sample with its replicate",
            style = MaterialTheme.typography.bodyMedium,
            color = LabMutedText
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LabEquationCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Both results must use the same units.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabMutedText,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = LabFormCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RpdInput(
                    label = "Original Sample Result",
                    value = originalResult,
                    onValueChange = {
                        originalResult = it
                        clearResultAndError(RpdField.ORIGINAL_SAMPLE)
                    },
                    error = fieldErrors[RpdField.ORIGINAL_SAMPLE]
                )

                Spacer(modifier = Modifier.height(18.dp))

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

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Relative Percent Difference",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LabNumberTextField(
                    value = relativePercentDifference,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Result",
                    error = generalError,
                    readOnly = true,
                    imeAction = ImeAction.None
                )

                if (calculationStepsEncoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CalculationStepsCard(
                        steps = decodeCalculationSteps(calculationStepsEncoded)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = ::calculate,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LabBlue)
                    ) {
                        Text("Calculate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            originalResult = ""
                            replicateResult = ""
                            relativePercentDifference = ""
                            calculationStepsEncoded = ""
                            generalError = null
                            fieldErrors = emptyMap()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Clear", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Only the final percentage is rounded, to two decimal places.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
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
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    LabNumberTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        error = error,
        imeAction = imeAction
    )
}
