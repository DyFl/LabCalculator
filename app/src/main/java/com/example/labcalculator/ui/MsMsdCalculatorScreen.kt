package com.example.labcalculator.ui

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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.labcalculator.calculation.ConcentrationUnit
import com.example.labcalculator.calculation.MsMsdCalculator
import com.example.labcalculator.calculation.MsMsdField
import com.example.labcalculator.calculation.MsMsdInput
import com.example.labcalculator.calculation.MsMsdResult
import com.example.labcalculator.ui.theme.LabBlue
import com.example.labcalculator.ui.theme.LabCalculatorTheme
import com.example.labcalculator.ui.theme.LabEquationCard
import com.example.labcalculator.ui.theme.LabError
import com.example.labcalculator.ui.theme.LabFormCard
import com.example.labcalculator.ui.theme.LabMutedText
import com.example.labcalculator.ui.theme.LabScreenBackground
import com.example.labcalculator.ui.theme.LabText

@Composable
fun MsMsdCalculatorScreen(modifier: Modifier = Modifier) {
    var rawSourceResult by rememberSaveable { mutableStateOf("") }
    var dilutionFactor by rememberSaveable { mutableStateOf("1") }
    var finalSpikeConcentration by rememberSaveable { mutableStateOf("") }
    var msResult by rememberSaveable { mutableStateOf("") }
    var msdResult by rememberSaveable { mutableStateOf("") }
    var concentrationUnitName by rememberSaveable {
        mutableStateOf(ConcentrationUnit.PPB.name)
    }

    var originalSourceConcentration by rememberSaveable { mutableStateOf("") }
    var msRecovery by rememberSaveable { mutableStateOf("") }
    var msdRecovery by rememberSaveable { mutableStateOf("") }
    var msMsdRpd by rememberSaveable { mutableStateOf("") }
    var calculationSectionsEncoded by rememberSaveable { mutableStateOf("") }
    var generalError by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldErrors by remember { mutableStateOf(emptyMap<MsMsdField, String>()) }
    val concentrationUnit = ConcentrationUnit.valueOf(concentrationUnitName)

    fun clearCalculatedValues() {
        originalSourceConcentration = ""
        msRecovery = ""
        msdRecovery = ""
        msMsdRpd = ""
        calculationSectionsEncoded = ""
    }

    fun inputChanged(field: MsMsdField) {
        clearCalculatedValues()
        generalError = null
        fieldErrors = fieldErrors - field
    }

    fun calculate() {
        when (
            val result = MsMsdCalculator.calculate(
                MsMsdInput(
                    rawSourceResult = rawSourceResult,
                    dilutionFactor = dilutionFactor,
                    finalSpikeConcentration = finalSpikeConcentration,
                    msResult = msResult,
                    msdResult = msdResult,
                    concentrationUnit = concentrationUnit
                )
            )
        ) {
            is MsMsdResult.Success -> {
                originalSourceConcentration = result.formattedOriginalSourceConcentration
                msRecovery = result.formattedMsRecovery
                msdRecovery = result.formattedMsdRecovery
                msMsdRpd = result.formattedMsMsdRpd
                calculationSectionsEncoded = encodeCalculationStepSections(
                    result.calculationSections.map { section ->
                        CalculationStepsSection(section.title, section.steps)
                    }
                )
                generalError = null
                fieldErrors = emptyMap()
            }

            is MsMsdResult.Invalid -> {
                clearCalculatedValues()
                generalError = null
                fieldErrors = result.errors.associate { it.field to it.message }
            }

            MsMsdResult.ZeroAverage -> {
                clearCalculatedValues()
                generalError = MsMsdResult.ZeroAverage.MESSAGE
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
            text = "Matrix Spike / Matrix Spike Duplicate",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabBlue,
            textAlign = TextAlign.Center
        )
        Text(
            text = "MS/MSD recovery and literal-result RPD",
            style = MaterialTheme.typography.bodyMedium,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))
        MsMsdEquationCard(concentrationUnit)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = LabFormCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Concentration unit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LabText
                )
                Text(
                    text = "This shared unit applies to the raw sample, spike, MS, and MSD values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabMutedText
                )
                Spacer(modifier = Modifier.height(6.dp))
                LabDropdown(
                    selected = concentrationUnit,
                    options = ConcentrationUnit.entries,
                    buttonText = { it.label },
                    menuText = { unit ->
                        when (unit) {
                            ConcentrationUnit.PPB -> "PPB (parts per billion)"
                            ConcentrationUnit.PPM -> "PPM (parts per million)"
                        }
                    },
                    onSelected = { selectedUnit ->
                        concentrationUnitName = selectedUnit.name
                        clearCalculatedValues()
                        generalError = null
                        fieldErrors = emptyMap()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))

                MsMsdInputField(
                    label = "Raw diluted source-sample result",
                    supportingText =
                        "Uncorrected source result before applying the sample dilution factor.",
                    value = rawSourceResult,
                    onValueChange = {
                        rawSourceResult = it
                        inputChanged(MsMsdField.RAW_SOURCE_RESULT)
                    },
                    error = fieldErrors[MsMsdField.RAW_SOURCE_RESULT],
                    suffix = concentrationUnit.label
                )

                MsMsdInputField(
                    label = "Sample dilution factor",
                    supportingText = "Decimal dilution factors are accepted; the value must exceed zero.",
                    value = dilutionFactor,
                    onValueChange = {
                        dilutionFactor = it
                        inputChanged(MsMsdField.DILUTION_FACTOR)
                    },
                    error = fieldErrors[MsMsdField.DILUTION_FACTOR]
                )

                MsMsdInputField(
                    label = "Final spike concentration added",
                    supportingText =
                        "Final concentration added to each diluted aliquot after sample dilution.",
                    value = finalSpikeConcentration,
                    onValueChange = {
                        finalSpikeConcentration = it
                        inputChanged(MsMsdField.FINAL_SPIKE_CONCENTRATION)
                    },
                    error = fieldErrors[MsMsdField.FINAL_SPIKE_CONCENTRATION],
                    suffix = concentrationUnit.label
                )

                MsMsdInputField(
                    label = "Literal MS result",
                    supportingText = "Measured result from the diluted-and-spiked MS aliquot.",
                    value = msResult,
                    onValueChange = {
                        msResult = it
                        inputChanged(MsMsdField.MS_RESULT)
                    },
                    error = fieldErrors[MsMsdField.MS_RESULT],
                    suffix = concentrationUnit.label
                )

                MsMsdInputField(
                    label = "Literal MSD result",
                    supportingText = "Measured result from the diluted-and-spiked MSD aliquot.",
                    value = msdResult,
                    onValueChange = {
                        msdResult = it
                        inputChanged(MsMsdField.MSD_RESULT)
                    },
                    error = fieldErrors[MsMsdField.MSD_RESULT],
                    suffix = concentrationUnit.label,
                    imeAction = ImeAction.Done
                )

                Text(
                    text = "All concentration values are interpreted as ${concentrationUnit.label}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LabBlue,
                    fontWeight = FontWeight.SemiBold
                )

                if (generalError != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = checkNotNull(generalError),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabError,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (originalSourceConcentration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LabBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MsMsdResultCard(
                        label = "Original source concentration",
                        value = originalSourceConcentration,
                        supportingText = "Raw source result × sample dilution factor"
                    )
                    MsMsdResultCard(label = "MS recovery", value = msRecovery)
                    MsMsdResultCard(label = "MSD recovery", value = msdRecovery)
                    MsMsdResultCard(label = "MS/MSD RPD", value = msMsdRpd)
                }

                if (calculationSectionsEncoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CalculationStepsCard(
                        sections = decodeCalculationStepSections(calculationSectionsEncoded)
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
                            rawSourceResult = ""
                            dilutionFactor = "1"
                            finalSpikeConcentration = ""
                            msResult = ""
                            msdResult = ""
                            concentrationUnitName = ConcentrationUnit.PPB.name
                            clearCalculatedValues()
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
            text = "No pass/fail decision is made. Recovery and RPD values are shown for analyst review.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MsMsdEquationCard(concentrationUnit: ConcentrationUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LabEquationCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Equations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LabBlue
            )
            Text(
                text = "All concentration terms use ${concentrationUnit.label}.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Original source = Raw source × Dilution factor",
                style = MaterialTheme.typography.bodyMedium,
                color = LabText
            )
            Text(
                text = "MS recovery (%) = ((MS − Raw source) ÷ Spike) × 100",
                style = MaterialTheme.typography.bodyMedium,
                color = LabText
            )
            Text(
                text = "MSD recovery (%) = ((MSD − Raw source) ÷ Spike) × 100",
                style = MaterialTheme.typography.bodyMedium,
                color = LabText
            )
            Text(
                text = "RPD (%) = |MS − MSD| ÷ ((MS + MSD) ÷ 2) × 100",
                style = MaterialTheme.typography.bodyMedium,
                color = LabText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dilution handling",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = LabBlue
            )
            Text(
                text = "The sample was diluted before the spike was added. Only the native source result is multiplied by the dilution factor; the spike, literal MS/MSD results, recoveries, and RPD are not.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
        }
    }
}

@Composable
private fun MsMsdInputField(
    label: String,
    supportingText: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    suffix: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = LabText
    )
    Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = LabMutedText
    )
    Spacer(modifier = Modifier.height(6.dp))
    LabNumberTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        suffix = suffix,
        error = error,
        imeAction = imeAction
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MsMsdResultCard(
    label: String,
    value: String,
    supportingText: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LabEquationCard)
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = LabMutedText
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LabBlue
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = LabMutedText
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000)
@Composable
private fun MsMsdCalculatorNarrowPreview() {
    LabCalculatorTheme {
        MsMsdCalculatorScreen()
    }
}
