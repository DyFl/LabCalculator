package com.dyfl.labcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.MsMsdCalculator
import com.dyfl.labcalculator.calculation.MsMsdField
import com.dyfl.labcalculator.calculation.MsMsdInput
import com.dyfl.labcalculator.calculation.MsMsdResult
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabFormCard
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabScreenBackground
import com.dyfl.labcalculator.ui.theme.LabText

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
    var rpdUnavailableReason by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldErrors by rememberSaveable { mutableStateOf(emptyMap<MsMsdField, String>()) }
    val concentrationUnit = ConcentrationUnit.valueOf(concentrationUnitName)

    fun clearCalculatedValues() {
        originalSourceConcentration = ""
        msRecovery = ""
        msdRecovery = ""
        msMsdRpd = ""
        rpdUnavailableReason = null
        calculationSectionsEncoded = ""
    }

    fun inputChanged(field: MsMsdField) {
        clearCalculatedValues()
        fieldErrors = fieldErrors - field
    }

    fun nextSample() {
        rawSourceResult = ""
        msResult = ""
        msdResult = ""
        clearCalculatedValues()
        fieldErrors = emptyMap()
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
                rpdUnavailableReason = result.rpdUnavailableReason
                fieldErrors = emptyMap()
            }

            is MsMsdResult.Invalid -> {
                clearCalculatedValues()
                fieldErrors = result.errors.associate { it.field to it.message }
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
                PresetControls(
                    kind = PresetKind.MS_MSD,
                    description = "Save units, dilution factor and spike concentration for later samples.",
                    currentSettings = {
                        PresetSettings.MsMsd(dilutionFactor, finalSpikeConcentration, concentrationUnit)
                    },
                    onApply = { settings ->
                        val preparation = settings as PresetSettings.MsMsd
                        dilutionFactor = preparation.dilutionFactor
                        finalSpikeConcentration = preparation.finalSpikeConcentration
                        concentrationUnitName = preparation.concentrationUnit.name
                        nextSample()
                    }
                )
                Text(
                    text = "Concentration unit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LabText
                )
                Text(
                    text = "This shared unit labels all concentrations; changing it does not convert entered numbers.",
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
                    supportingText = "Uncorrected measured MS result, on the same dilution basis as the raw source. Do not enter a dilution-corrected result.",
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
                    supportingText = "Uncorrected measured MSD result, on the same dilution basis as the raw source. Do not enter a dilution-corrected result.",
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

                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = ::calculate,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LabBlue)
                    ) {
                        Text("Calculate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = ::nextSample,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) {
                        Text("Next sample", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = {
                        dilutionFactor = "1"
                        finalSpikeConcentration = ""
                        concentrationUnitName = ConcentrationUnit.PPB.name
                        nextSample()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clear all") }

                if (originalSourceConcentration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    LabResultCard(
                        label = "Original source concentration",
                        value = originalSourceConcentration,
                        supportingText = "Raw source result × sample dilution factor"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabResultCard(label = "MS recovery", value = msRecovery)
                    Spacer(modifier = Modifier.height(8.dp))
                    LabResultCard(label = "MSD recovery", value = msdRecovery)
                    Spacer(modifier = Modifier.height(8.dp))
                    LabResultCard(label = "MS/MSD RPD", value = msMsdRpd,
                        supportingText = rpdUnavailableReason,
                        copyEnabled = rpdUnavailableReason == null)
                }

                if (calculationSectionsEncoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CalculationStepsCard(
                        sections = decodeCalculationStepSections(calculationSectionsEncoded)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "RPD compares measured MS/MSD concentrations, not recoveries. No pass/fail decision is made. This assumes the spike does not materially change the native sample concentration.",
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
                text = "RPD (%) = |MS − MSD| ÷ |(MS + MSD) ÷ 2| × 100",
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
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        suffix = suffix,
        error = error,
        imeAction = imeAction
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000)
@Composable
private fun MsMsdCalculatorNarrowPreview() {
    LabCalculatorTheme {
        MsMsdCalculatorScreen()
    }
}
