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
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionCalculator
import com.dyfl.labcalculator.calculation.DilutionField
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.DilutionResult
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
    var volumeFromStock by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var errors by rememberSaveable { mutableStateOf(emptyMap<DilutionField, String>()) }

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
                    volumeFromStock = ""
                    calculationStepsEncoded = ""
                    errors = emptyMap()
                }
            )
            LabConcentrationInput(
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

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))

            LabConcentrationInput(
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
                error = errors[DilutionField.FINAL_CONCENTRATION],
                supportingText = "Use the same concentration basis for stock and target."
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
                volumeFromStock = ""
                calculationStepsEncoded = ""
                errors = emptyMap()
            })
        }

        if (volumeFromStock.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabResultCard(label = "Volume from stock (V₁)", value = "$volumeFromStock mL")
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
private fun EquationCard() {
    LabFormulaCard {
        Text(
            text = "C₁V₁ = C₂V₂",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )
        Text(
            text = "V₁ (mL) = [C₂ (PPB) × V₂ (mL)] ÷ C₁ (PPB)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Both concentrations are converted to PPB first (1 PPM = 1,000 PPB). Use matching concentration bases; ppm ≈ mg/L only for dilute water solutions. Make up to the final volume, not that volume of solvent.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Short repeating results end in R (for example, 0.333R). Long expansions use an exact fraction in mL. No measurement precision is inferred.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText
        )
    }
}
