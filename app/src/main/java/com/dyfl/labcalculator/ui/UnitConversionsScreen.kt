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
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.MetricUnit
import com.dyfl.labcalculator.calculation.UnitCategory
import com.dyfl.labcalculator.calculation.UnitConversionResult
import com.dyfl.labcalculator.calculation.UnitConverter
import com.dyfl.labcalculator.calculation.UnitChanges
import com.dyfl.labcalculator.calculation.UnitChangeResult
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabMutedText

@Composable
fun UnitConversionsScreen(modifier: Modifier = Modifier) {
    var categoryName by rememberSaveable { mutableStateOf(UnitCategory.MASS.name) }
    var fromUnitName by rememberSaveable { mutableStateOf(MetricUnit.MILLIGRAM.name) }
    var toUnitName by rememberSaveable { mutableStateOf(MetricUnit.MICROGRAM.name) }
    var inputValue by rememberSaveable { mutableStateOf("") }
    var convertedValue by rememberSaveable { mutableStateOf("") }
    var calculationStepsEncoded by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }
    var unitChangeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCategoryName by rememberSaveable { mutableStateOf<String?>(null) }

    val category = UnitCategory.valueOf(categoryName)
    val categoryUnits = MetricUnit.forCategory(category)
    val fromUnit = MetricUnit.valueOf(fromUnitName)
    val toUnit = MetricUnit.valueOf(toUnitName)

    fun clearResult(clearError: Boolean = false) {
        convertedValue = ""
        calculationStepsEncoded = ""
        if (clearError) inputError = null
        unitChangeMessage = null
    }

    fun changeStartingUnit(unit: MetricUnit, swap: Boolean = false) {
        if (unit == fromUnit && !swap) return
        when (val change = UnitChanges.metric(listOf(inputValue), fromUnit, unit)) {
            is UnitChangeResult.Converted -> {
                inputValue = change.values.single()
                if (swap) toUnitName = fromUnitName
                fromUnitName = unit.name
                clearResult()
            }
            is UnitChangeResult.Blocked -> unitChangeMessage = change.message
            UnitChangeResult.ResetRequired -> error("Starting units must be in the same category.")
        }
    }

    fun resetCategory(selectedCategory: UnitCategory) {
        val units = MetricUnit.forCategory(selectedCategory)
        categoryName = selectedCategory.name
        fromUnitName = units.getOrElse(1) { units.first() }.name
        toUnitName = units.first().name
        inputValue = ""
        clearResult(clearError = true)
        pendingCategoryName = null
    }

    if (pendingCategoryName != null) UnitResetDialog(
        description = "Changing category cannot convert the entered quantity. Clear the input and results, " +
            "select ${UnitCategory.valueOf(checkNotNull(pendingCategoryName)).displayName}, and re-enter the value.",
        onCancel = { pendingCategoryName = null },
        onReset = {
            resetCategory(UnitCategory.valueOf(checkNotNull(pendingCategoryName)))
        }
    )

    fun calculate() {
        when (val result = UnitConverter.convert(inputValue, fromUnit, toUnit)) {
            is UnitConversionResult.Success -> {
                convertedValue = result.formattedValue
                calculationStepsEncoded = encodeCalculationSteps(result.calculationSteps)
                inputError = null
            }

            is UnitConversionResult.Invalid -> {
                convertedValue = ""
                calculationStepsEncoded = ""
                inputError = result.message
            }
        }
    }

    LabScreen(modifier = modifier, onCalculate = ::calculate) {
        LabScreenHeading("Unit Conversions",
            subtitle = "Exact metric conversions by category")

        Spacer(modifier = Modifier.height(14.dp))

        LabFormulaCard {
            Text(
                text = UnitConverter.explanation(fromUnit, toUnit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LabBlue
            )
            Text(
                text = "Only units within the selected category can be converted. Mass-per-volume units do not imply a parts-per basis or density.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
            Text("Starting unit changes and Swap units convert the quantity exactly. Blank input stays blank; " +
                "invalid input blocks the change. Destination changes select only the output unit. " +
                "Category changes clear the input for re-entry, with confirmation when it is populated.",
                style = MaterialTheme.typography.bodySmall, color = LabMutedText)
        }
        Spacer(modifier = Modifier.height(14.dp))

        LabInputsCard {
            PresetControls(
                kind = PresetKind.UNIT_CONVERSION,
                description = "Save a starting and destination unit pair.",
                currentSettings = { PresetSettings.Conversion(fromUnit, toUnit) },
                onApply = { settings ->
                    val conversion = settings as PresetSettings.Conversion
                    categoryName = conversion.fromUnit.category.name
                    fromUnitName = conversion.fromUnit.name
                    toUnitName = conversion.toUnit.name
                    inputValue = ""
                    clearResult(clearError = true)
                    pendingCategoryName = null
                }
            )
            LabDropdown(
                label = "Category",
                selected = category,
                options = UnitCategory.entries,
                buttonText = { it.displayName },
                onSelected = { selectedCategory ->
                    if (selectedCategory == category) return@LabDropdown
                    if (inputValue.isBlank()) resetCategory(selectedCategory)
                    else pendingCategoryName = selectedCategory.name
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            UnitChangeMessage(unitChangeMessage)
            LabDropdown(
                label = "Starting unit",
                selected = fromUnit,
                options = categoryUnits,
                buttonText = { "${it.displayName} (${it.symbol})" },
                onSelected = { changeStartingUnit(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabSwapUnitsButton(onClick = {
                changeStartingUnit(toUnit, swap = true)
            })

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabDropdown(
                label = "Destination unit",
                selected = toUnit,
                options = categoryUnits,
                buttonText = { "${it.displayName} (${it.symbol})" },
                onSelected = {
                    if (it != toUnit) {
                        toUnitName = it.name
                        clearResult()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabNumberTextField(
                label = "Value to convert",
                value = inputValue,
                onValueChange = {
                    inputValue = it
                    clearResult(clearError = true)
                },
                modifier = Modifier.fillMaxWidth(),
                suffix = fromUnit.symbol,
                error = inputError,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabCalculateActions(onClear = {
                inputValue = ""
                convertedValue = ""
                calculationStepsEncoded = ""
                inputError = null
                unitChangeMessage = null
                pendingCategoryName = null
            })
        }

        if (convertedValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(LabGroupSpacing))
            LabResultCard(label = "Converted result", value = "$convertedValue ${toUnit.symbol}")
        }

        if (calculationStepsEncoded.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CalculationStepsCard(
                steps = decodeCalculationSteps(calculationStepsEncoded)
            )
        }
    }
}
