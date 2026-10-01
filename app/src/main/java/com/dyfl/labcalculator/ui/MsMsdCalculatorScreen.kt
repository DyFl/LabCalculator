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

    fun concentrationUnitChanged(unit: ConcentrationUnit) {
        if (unit == concentrationUnit) return
        when (val change = UnitChanges.concentrations(
            listOf(rawSourceResult, finalSpikeConcentration, msResult, msdResult), concentrationUnit, unit)) {
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
            UnitChangeResult.ResetRequired -> pendingUnitName = unit.name
        }
    }

    fun nextSample() {
        rawSourceResult = ""
        msResult = ""
        msdResult = ""
        clearCalculatedValues()
        fieldErrors = emptyMap()
        unitChangeMessage = null
        pendingUnitName = null
    }

    if (pendingUnitName != null) UnitResetDialog(
        description = "No conversion between these concentration families is supported. Clear source, spike, MS and MSD, " +
            "select ${ConcentrationUnit.valueOf(checkNotNull(pendingUnitName)).label}, and re-enter. Dilution factor stays.",
        onCancel = { pendingUnitName = null },
        onReset = {
            concentrationUnitName = checkNotNull(pendingUnitName)
            rawSourceResult = ""
            finalSpikeConcentration = ""
            msResult = ""
            msdResult = ""
            fieldErrors = fieldErrors.filterKeys { it == MsMsdField.DILUTION_FACTOR }
            clearCalculatedValues()
            unitChangeMessage = null
            pendingUnitName = null
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
                    nextSample()
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
            LabInfoRow(CONCENTRATION_CHANGE_GUIDANCE)
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
                    "Final concentration added to each diluted aliquot after sample dilution.",
                value = finalSpikeConcentration,
                onValueChange = {
                    finalSpikeConcentration = it
                    inputChanged(MsMsdField.FINAL_SPIKE_CONCENTRATION)
                },
                error = fieldErrors[MsMsdField.FINAL_SPIKE_CONCENTRATION],
                unit = concentrationUnit
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))
            Text("Uncorrected measurements", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            MsMsdInputField(
                label = "Raw diluted source-sample result",
                supportingText = "Uncorrected measured result, before applying the dilution factor.",
                value = rawSourceResult,
                onValueChange = {
                    rawSourceResult = it
                    inputChanged(MsMsdField.RAW_SOURCE_RESULT)
                },
                error = fieldErrors[MsMsdField.RAW_SOURCE_RESULT],
                unit = concentrationUnit
            )
            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            MsMsdInputField(
                label = "Literal MS result",
                supportingText = "Uncorrected MS result, on the same dilution basis as the source.",
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
                supportingText = "Uncorrected MSD result, on the same dilution basis as the source.",
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
                nextSample()
            })
        }

        if (originalSourceConcentration.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabFieldSpacing))
            LabResultCard(
                label = "Original source concentration",
                value = originalSourceConcentration,
                supportingText = "Raw source result × sample dilution factor; ${concentrationUnit.family.label}."
            )
            Spacer(modifier = Modifier.height(8.dp))
            LabResultCard(label = "MS recovery", value = msRecovery, scrollIntoView = false)
            Spacer(modifier = Modifier.height(8.dp))
            LabResultCard(label = "MSD recovery", value = msdRecovery, scrollIntoView = false)
            Spacer(modifier = Modifier.height(8.dp))
            LabResultCard(label = "MS/MSD RPD", value = msMsdRpd,
                supportingText = rpdUnavailableReason,
                scrollIntoView = false,
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
            text = MS_MSD_BASIS_GUIDANCE + " Only the native source result is multiplied by the dilution factor; " +
                "the spike, literal MS/MSD results, recoveries, and RPD are not.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
        Text(
            text = "RPD compares measured MS/MSD concentrations, not recoveries. No pass/fail decision is made. This assumes the spike does not materially change the native sample concentration.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
    }
}

@Composable
private fun MsMsdInputField(
    label: String,
    supportingText: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    unit: ConcentrationUnit? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    LabNumberTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        suffix = unit?.label,
        modifier = Modifier.fillMaxWidth(),
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
