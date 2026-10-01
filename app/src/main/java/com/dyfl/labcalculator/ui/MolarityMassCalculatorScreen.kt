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
import com.dyfl.labcalculator.calculation.MolarityMassCalculator
import com.dyfl.labcalculator.calculation.MolarityMassField
import com.dyfl.labcalculator.calculation.MolarityMassInput
import com.dyfl.labcalculator.calculation.MolarityMassResult
import com.dyfl.labcalculator.calculation.MolarityVolumeUnit
import com.dyfl.labcalculator.calculation.UnitChanges
import com.dyfl.labcalculator.calculation.UnitChangeResult
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabMutedText

@Composable
fun MolarityMassCalculatorScreen(modifier: Modifier = Modifier) {
    var molarity by rememberSaveable { mutableStateOf("") }
    var finalSolutionVolume by rememberSaveable { mutableStateOf("") }
    var volumeUnitName by rememberSaveable { mutableStateOf(MolarityVolumeUnit.MILLILITER.name) }
    var formulaWeight by rememberSaveable { mutableStateOf("") }
    var requiredMass by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var errors by rememberSaveable { mutableStateOf(emptyMap<MolarityMassField, String>()) }
    var unitChangeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val volumeUnit = MolarityVolumeUnit.valueOf(volumeUnitName)

    fun clearResultAndError(field: MolarityMassField) {
        requiredMass = ""
        calculationStepsEncoded = ""
        unitChangeMessage = null
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

    LabScreen(modifier = modifier, onCalculate = ::calculate) {
        LabScreenHeading("Molarity / Dry Chemical Mass")
        Spacer(modifier = Modifier.height(14.dp))
        LabFormulaCard {
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
            Text(
                text = "Use the final solution volume. 1,000 mL = 1 L. Volume unit changes convert the " +
                    "quantity exactly; blanks stay blank and invalid numbers block the change.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Use the formula weight for the actual reagent, including waters of hydration. Assumes pure reagent; no purity correction is applied.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
            Text(
                text = "Mass is displayed to 5 significant figures. This does not infer measurement precision from your inputs. Results are in grams.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        LabInputsCard {
            PresetControls(
                kind = PresetKind.MOLARITY_MASS,
                description = "Save molarity, final volume and formula weight as a reagent recipe.",
                currentSettings = {
                    PresetSettings.MolarityMass(MolarityMassInput(
                        molarity, finalSolutionVolume, volumeUnit, formulaWeight))
                },
                onApply = { settings ->
                    val input = (settings as PresetSettings.MolarityMass).input
                    molarity = input.molarity
                    finalSolutionVolume = input.finalSolutionVolume
                    volumeUnitName = input.volumeUnit.name
                    formulaWeight = input.formulaWeight
                    requiredMass = ""
                    calculationStepsEncoded = ""
                    errors = emptyMap()
                    unitChangeMessage = null
                }
            )
            LabNumberTextField(
                label = "Desired Molarity",
                value = molarity,
                onValueChange = {
                    molarity = it
                    clearResultAndError(MolarityMassField.MOLARITY)
                },
                modifier = Modifier.fillMaxWidth(),
                suffix = "M (mol/L)",
                error = errors[MolarityMassField.MOLARITY]
            )
            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabNumberWithUnit(
                label = "Final Solution Volume",
                value = finalSolutionVolume,
                onValueChange = {
                    finalSolutionVolume = it
                    clearResultAndError(MolarityMassField.FINAL_SOLUTION_VOLUME)
                },
                unit = volumeUnit,
                options = MolarityVolumeUnit.entries,
                unitText = { it.label },
                onUnitChange = {
                    if (it != volumeUnit) {
                        when (val change = UnitChanges.metric(listOf(finalSolutionVolume), volumeUnit.metricUnit, it.metricUnit)) {
                            is UnitChangeResult.Converted -> {
                                finalSolutionVolume = change.values.single()
                                volumeUnitName = it.name
                                requiredMass = ""
                                calculationStepsEncoded = ""
                                unitChangeMessage = null
                            }
                            is UnitChangeResult.Blocked -> unitChangeMessage = change.message
                            UnitChangeResult.ResetRequired -> error("Volume units must be compatible.")
                        }
                    }
                },
                error = errors[MolarityMassField.FINAL_SOLUTION_VOLUME],
                supportingText = "Use the final solution volume, including the reagent."
            )
            UnitChangeMessage(unitChangeMessage)

            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabNumberTextField(
                label = "Formula Weight",
                value = formulaWeight,
                onValueChange = {
                    formulaWeight = it
                    clearResultAndError(MolarityMassField.FORMULA_WEIGHT)
                },
                modifier = Modifier.fillMaxWidth(),
                suffix = "g/mol",
                supportingText = "Use the actual reagent, including hydration. Assumes pure reagent.",
                error = errors[MolarityMassField.FORMULA_WEIGHT],
                imeAction = ImeAction.Done
            )
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabCalculateActions(onClear = {
                molarity = ""
                finalSolutionVolume = ""
                formulaWeight = ""
                volumeUnitName = MolarityVolumeUnit.MILLILITER.name
                requiredMass = ""
                calculationStepsEncoded = ""
                errors = emptyMap()
                unitChangeMessage = null
            })
        }

        if (requiredMass.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabResultCard(label = "Required Mass", value = "$requiredMass g")
        }

        if (calculationStepsEncoded.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CalculationStepsCard(steps = decodeCalculationSteps(calculationStepsEncoded))
        }
    }
}
