package com.dyfl.labcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BenchWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "test_workflow_${UUID.randomUUID()}"
    private val store = CalculatorPresetStore(context, preferencesName)

    @After
    fun cleanUp() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun app() {
        compose.setContent {
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme { LabCalculatorApp() }
            }
        }
    }

    private fun field(label: String) = compose.onNode(
        hasSetTextAction() and (hasText(label) or hasContentDescription(label)))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun assertEmpty(label: String) = field(label).assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))

    @Test
    fun copyIncludesUnitsAndWorkingExpandsOnRequest() {
        app()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "Before copying")) }
        field("Stock concentration (C₁)").performTextInput("10")
        field("Final concentration (C₂)").performTextInput("200")
        field("Final solution volume (V₂)").performTextInput("50")
        click("Calculate")
        compose.onNodeWithText("1 mL").assertExists()
        compose.onNodeWithText("Final Volume from stock", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Copy Volume from stock (V₁)")
            .performScrollTo().performClick()
        compose.waitUntil {
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() == "1 mL"
        }
        click("Calculation Steps")
        compose.onNodeWithText("Final Volume from stock", substring = true).assertExists()
    }

    @Test
    fun savedDilutionRecipeLoadsInputsAndRequiresFreshCalculation() {
        app()
        field("Stock concentration (C₁)").performTextInput("10")
        field("Final concentration (C₂)").performTextInput("200")
        field("Final solution volume (V₂)").performTextInput("50")
        click("Calculate")
        click("Save preset")
        compose.onNodeWithText("Save concentrations and final volume as a preparation recipe.").assertExists()
        field("Preset name").performTextInput("Daily standard")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { store.load(PresetKind.DILUTION).size == 1 }
        compose.waitUntil { compose.onAllNodesWithText("Preset name").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        click("Clear all")
        click("Load preset")
        compose.onNodeWithText("Daily standard").performClick()
        field("Stock concentration (C₁)").assertTextContains("10")
        field("Final concentration (C₂)").assertTextContains("200")
        field("Final solution volume (V₂)").assertTextContains("50")
        compose.onNodeWithText("1 mL").assertDoesNotExist()
        click("Calculate")
        compose.onNodeWithText("1 mL").assertExists()
    }

    @Test
    fun nextSampleKeepsPreparationAndLoadedPresetsClearMeasurements() {
        store.save("Metals daily", PresetSettings.MsMsd("10", "50", ConcentrationUnit.PPM))
        app()
        click("MS/MSD")
        click("Load preset")
        compose.onNodeWithText("Metals daily").performClick()
        field("Raw diluted source-sample result").performTextInput("5")
        field("Literal MS result").performTextInput("55")
        field("Literal MSD result").performTextInput("50")
        click("Calculate")
        compose.onNodeWithText("50 PPM").assertExists()
        click("Next sample")
        assertEmpty("Raw diluted source-sample result")
        assertEmpty("Literal MS result")
        assertEmpty("Literal MSD result")
        field("Sample dilution factor").assertTextContains("10")
        field("Final spike concentration added").assertTextContains("50")
        compose.onNodeWithText("50 PPM").assertDoesNotExist()
        field("Literal MS result").performTextInput("1")
        click("Load preset")
        compose.onNodeWithText("Metals daily").performClick()
        assertEmpty("Literal MS result")
        assertEquals("10", (store.load(PresetKind.MS_MSD).single().settings as PresetSettings.MsMsd).dilutionFactor)
    }

    @Test
    fun undefinedRpdKeepsOtherResultsAndClearsOnEdit() {
        app()
        click("MS/MSD")
        field("Raw diluted source-sample result").performTextInput("5")
        field("Sample dilution factor").performTextReplacement("10")
        field("Final spike concentration added").performTextInput("50")
        field("Literal MS result").performTextInput("0")
        field("Literal MSD result").performTextInput("0")
        click("Calculate")
        compose.onNodeWithText("50 PPB").assertExists()
        compose.onAllNodesWithText("-10.00%").assertCountEquals(2)
        compose.onNodeWithText("Undefined").assertExists()
        compose.onNodeWithContentDescription("Copy MS/MSD RPD").assertIsNotEnabled()
        field("Literal MSD result").performTextReplacement("1")
        compose.onNodeWithText("Undefined").assertDoesNotExist()
        compose.onNodeWithText("50 PPB").assertDoesNotExist()
    }
}
