package com.example.labcalculator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.labcalculator.ui.theme.LabBlue
import com.example.labcalculator.ui.theme.LabCalculatorTheme
import com.example.labcalculator.ui.theme.LabFormCard
import com.example.labcalculator.ui.theme.LabMutedText
import com.example.labcalculator.ui.theme.LabScreenBackground

private enum class CalculatorTab(val title: String) {
    DILUTION("Dilution"),
    RPD("RPD"),
    UNIT_CONVERSIONS("Unit conversions"),
    MS_MSD("MS/MSD")
}

@Composable
fun LabCalculatorApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(CalculatorTab.DILUTION.name) }
    val selectedTab = CalculatorTab.valueOf(selectedTabName)
    val tabStateHolder = rememberSaveableStateHolder()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LabScreenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Text(
                text = "Lab Calculator",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = LabBlue
            )

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = LabFormCard,
                contentColor = LabBlue,
                edgePadding = 8.dp,
                minTabWidth = 96.dp
            ) {
                CalculatorTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTabName = tab.name },
                        selectedContentColor = LabBlue,
                        unselectedContentColor = LabMutedText,
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                tabStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        CalculatorTab.DILUTION -> DilutionCalculatorScreen()
                        CalculatorTab.RPD -> RpdCalculatorScreen()
                        CalculatorTab.UNIT_CONVERSIONS -> UnitConversionsScreen()
                        CalculatorTab.MS_MSD -> MsMsdCalculatorScreen()
                    }
                }
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
