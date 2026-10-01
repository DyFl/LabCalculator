package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.MsMsdCalculator
import com.dyfl.labcalculator.calculation.MsMsdField
import com.dyfl.labcalculator.calculation.MsMsdInput
import com.dyfl.labcalculator.calculation.MsMsdResult
import com.dyfl.labcalculator.calculation.UnitChanges
import com.dyfl.labcalculator.calculation.UnitChangeResult
import com.dyfl.labcalculator.calculation.CONCENTRATION_CHANGE_GUIDANCE
import com.dyfl.labcalculator.calculation.MS_MSD_BASIS_GUIDANCE
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.theme.LabFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabText
import kotlinx.coroutines.launch

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
    var unitChangeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUnitName by rememberSaveable { mutableStateOf<String?>(null) }
    val concentrationUnit = ConcentrationUnit.valueOf(concentrationUnitName)
    val sourceFocus = remember { FocusRequester() }
    val sourceRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

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
        unitChangeMessage = null
        fieldErrors = fieldErrors - field
    }

    fun resetConcentrations(unit: ConcentrationUnit) {
        concentrationUnitName = unit.name
        rawSourceResult = ""
        finalSpikeConcentration = ""
        msResult = ""
        msdResult = ""
        fieldErrors = fieldErrors.filterKeys { it == MsMsdField.DILUTION_FACTOR }
        clearCalculatedValues()
        unitChangeMessage = null
        pendingUnitName = null
    }

    fun concentrationUnitChanged(unit: ConcentrationUnit) {
        if (unit == concentrationUnit) return
        val concentrations = listOf(rawSourceResult, finalSpikeConcentration, msResult, msdResult)
        when (val change = UnitChanges.concentrations(
            concentrations, concentrationUnit, unit)) {
            is UnitChangeResult.Converted -> {
                rawSourceResult = change.values[0]
                finalSpikeConcentration = change.values[1]
                msResult = change.values[2]
                msdResult = change.values[3]
                concentrationUnitName = unit.name
                clearCalculatedValues()
                unitChangeMessage = null
            }
            is UnitChangeResult.Blocked -> unitChangeMessage = change.message
            UnitChangeResult.ResetRequired -> {
                if (concentrations.all { it.isBlank() }) resetConcentrations(unit)
                else pendingUnitName = unit.name
            }
        }
    }

    fun clearMeasurements() {
        rawSourceResult = ""
        msResult = ""
        msdResult = ""
        clearCalculatedValues()
        unitChangeMessage = null
        pendingUnitName = null
    }

    fun nextSample() {
        clearMeasurements()
        fieldErrors = fieldErrors.filterKeys {
            it == MsMsdField.DILUTION_FACTOR || it == MsMsdField.FINAL_SPIKE_CONCENTRATION
        }
        scope.launch {
            // Wait for the cleared form to lay out, then show the first measurement and keyboard.
            withFrameNanos { }
            sourceFocus.requestFocus()
            sourceRequester.bringIntoView()
        }
    }

    if (pendingUnitName != null) UnitResetDialog(
        description = "No conversion between these concentration families is supported. Clear source, spike, MS and MSD, " +
            "select ${ConcentrationUnit.valueOf(checkNotNull(pendingUnitName)).label}, and re-enter. Dilution factor stays.",
        onCancel = { pendingUnitName = null },
        onReset = {
            resetConcentrations(ConcentrationUnit.valueOf(checkNotNull(pendingUnitName)))
        }
    )

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

    LabScreen(modifier = modifier, onCalculate = ::calculate) {
        LabScreenHeading("Matrix Spike / Matrix Spike Duplicate",
            subtitle = "MS/MSD recovery and literal-result RPD")

        Spacer(modifier = Modifier.height(14.dp))
        MsMsdEquationCard(concentrationUnit)
        Spacer(modifier = Modifier.height(14.dp))

        LabInfoRow("All concentrations are in ${concentrationUnit.label}.")
        LabInfoRow(MS_MSD_BASIS_GUIDANCE)
        Spacer(modifier = Modifier.height(LabFieldSpacing))

        LabInputsCard {
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
                    clearMeasurements()
                    fieldErrors = emptyMap()
                }
            )
            Text("Preparation settings", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            LabDropdown(
                label = "Shared concentration unit",
                selected = concentrationUnit,
                options = ConcentrationUnit.entries,
                buttonText = { it.label },
                menuText = { it.description },
                onSelected = ::concentrationUnitChanged,
                modifier = Modifier.fillMaxWidth(),
                supportingText = "${concentrationUnit.family.label}. Applies to source, spike, MS and MSD."
            )
            UnitChangeMessage(unitChangeMessage)
            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            MsMsdInputField(
                label = "Sample dilution factor",
                supportingText = "Positive factors, including decimals, are accepted.",
                value = dilutionFactor,
                onValueChange = {
                    dilutionFactor = it
                    inputChanged(MsMsdField.DILUTION_FACTOR)
                },
                error = fieldErrors[MsMsdField.DILUTION_FACTOR]
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))
            MsMsdInputField(
                label = "Final spike concentration added",
                supportingText =
                    "Added to each diluted aliquot after dilution.",
                value = finalSpikeConcentration,
                onValueChange = {
                    finalSpikeConcentration = it
                    inputChanged(MsMsdField.FINAL_SPIKE_CONCENTRATION)
                },
                error = fieldErrors[MsMsdField.FINAL_SPIKE_CONCENTRATION],
                unit = concentrationUnit
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))
            // Include the heading so a wrapped floating label cannot sit under the tab bar.
            Column(Modifier.bringIntoViewRequester(sourceRequester)) {
                Text("Uncorrected measurements", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                MsMsdInputField(
                    label = "Raw diluted source-sample result",
                    supportingText = "Before applying the dilution factor.",
                    value = rawSourceResult,
                    onValueChange = {
                        rawSourceResult = it
                        inputChanged(MsMsdField.RAW_SOURCE_RESULT)
                    },
                    error = fieldErrors[MsMsdField.RAW_SOURCE_RESULT],
                    unit = concentrationUnit,
                    modifier = Modifier.focusRequester(sourceFocus)
                )
            }
            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            MsMsdInputField(
                label = "Literal MS result",
                value = msResult,
                onValueChange = {
                    msResult = it
                    inputChanged(MsMsdField.MS_RESULT)
                },
                error = fieldErrors[MsMsdField.MS_RESULT],
                unit = concentrationUnit
            )

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            MsMsdInputField(
                label = "Literal MSD result",
                value = msdResult,
                onValueChange = {
                    msdResult = it
                    inputChanged(MsMsdField.MSD_RESULT)
                },
                error = fieldErrors[MsMsdField.MSD_RESULT],
                unit = concentrationUnit,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabCalculateActions(onNextSample = ::nextSample, onClear = {
                    dilutionFactor = "1"
                    finalSpikeConcentration = ""
                    concentrationUnitName = ConcentrationUnit.PPB.name
                clearMeasurements()
                fieldErrors = emptyMap()
            })
        }

        if (originalSourceConcentration.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabFieldSpacing))
            MsMsdResultSummary(originalSourceConcentration, msRecovery, msdRecovery,
                msMsdRpd, rpdUnavailableReason)
        }

        if (calculationSectionsEncoded.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CalculationStepsCard(
                sections = decodeCalculationStepSections(calculationSectionsEncoded)
            )
        }
    }
}

@Composable
private fun MsMsdEquationCard(concentrationUnit: ConcentrationUnit) {
    LabFormulaCard {
        Text(
            text = "All concentration terms use ${concentrationUnit.label}.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
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
        Text(
            text = "Dilution handling",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )
        Text(
            text = "Only the native source result is multiplied by the dilution factor; " +
                "the spike, literal MS/MSD results, recoveries, and RPD are not.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
        Text(
            text = "RPD compares measured MS/MSD concentrations, not recoveries. No pass/fail decision is made.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
        Text(CONCENTRATION_CHANGE_GUIDANCE, style = MaterialTheme.typography.bodySmall,
            color = LabMutedText)
    }
}

@Composable
private fun MsMsdInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    unit: ConcentrationUnit? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    LabNumberTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        suffix = unit?.label,
        modifier = modifier.fillMaxWidth(),
        supportingText = supportingText,
        error = error,
        imeAction = imeAction
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000)
@Composable
private fun MsMsdCalculatorNarrowPreview() {
    LabCalculatorTheme {
        MsMsdCalculatorScreen()
    }
}
