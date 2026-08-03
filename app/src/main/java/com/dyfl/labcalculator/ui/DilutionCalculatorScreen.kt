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
import androidx.compose.foundation.layout.width
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
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionCalculator
import com.dyfl.labcalculator.calculation.DilutionField
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.DilutionResult
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabFormCard
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabScreenBackground

@Composable
fun DilutionCalculatorScreen(modifier: Modifier = Modifier) {
    var stockConcentration by rememberSaveable { mutableStateOf("") }
    var finalConcentration by rememberSaveable { mutableStateOf("") }
    var finalSolutionVolume by rememberSaveable { mutableStateOf("") }
    var stockUnitName by rememberSaveable { mutableStateOf(ConcentrationUnit.PPM.name) }
    var finalUnitName by rememberSaveable { mutableStateOf(ConcentrationUnit.PPB.name) }
    var volumeFromStock by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<DilutionField, String>()) }

    val stockUnit = ConcentrationUnit.valueOf(stockUnitName)
    val finalUnit = ConcentrationUnit.valueOf(finalUnitName)

    fun clearResultAndError(field: DilutionField) {
        volumeFromStock = ""
        calculationStepsEncoded = ""
        errors = errors - field
    }

    fun calculate() {
        when (
            val result = DilutionCalculator.calculate(
                DilutionInput(
                    stockConcentration = stockConcentration,
                    stockUnit = stockUnit,
                    finalConcentration = finalConcentration,
                    finalUnit = finalUnit,
                    finalSolutionVolumeMl = finalSolutionVolume
                )
            )
        ) {
            is DilutionResult.Success -> {
                volumeFromStock = result.volumeFromStockMl
                calculationStepsEncoded = encodeCalculationSteps(result.calculationSteps)
                errors = emptyMap()
            }

            is DilutionResult.Invalid -> {
                volumeFromStock = ""
                calculationStepsEncoded = ""
                errors = result.errors.associate { it.field to it.message }
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
            text = "Standard / reagent dilution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )

        Spacer(modifier = Modifier.height(14.dp))

        EquationCard(stockUnit = stockUnit, finalUnit = finalUnit)

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = LabFormCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ConcentrationInput(
                    label = "Stock concentration (C₁)",
                    value = stockConcentration,
                    onValueChange = {
                        stockConcentration = it
                        clearResultAndError(DilutionField.STOCK_CONCENTRATION)
                    },
                    unit = stockUnit,
                    onUnitChange = {
                        stockUnitName = it.name
                        volumeFromStock = ""
                        calculationStepsEncoded = ""
                        errors = emptyMap()
                    },
                    error = errors[DilutionField.STOCK_CONCENTRATION]
                )

                ConcentrationInput(
                    label = "Final concentration (C₂)",
                    value = finalConcentration,
                    onValueChange = {
                        finalConcentration = it
                        clearResultAndError(DilutionField.FINAL_CONCENTRATION)
                    },
                    unit = finalUnit,
                    onUnitChange = {
                        finalUnitName = it.name
                        volumeFromStock = ""
                        calculationStepsEncoded = ""
                        errors = emptyMap()
                    },
                    error = errors[DilutionField.FINAL_CONCENTRATION]
                )

                Spacer(modifier = Modifier.height(18.dp))

                LabeledNumberInput(
                    label = "Final solution volume (V₂)",
                    value = finalSolutionVolume,
                    onValueChange = {
                        finalSolutionVolume = it
                        clearResultAndError(DilutionField.FINAL_SOLUTION_VOLUME)
                    },
                    suffix = "mL",
                    error = errors[DilutionField.FINAL_SOLUTION_VOLUME],
                    imeAction = ImeAction.Done
                )

                Spacer(modifier = Modifier.height(18.dp))

                ReadOnlyVolumeResult(value = volumeFromStock)

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
                            stockConcentration = ""
                            finalConcentration = ""
                            finalSolutionVolume = ""
                            stockUnitName = ConcentrationUnit.PPM.name
                            finalUnitName = ConcentrationUnit.PPB.name
                            volumeFromStock = ""
                            calculationStepsEncoded = ""
                            errors = emptyMap()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Clear all", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Repeating results show three repetitions followed by R (for example, 0.333R).",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EquationCard(
    stockUnit: ConcentrationUnit,
    finalUnit: ConcentrationUnit
) {
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
                text = "C₁V₁ = C₂V₂",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LabBlue
            )
            Text(
                text = "V₁ (mL) = [C₂ (${finalUnit.label}) × V₂ (mL)] ÷ C₁ (${stockUnit.label})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "1 PPM = 1,000 PPB. Units are converted before calculation.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ConcentrationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: ConcentrationUnit,
    onUnitChange: (ConcentrationUnit) -> Unit,
    error: String?
) {
    Column {
        FieldHeading(label)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            LabNumberTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                error = error
            )
            Spacer(modifier = Modifier.width(10.dp))
            LabDropdown(
                selected = unit,
                options = ConcentrationUnit.entries,
                buttonText = { it.label },
                menuText = {
                    when (it) {
                        ConcentrationUnit.PPM -> "PPM (parts per million)"
                        ConcentrationUnit.PPB -> "PPB (parts per billion)"
                    }
                },
                onSelected = onUnitChange,
                modifier = Modifier.width(106.dp)
            )
        }
    }
}

@Composable
private fun LabeledNumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    error: String?,
    imeAction: ImeAction
) {
    Column {
        FieldHeading(label)
        Spacer(modifier = Modifier.height(6.dp))
        LabNumberTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            suffix = suffix,
            error = error,
            imeAction = imeAction
        )
    }
}

@Composable
private fun ReadOnlyVolumeResult(value: String) {
    Column {
        FieldHeading("Volume from stock (V₁)")
        Spacer(modifier = Modifier.height(6.dp))
        LabNumberTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Result",
            suffix = "mL",
            readOnly = true,
            imeAction = ImeAction.None
        )
    }
}

@Composable
private fun FieldHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}
