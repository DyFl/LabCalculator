package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionCalculator
import com.dyfl.labcalculator.calculation.DilutionField
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.DilutionResult
import com.dyfl.labcalculator.calculation.DILUTION_DISPLAY_POLICY
import com.dyfl.labcalculator.calculation.CONCENTRATION_CHANGE_GUIDANCE
import com.dyfl.labcalculator.calculation.UnitChanges
import com.dyfl.labcalculator.calculation.UnitChangeResult
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabMutedText

@Composable
fun DilutionCalculatorScreen(modifier: Modifier = Modifier) {
    var stockConcentration by rememberSaveable { mutableStateOf("") }
    var finalConcentration by rememberSaveable { mutableStateOf("") }
    var finalSolutionVolume by rememberSaveable { mutableStateOf("") }
    var stockUnitName by rememberSaveable { mutableStateOf(ConcentrationUnit.PPM.name) }
    var finalUnitName by rememberSaveable { mutableStateOf(ConcentrationUnit.PPB.name) }
    var calculated by rememberSaveable { mutableStateOf(false) }
    var errors by rememberSaveable { mutableStateOf(emptyMap<DilutionField, String>()) }
    var unitChangeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUnitName by rememberSaveable { mutableStateOf<String?>(null) }

    val stockUnit = ConcentrationUnit.valueOf(stockUnitName)
    val finalUnit = ConcentrationUnit.valueOf(finalUnitName)
    val input = DilutionInput(stockConcentration, stockUnit, finalConcentration,
        finalUnit, finalSolutionVolume)
    // Restore exact working from original inputs, never from a rounded display string.
    val result = remember(input, calculated) {
        if (calculated) DilutionCalculator.calculate(input) as? DilutionResult.Success else null
    }

    fun clearResultAndError(field: DilutionField) {
        calculated = false
        unitChangeMessage = null
        errors = DilutionCalculator.errorsAfterEdit(errors, field)
    }

    fun resetConcentrations(unit: ConcentrationUnit) {
        stockUnitName = unit.name
        finalUnitName = unit.name
        stockConcentration = ""
        finalConcentration = ""
        errors = errors.filterKeys { it == DilutionField.FINAL_SOLUTION_VOLUME }
        calculated = false
        unitChangeMessage = null
        pendingUnitName = null
    }

    fun changeUnit(unit: ConcentrationUnit, stock: Boolean) {
        val current = if (stock) stockUnit else finalUnit
        if (unit == current) return
        val value = if (stock) stockConcentration else finalConcentration
        when (val change = UnitChanges.concentrations(listOf(value), current, unit)) {
            is UnitChangeResult.Converted -> {
                if (stock) {
                    stockConcentration = change.values.single()
                    stockUnitName = unit.name
                } else {
                    finalConcentration = change.values.single()
                    finalUnitName = unit.name
                }
                calculated = false
                unitChangeMessage = null
                // Quantities are unchanged, so existing validation errors remain relevant.
            }
            is UnitChangeResult.Blocked -> unitChangeMessage = change.message
            UnitChangeResult.ResetRequired -> {
                // A family reset affects both fields, including the unselected one.
                if (stockConcentration.isBlank() && finalConcentration.isBlank()) resetConcentrations(unit)
                else pendingUnitName = unit.name
            }
        }
    }

    if (pendingUnitName != null) UnitResetDialog(
        description = "No conversion between these concentration families is supported. Clear stock and target, " +
            "select ${ConcentrationUnit.valueOf(checkNotNull(pendingUnitName)).label} for both, and re-enter. Final volume stays.",
        onCancel = { pendingUnitName = null },
        onReset = {
            resetConcentrations(ConcentrationUnit.valueOf(checkNotNull(pendingUnitName)))
        }
    )

    fun calculate() {
        when (
            val calculation = DilutionCalculator.calculate(input)
        ) {
            is DilutionResult.Success -> {
                calculated = true
                errors = emptyMap()
            }

            is DilutionResult.Invalid -> {
                calculated = false
                errors = calculation.errors.associate { it.field to it.message }
            }
        }
    }

    LabScreen(modifier = modifier, onCalculate = ::calculate) {
        LabScreenHeading("Standard / reagent dilution")

        Spacer(modifier = Modifier.height(14.dp))

        EquationCard()

        Spacer(modifier = Modifier.height(14.dp))

        LabInputsCard {
            PresetControls(
                kind = PresetKind.DILUTION,
                description = "Save concentrations and final volume as a preparation recipe.",
                currentSettings = {
                    PresetSettings.Dilution(DilutionInput(stockConcentration, stockUnit,
                        finalConcentration, finalUnit, finalSolutionVolume))
                },
                onApply = { settings ->
                    val input = (settings as PresetSettings.Dilution).input
                    stockConcentration = input.stockConcentration
                    stockUnitName = input.stockUnit.name
                    finalConcentration = input.finalConcentration
                    finalUnitName = input.finalUnit.name
                    finalSolutionVolume = input.finalSolutionVolumeMl
                    calculated = false
                    errors = emptyMap()
                    unitChangeMessage = null
                    pendingUnitName = null
                }
            )
            LabInfoRow("Concentration family: ${stockUnit.family.label}. " +
                "Stock and target must share the same basis. PPM/PPB are not treated as mg/L/µg/L.")
            UnitChangeMessage(unitChangeMessage)
            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabConcentrationInput(
                label = "Stock concentration (C₁)",
                value = stockConcentration,
                onValueChange = {
                    stockConcentration = it
                    clearResultAndError(DilutionField.STOCK_CONCENTRATION)
                },
                unit = stockUnit,
                onUnitChange = { changeUnit(it, stock = true) },
                error = errors[DilutionField.STOCK_CONCENTRATION]
            )

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))

            LabConcentrationInput(
                label = "Final concentration (C₂)",
                value = finalConcentration,
                onValueChange = {
                    finalConcentration = it
                    clearResultAndError(DilutionField.FINAL_CONCENTRATION)
                },
                unit = finalUnit,
                onUnitChange = { changeUnit(it, stock = false) },
                error = errors[DilutionField.FINAL_CONCENTRATION],
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabNumberTextField(
                label = "Final solution volume (V₂)",
                value = finalSolutionVolume,
                onValueChange = {
                    finalSolutionVolume = it
                    clearResultAndError(DilutionField.FINAL_SOLUTION_VOLUME)
                },
                suffix = "mL",
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Total prepared solution volume, including the stock.",
                error = errors[DilutionField.FINAL_SOLUTION_VOLUME],
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabCalculateActions(onClear = {
                stockConcentration = ""
                finalConcentration = ""
                finalSolutionVolume = ""
                stockUnitName = ConcentrationUnit.PPM.name
                finalUnitName = ConcentrationUnit.PPB.name
                calculated = false
                errors = emptyMap()
                unitChangeMessage = null
                pendingUnitName = null
            })
        }

        if (result != null) {
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabResultCard(
                label = "Preparation",
                value = result.preparationInstruction,
                valueStyle = MaterialTheme.typography.titleLarge,
                supportingText = if (result.isApproximate)
                    "≈ marks an approximation. Exact values are in Calculation Steps. " +
                        "Display precision does not represent pipette capability or measurement uncertainty."
                    else null
            )
            Spacer(modifier = Modifier.height(14.dp))
            CalculationStepsCard(steps = result.calculationSteps)
        }
    }
}

@Composable
private fun EquationCard() {
    LabFormulaCard {
        Text(
            text = "C₁V₁ = C₂V₂",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )
        Text(
            text = "V₁ (mL) = [C₂ × V₂ (mL)] ÷ C₁, using matching concentration units",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "1 PPM = 1,000 PPB on the same parts-per basis; 1 mg/L = 1,000 µg/L. " +
                "Only conversions within a family are supported. No density is inferred. " +
                "Make up to the final volume, including the stock.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
        Text(
            text = DILUTION_DISPLAY_POLICY,
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
        Text(CONCENTRATION_CHANGE_GUIDANCE, style = MaterialTheme.typography.bodySmall,
            color = LabMutedText)
    }
}
