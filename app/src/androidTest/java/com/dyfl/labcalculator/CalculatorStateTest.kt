package com.dyfl.labcalculator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import org.junit.Rule
import org.junit.Test

class CalculatorStateTest {
    @get:Rule val compose = createComposeRule()

    private fun field(label: String) = compose.onNode(
        hasSetTextAction() and (hasText(label) or hasContentDescription(label)))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun select(label: String, option: String) {
        compose.onNodeWithContentDescription(label).performScrollTo().performClick()
        compose.onNode(hasText(option) and hasAnyAncestor(isPopup())).performClick()
    }
    private fun assertEmpty(label: String) = field(label).assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))

    @Test
    fun formulaDetailsAreOptionalAndExpansionSurvivesRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        compose.onNodeWithText("C₁V₁ = C₂V₂").assertDoesNotExist()
        compose.onNodeWithText("Total prepared solution volume, including the stock.").assertExists()
        field("Stock concentration (C₁)").performTextInput("10")
        click("Formula and assumptions")
        compose.onNodeWithText("C₁V₁ = C₂V₂").assertExists()
        click("RPD")
        compose.onNodeWithText("RPD (%) = |Original − Replicate|").assertDoesNotExist()
        click("Dilution")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("C₁V₁ = C₂V₂").assertExists()
        field("Stock concentration (C₁)").assertTextContains("10")
        click("Formula and assumptions")
        compose.onNodeWithText("C₁V₁ = C₂V₂").assertDoesNotExist()
    }

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

    @Test
    fun dilutionStockCorrectionClearsTargetErrorWithoutCalculating() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        field("Stock concentration (C₁)").performTextInput("1")
        field("Final concentration (C₂)").performTextInput("2000")
        field("Final solution volume (V₂)").performTextInput("50")
        click("Calculate")
        compose.onNodeWithText("Final concentration cannot exceed the stock concentration.").assertExists()

        field("Stock concentration (C₁)").performScrollTo().performTextReplacement("3")
        compose.onNodeWithText("Final concentration cannot exceed the stock concentration.").assertDoesNotExist()
        compose.onNodeWithTag("Result card: Volume from stock (V₁)").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
        click("Calculate")
        compose.onNodeWithText("33.333R mL").assertExists()
        field("Stock concentration (C₁)").performScrollTo().performTextReplacement("4")
        compose.onNodeWithText("33.333R mL").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
    }

    @Test
    fun dilutionConcentrationEditsAndUnitChangesPreserveUnrelatedErrors() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("Calculate")
        field("Stock concentration (C₁)").performScrollTo().performTextReplacement("3")
        compose.onNodeWithText("Enter the stock concentration.").assertDoesNotExist()
        compose.onNodeWithText("Enter the final concentration.").assertExists()
        compose.onNodeWithText("Enter the final solution volume.").assertExists()
        select("Unit for Stock concentration (C₁)", "PPB (parts per billion)")
        compose.onNodeWithText("Enter the final concentration.").assertExists()
        compose.onNodeWithText("Enter the final solution volume.").assertExists()
        select("Unit for Final concentration (C₂)", "PPM (parts per million)")
        compose.onNodeWithText("Enter the final solution volume.").assertExists()
        field("Stock concentration (C₁)").assertTextContains("3")
    }

    @Test
    fun conversionCategoryChangesClearInputResultsAndSteps() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("Unit conversions")
        for ((category, result) in listOf("Volume" to "25000 µg",
                "Mass concentration" to "25000 µL", "Mass" to "25000 ng/L")) {
            field("Value to convert").performScrollTo().performTextReplacement("25")
            click("Calculate")
            compose.onNodeWithText(result).assertExists()
            compose.onNodeWithText("Calculation Steps").assertExists()
            select("Category", category)
            assertEmpty("Value to convert")
            compose.onNodeWithText(result).assertDoesNotExist()
            compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
        }
    }

    @Test
    fun conversionCategoryChangesClearInvalidInputAndErrors() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("Unit conversions")
        field("Value to convert").performScrollTo().performTextInput("invalid")
        click("Calculate")
        compose.onNodeWithText("Enter a valid number", substring = true).assertExists()
        select("Category", "Volume")
        assertEmpty("Value to convert")
        compose.onNodeWithText("Enter a valid number", substring = true).assertDoesNotExist()
    }

    @Test
    fun conversionCategoryReselectionPreservesCustomPairInputResultsAndErrors() {
        compose.setContent { LabCalculatorTheme { LabCalculatorApp() } }
        click("Unit conversions")
        select("Starting unit", "grams (g)")
        select("Destination unit", "milligrams (mg)")
        field("Value to convert").performScrollTo().performTextInput("2")
        click("Calculate")
        select("Category", "Mass")
        compose.onNodeWithContentDescription("Starting unit").assertTextContains("grams (g)")
        compose.onNodeWithContentDescription("Destination unit").assertTextContains("milligrams (mg)")
        field("Value to convert").assertTextContains("2")
        compose.onNodeWithText("2000 mg").assertExists()
        compose.onNodeWithText("Calculation Steps").assertExists()

        compose.onNodeWithContentDescription("Swap units").performScrollTo().performClick()
        field("Value to convert").assertTextContains("2")
        compose.onNodeWithText("2000 mg").assertDoesNotExist()
        click("Calculate")
        compose.onNodeWithText("0.002 g").assertExists()
        select("Category", "Mass")
        compose.onNodeWithContentDescription("Starting unit").assertTextContains("milligrams (mg)")
        compose.onNodeWithContentDescription("Destination unit").assertTextContains("grams (g)")
        compose.onNodeWithText("0.002 g").assertExists()

        field("Value to convert").performScrollTo().performTextReplacement("invalid")
        click("Calculate")
        select("Category", "Mass")
        field("Value to convert").assertTextContains("invalid")
        compose.onNodeWithText("Enter a valid number", substring = true).assertExists()
    }
}
