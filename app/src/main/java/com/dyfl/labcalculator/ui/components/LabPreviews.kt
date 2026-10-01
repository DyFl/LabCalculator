package com.dyfl.labcalculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.CONCENTRATION_CHANGE_GUIDANCE
import com.dyfl.labcalculator.calculation.MS_MSD_BASIS_GUIDANCE
import com.dyfl.labcalculator.calculation.MetricUnit
import com.dyfl.labcalculator.calculation.MolarityVolumeUnit
import com.dyfl.labcalculator.calculation.UnitCategory
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import com.dyfl.labcalculator.ui.theme.LabFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabRelatedFieldSpacing
import com.dyfl.labcalculator.ui.theme.LabGroupSpacing

@Preview(name = "Light · 360 × 640", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Dark · 360 × 640", widthDp = 360, heightDp = 640,
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light · 360 × 640 · 1.3×", widthDp = 360, heightDp = 640,
    showBackground = true, fontScale = 1.3f)
@Preview(name = "Dark · 360 × 640 · 1.3×", widthDp = 360, heightDp = 640,
    showBackground = true, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class LabSmallScreenPreviews

internal enum class LabPreviewState { EMPTY, FILLED, ERROR, RESULT }
internal enum class LabPreviewTab(val title: String, val heading: String) {
    DILUTION("Dilution", "Standard / reagent dilution"),
    RPD("RPD", "Relative Percent Difference"),
    CONVERT("Unit conversions", "Unit Conversions"),
    MS_MSD("MS/MSD", "Matrix Spike / Matrix Spike Duplicate"),
    MOLARITY("Molarity", "Molarity / Dry Chemical Mass")
}

internal data class LabPreviewCase(val tab: LabPreviewTab, val state: LabPreviewState)

internal class LabPreviewParameters : PreviewParameterProvider<LabPreviewCase> {
    override val values = LabPreviewTab.entries.asSequence().flatMap { tab ->
        LabPreviewState.entries.asSequence().map { state -> LabPreviewCase(tab, state) }
    }
    override fun getDisplayName(index: Int): String = values.elementAt(index).let {
        "${it.tab.title} · ${it.state.name.lowercase()}"
    }
}

/** All five tabs and four states exercise shared UI without mutating stored state. */
@Composable
internal fun LabComponentPreview(state: LabPreviewState, tab: LabPreviewTab = LabPreviewTab.DILUTION) {
    val filled = state == LabPreviewState.FILLED || state == LabPreviewState.RESULT
    val error = state == LabPreviewState.ERROR
    LabAppChrome(LabPreviewTab.entries.map { it.title }, tab.ordinal, {}) {
        LabScreen(onCalculate = {}, scrollState = rememberScrollState()) {
            LabScreenHeading(tab.heading)
            Spacer(Modifier.height(14.dp))
            LabFormulaCard { Text("Formula and input assumptions") }
            Spacer(Modifier.height(14.dp))
            if (tab == LabPreviewTab.MS_MSD) {
                LabInfoRow("All concentrations are in PPB.")
                LabInfoRow(MS_MSD_BASIS_GUIDANCE)
                Spacer(Modifier.height(LabFieldSpacing))
            }
            LabInputsCard {
                Column {
                    when (tab) {
                        LabPreviewTab.DILUTION -> {
                            LabInfoRow("Concentration family: Parts per: matching ratio basis. " +
                                "Stock and target must share the same basis. PPM/PPB are not treated as mg/L/µg/L.")
                            LabInfoRow(CONCENTRATION_CHANGE_GUIDANCE)
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabConcentrationInput("Stock concentration (C₁)", if (filled) "10" else "", {},
                                ConcentrationUnit.PPM, {},
                                error = if (error) "Enter the stock concentration." else null)
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabConcentrationInput("Final concentration (C₂)", if (filled) "200" else "", {},
                                ConcentrationUnit.PPB, {}, error = null,
                                supportingText = "Use the same concentration basis for stock and target.")
                            Spacer(Modifier.height(LabGroupSpacing))
                            LabNumberTextField(if (filled) "50" else "", {}, label = "Final solution volume (V₂)",
                                suffix = "mL", supportingText = "Total prepared solution volume, including the stock.",
                                imeAction = ImeAction.Done)
                        }
                        LabPreviewTab.RPD -> {
                            LabNumberTextField(if (filled) "10" else "", {}, label = "Original Sample Result",
                                error = if (error) "Enter the original sample result." else null)
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabNumberTextField(if (filled) "12" else "", {}, label = "Replicate Sample Result",
                                imeAction = ImeAction.Done)
                        }
                        LabPreviewTab.CONVERT -> {
                            // The longest names also exercise wrapping in dropdown fields.
                            LabDropdown("Category", UnitCategory.MASS_CONCENTRATION, UnitCategory.entries,
                                buttonText = { it.displayName }, onSelected = {})
                            Spacer(Modifier.height(LabGroupSpacing))
                            Column(verticalArrangement = Arrangement.spacedBy(LabRelatedFieldSpacing)) {
                                LabDropdown("Starting unit", MetricUnit.MILLIGRAM_PER_LITER,
                                    MetricUnit.forCategory(UnitCategory.MASS_CONCENTRATION),
                                    buttonText = { "${it.displayName} (${it.symbol})" }, onSelected = {})
                                LabSwapUnitsButton {}
                                LabDropdown("Destination unit", MetricUnit.MICROGRAM_PER_LITER,
                                    MetricUnit.forCategory(UnitCategory.MASS_CONCENTRATION),
                                    buttonText = { "${it.displayName} (${it.symbol})" }, onSelected = {})
                            }
                            Spacer(Modifier.height(LabGroupSpacing))
                            LabNumberTextField(if (filled) "1" else "", {}, label = "Value to convert",
                                suffix = "mg/L", error = if (error) "Enter the value to convert." else null,
                                imeAction = ImeAction.Done)
                        }
                        LabPreviewTab.MS_MSD -> {
                            Text("Preparation settings", style = MaterialTheme.typography.titleSmall)
                            LabDropdown("Shared concentration unit", ConcentrationUnit.PPB, ConcentrationUnit.entries,
                                buttonText = { it.label }, menuText = { it.description }, onSelected = {},
                                supportingText = "Parts per: matching ratio basis. Applies to source, spike, MS and MSD.")
                            LabInfoRow(CONCENTRATION_CHANGE_GUIDANCE)
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabNumberTextField(if (filled) "10" else "1", {}, label = "Sample dilution factor",
                                supportingText = "Positive factors, including decimals, are accepted.")
                            Spacer(Modifier.height(LabGroupSpacing))
                            LabNumberTextField(if (filled) "50" else "", {}, label = "Final spike concentration added", suffix = "PPB",
                                supportingText = "Final concentration added to each diluted aliquot after sample dilution.")
                            Spacer(Modifier.height(LabGroupSpacing))
                            Text("Uncorrected measurements", style = MaterialTheme.typography.titleSmall)
                            LabNumberTextField(if (filled) "5" else "", {}, label = "Raw diluted source-sample result", suffix = "PPB",
                                error = if (error) "Enter the raw source result." else null,
                                supportingText = "Uncorrected measured result, before applying the dilution factor.")
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabNumberTextField(if (filled) "55" else "", {}, label = "Literal MS result", suffix = "PPB",
                                supportingText = "Uncorrected MS result, on the same dilution basis as the source.")
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabNumberTextField(if (filled) "50" else "", {}, label = "Literal MSD result", suffix = "PPB", imeAction = ImeAction.Done,
                                supportingText = "Uncorrected MSD result, on the same dilution basis as the source.")
                        }
                        LabPreviewTab.MOLARITY -> {
                            LabNumberTextField(if (filled) "1" else "", {}, label = "Desired Molarity",
                                suffix = "M (mol/L)", error = if (error) "Enter the desired molarity." else null)
                            Spacer(Modifier.height(LabRelatedFieldSpacing))
                            LabNumberWithUnit("Final Solution Volume", if (filled) "1000" else "", {},
                                MolarityVolumeUnit.MILLILITER, MolarityVolumeUnit.entries,
                                unitText = { it.label }, onUnitChange = {},
                                supportingText = "Use the final solution volume, including the reagent.")
                            Spacer(Modifier.height(LabGroupSpacing))
                            LabNumberTextField(if (filled) "58.44" else "", {}, label = "Formula Weight",
                                suffix = "g/mol", imeAction = ImeAction.Done,
                                supportingText = "Use the actual reagent, including hydration. Assumes pure reagent.")
                        }
                    }
                    Spacer(Modifier.height(LabGroupSpacing))
                    LabCalculateActions(onClear = {}, onNextSample = if (tab == LabPreviewTab.MS_MSD) ({}) else null)
                }
            }
            if (state == LabPreviewState.RESULT) {
                Spacer(Modifier.height(LabGroupSpacing))
                CompositionLocalProvider(LocalCalculationRequest provides 1) {
                    when (tab) {
                        LabPreviewTab.DILUTION -> LabResultCard("Preparation",
                            "Transfer 1 mL of stock and make up to 50 mL final solution volume.",
                            valueStyle = MaterialTheme.typography.titleLarge)
                        LabPreviewTab.RPD -> LabResultCard("Relative Percent Difference", "18.18%")
                        LabPreviewTab.CONVERT -> LabResultCard("Converted result", "1000 µg/L")
                        LabPreviewTab.MOLARITY -> LabResultCard("Required Mass", "58.440 g")
                        LabPreviewTab.MS_MSD -> {
                            LabResultCard("Original source concentration", "50 PPB",
                                supportingText = "Raw source result × sample dilution factor")
                            Spacer(Modifier.height(8.dp))
                            LabResultCard("MS recovery", "100.00%", scrollIntoView = false)
                            Spacer(Modifier.height(8.dp))
                            LabResultCard("MSD recovery", "90.00%", scrollIntoView = false)
                            Spacer(Modifier.height(8.dp))
                            LabResultCard("MS/MSD RPD", "9.52%", scrollIntoView = false)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                CalculationStepsCard(steps = listOf("Substitute the entered values.", "Calculated result with units."))
            }
        }
    }
}

@LabSmallScreenPreviews
@Composable
private fun CalculatorStatePreview(@PreviewParameter(LabPreviewParameters::class) case: LabPreviewCase) =
    LabCalculatorTheme { LabComponentPreview(case.state, case.tab) }
