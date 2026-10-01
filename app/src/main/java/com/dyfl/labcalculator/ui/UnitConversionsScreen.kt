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

    val category = UnitCategory.valueOf(categoryName)
    val categoryUnits = MetricUnit.forCategory(category)
    val fromUnit = MetricUnit.valueOf(fromUnitName)
    val toUnit = MetricUnit.valueOf(toUnitName)

    fun clearResult() {
        convertedValue = ""
        calculationStepsEncoded = ""
        inputError = null
    }

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
                text = "Only units within the selected category can be converted.",
                style = MaterialTheme.typography.bodySmall,
                color = LabMutedText
            )
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
                    clearResult()
                }
            )
            LabDropdown(
                label = "Category",
                selected = category,
                options = UnitCategory.entries,
                buttonText = { it.displayName },
                onSelected = { selectedCategory ->
                    val units = MetricUnit.forCategory(selectedCategory)
                    categoryName = selectedCategory.name
                    fromUnitName = units.getOrElse(1) { units.first() }.name
                    toUnitName = units.first().name
                    clearResult()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabDropdown(
                label = "Starting unit",
                selected = fromUnit,
                options = categoryUnits,
                buttonText = { "${it.displayName} (${it.symbol})" },
                onSelected = {
                    fromUnitName = it.name
                    clearResult()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabSwapUnitsButton(onClick = {
                val oldFromUnitName = fromUnitName
                fromUnitName = toUnitName
                toUnitName = oldFromUnitName
                clearResult()
            })

            Spacer(modifier = Modifier.height(LabRelatedFieldSpacing))
            LabDropdown(
                label = "Destination unit",
                selected = toUnit,
                options = categoryUnits,
                buttonText = { "${it.displayName} (${it.symbol})" },
                onSelected = {
                    toUnitName = it.name
                    clearResult()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(LabGroupSpacing))

            LabNumberTextField(
                label = "Value to convert",
                value = inputValue,
                onValueChange = {
                    inputValue = it
                    clearResult()
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
