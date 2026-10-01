package com.dyfl.labcalculator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme

private enum class CalculatorTab(val title: String) {
    DILUTION("Dilution"),
    RPD("RPD"),
    UNIT_CONVERSIONS("Unit conversions"),
    MS_MSD("MS/MSD"),
    MOLARITY_MASS("Molarity")
}

@Composable
fun LabCalculatorApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(CalculatorTab.DILUTION.name) }
    val selectedTab = CalculatorTab.valueOf(selectedTabName)
    val tabStateHolder = rememberSaveableStateHolder()

    LabAppChrome(
        titles = CalculatorTab.entries.map { it.title },
        selectedTabIndex = selectedTab.ordinal,
        onTabSelected = { selectedTabName = CalculatorTab.entries[it].name }
    ) {
        tabStateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                CalculatorTab.DILUTION -> DilutionCalculatorScreen()
                CalculatorTab.RPD -> RpdCalculatorScreen()
                CalculatorTab.UNIT_CONVERSIONS -> UnitConversionsScreen()
                CalculatorTab.MS_MSD -> MsMsdCalculatorScreen()
                CalculatorTab.MOLARITY_MASS -> MolarityMassCalculatorScreen()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable
private fun LabCalculatorAppPreview() {
    LabCalculatorTheme {
        LabCalculatorApp()
    }
}
