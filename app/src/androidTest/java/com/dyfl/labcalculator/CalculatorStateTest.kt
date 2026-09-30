package com.dyfl.labcalculator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import org.junit.Rule
import org.junit.Test

class CalculatorStateTest {
    @get:Rule val compose = createComposeRule()

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    @Test
    fun resultSurvivesTabSwitchAndRestorationButClearsOnEdit() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("RPD")
        field("Original Sample Result").performTextInput("10")
        field("Replicate Sample Result").performTextInput("12")
        click("Calculate")
        compose.onNodeWithText("18.18%").assertExists()
        click("Dilution")
        field("Stock concentration (C₁)").performTextInput("7")
        click("RPD")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("18.18%").assertExists()
        field("Original Sample Result").performTextReplacement("11")
        compose.onNodeWithText("18.18%").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
        click("Dilution")
        field("Stock concentration (C₁)").assertTextContains("7")
    }

    @Test
    fun fieldErrorsSurviveRestorationAndClearResetsThem() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("Calculate")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Enter the stock concentration.").assertExists()
        click("RPD")
        click("Dilution")
        compose.onNodeWithText("Enter the stock concentration.").assertExists()
        click("Clear all")
        compose.onNodeWithText("Enter the stock concentration.").assertDoesNotExist()
    }

    @Test
    fun oversizedPasteDoesNotBecomeATruncatedCalculation() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        field("Stock concentration (C₁)").performTextInput("9".repeat(257))
        compose.onNodeWithText("Input was not accepted. Use at most 256 characters.").assertExists()
        click("Calculate")
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
    }
}
