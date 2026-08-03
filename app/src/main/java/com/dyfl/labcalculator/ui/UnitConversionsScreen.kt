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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyfl.labcalculator.calculation.MetricUnit
import com.dyfl.labcalculator.calculation.UnitCategory
import com.dyfl.labcalculator.calculation.UnitConversionResult
import com.dyfl.labcalculator.calculation.UnitConverter
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabEquationCard
import com.dyfl.labcalculator.ui.theme.LabFormCard
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabScreenBackground

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
            text = "Unit Conversions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabBlue
        )
        Text(
            text = "Exact metric conversions by category",
            style = MaterialTheme.typography.bodyMedium,
            color = LabMutedText
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = LabFormCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ConversionHeading("Category")
                Spacer(modifier = Modifier.height(6.dp))
                LabDropdown(
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

                Spacer(modifier = Modifier.height(18.dp))

                ConversionHeading("Starting unit")
                Spacer(modifier = Modifier.height(6.dp))
                LabDropdown(
                    selected = fromUnit,
                    options = categoryUnits,
                    buttonText = { "${it.displayName} (${it.symbol})" },
                    onSelected = {
                        fromUnitName = it.name
                        clearResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ConversionHeading("Destination unit")
                Spacer(modifier = Modifier.height(6.dp))
                LabDropdown(
                    selected = toUnit,
                    options = categoryUnits,
                    buttonText = { "${it.displayName} (${it.symbol})" },
                    onSelected = {
                        toUnitName = it.name
                        clearResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val oldFromUnitName = fromUnitName
                        fromUnitName = toUnitName
                        toUnitName = oldFromUnitName
                        clearResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⇄  Swap starting and destination units")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LabEquationCard)
                ) {
                    Text(
                        text = UnitConverter.explanation(fromUnit, toUnit),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LabBlue,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                ConversionHeading("Value to convert")
                Spacer(modifier = Modifier.height(6.dp))
                LabNumberTextField(
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

                Spacer(modifier = Modifier.height(18.dp))

                ConversionHeading("Converted result")
                Spacer(modifier = Modifier.height(6.dp))
                LabNumberTextField(
                    value = convertedValue,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Result",
                    suffix = toUnit.symbol,
                    readOnly = true,
                    imeAction = ImeAction.None
                )

                if (calculationStepsEncoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CalculationStepsCard(
                        steps = decodeCalculationSteps(calculationStepsEncoded)
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
                            inputValue = ""
                            convertedValue = ""
                            calculationStepsEncoded = ""
                            inputError = null
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
            text = "Only units within the selected category can be converted.",
            style = MaterialTheme.typography.bodySmall,
            color = LabMutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConversionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}
