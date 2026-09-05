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
import com.dyfl.labcalculator.calculation.MolarityMassCalculator
import com.dyfl.labcalculator.calculation.MolarityMassField
import com.dyfl.labcalculator.calculation.MolarityMassInput
import com.dyfl.labcalculator.calculation.MolarityMassResult
import com.dyfl.labcalculator.calculation.MolarityVolumeUnit
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabFormCard
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabScreenBackground

@Composable
fun MolarityMassCalculatorScreen(modifier: Modifier = Modifier) {
    var molarity by rememberSaveable { mutableStateOf("") }
    var finalSolutionVolume by rememberSaveable { mutableStateOf("") }
    var volumeUnitName by rememberSaveable { mutableStateOf(MolarityVolumeUnit.MILLILITER.name) }
    var formulaWeight by rememberSaveable { mutableStateOf("") }
    var requiredMass by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<MolarityMassField, String>()) }
    val volumeUnit = MolarityVolumeUnit.valueOf(volumeUnitName)

    fun clearResultAndError(field: MolarityMassField) {
        requiredMass = ""
        calculationStepsEncoded = ""
        errors = errors - field
    }

    fun calculate() {
        when (val result = MolarityMassCalculator.calculate(
            MolarityMassInput(molarity, finalSolutionVolume, volumeUnit, formulaWeight)
        )) {
            is MolarityMassResult.Success -> {
                requiredMass = result.formattedMassGrams
                calculationStepsEncoded = encodeCalculationSteps(result.calculationSteps)
                errors = emptyMap()
            }
            is MolarityMassResult.Invalid -> {
                requiredMass = ""
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
            text = "Molarity / Dry Chemical Mass",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LabEquationCard)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "m = M × V × FW",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LabBlue
                )
                Text(
                    text = "Mass (g) = molarity (mol/L) × volume (L) × formula weight (g/mol)",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Use the final solution volume. 1,000 mL = 1 L.",
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
                FieldHeading("Desired Molarity")
                LabNumberTextField(
                    value = molarity,
                    onValueChange = {
                        molarity = it
                        clearResultAndError(MolarityMassField.MOLARITY)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = "M (mol/L)",
                    error = errors[MolarityMassField.MOLARITY]
                )
                Spacer(modifier = Modifier.height(18.dp))
                FieldHeading("Final Solution Volume")
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    LabNumberTextField(
                        value = finalSolutionVolume,
                        onValueChange = {
                            finalSolutionVolume = it
                            clearResultAndError(MolarityMassField.FINAL_SOLUTION_VOLUME)
                        },
                        modifier = Modifier.weight(1f),
                        error = errors[MolarityMassField.FINAL_SOLUTION_VOLUME]
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    LabDropdown(
                        selected = volumeUnit,
                        options = MolarityVolumeUnit.entries,
                        buttonText = { it.label },
                        onSelected = {
                            volumeUnitName = it.name
                            clearResultAndError(MolarityMassField.FINAL_SOLUTION_VOLUME)
                        },
                        modifier = Modifier.width(106.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                FieldHeading("Formula Weight")
                LabNumberTextField(
                    value = formulaWeight,
                    onValueChange = {
                        formulaWeight = it
                        clearResultAndError(MolarityMassField.FORMULA_WEIGHT)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = "g/mol",
                    error = errors[MolarityMassField.FORMULA_WEIGHT],
                    imeAction = ImeAction.Done
                )
                Text(
                    text = "Enter the formula weight from the bottle or reagent documentation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabMutedText
                )
                Spacer(modifier = Modifier.height(18.dp))
                FieldHeading("Required Mass")
                LabNumberTextField(
                    value = requiredMass,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Result",
                    suffix = "g",
                    readOnly = true,
                    imeAction = ImeAction.None
                )
                if (calculationStepsEncoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CalculationStepsCard(steps = decodeCalculationSteps(calculationStepsEncoded))
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = ::calculate,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LabBlue)
                    ) {
                        Text("Calculate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            molarity = ""
                            finalSolutionVolume = ""
                            formulaWeight = ""
                            volumeUnitName = MolarityVolumeUnit.MILLILITER.name
                            requiredMass = ""
                            calculationStepsEncoded = ""
                            errors = emptyMap()
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Clear", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Only the final mass is rounded, to 5 significant figures. Results are in grams.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FieldHeading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
}
