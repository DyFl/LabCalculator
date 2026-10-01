package com.dyfl.labcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.MsMsdCalculator
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WorkflowCleanupTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferencesName = "test_cleanup_${UUID.randomUUID()}"
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val store = CalculatorPresetStore(context, preferencesName)
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var dark by mutableStateOf(false)
    private var renderedDensity: Density? = null
    private val measurements = listOf("Raw diluted source-sample result", "Literal MS result", "Literal MSD result")
    private val concentrations = measurements + "Final spike concentration added"
    private val summary = listOf("Original source concentration" to "50 PPB",
        "MS recovery" to "100.00%", "MSD recovery" to "90.00%", "MS/MSD RPD" to "9.52%")

    @After fun cleanUp() { preferences.edit().clear().commit() }

    private fun app(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            renderedDensity = LocalDensity.current
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme(darkTheme = dark) { LabCalculatorApp() }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }
    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))
    private fun value(label: String, value: String) = field(label).performScrollTo().performTextReplacement(value)
    private fun expect(label: String, value: String) = field(label).assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun select(label: String, option: String) {
        compose.onNodeWithContentDescription(label).performScrollTo().performClick()
        compose.onNode(hasText(option) and hasAnyAncestor(isPopup())).performClick()
    }
    private fun msValues(ms: String = "55", msd: String = "50") {
        value("Sample dilution factor", "10")
        value("Final spike concentration added", "50")
        value(measurements[0], "5")
        value(measurements[1], ms)
        value(measurements[2], msd)
    }
    private fun assertNoResults() {
        compose.onNodeWithTag("MS/MSD summary").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
    }
    private fun assertSourceReady() {
        field(measurements[0]).assertIsFocused().assertIsDisplayed()
        val viewport = compose.onNodeWithTag("Calculator scroll").fetchSemanticsNode().boundsInRoot
        val source = field(measurements[0]).fetchSemanticsNode().boundsInRoot
        val heading = compose.onNodeWithText("Uncorrected measurements").fetchSemanticsNode().boundsInRoot
        assertTrue("Source field must be entirely visible after Next sample", source.top >= viewport.top - 1f &&
            source.bottom <= viewport.bottom + 1f)
        assertTrue("Measurement heading must be visible above the wrapped floating label",
            heading.top >= viewport.top - 1f && heading.bottom <= viewport.bottom + 1f)
    }
    private fun copy(label: String, expected: String) {
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "Before")) }
        compose.onNodeWithContentDescription("Copy $label").performScrollTo().assertIsEnabled()
            .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        compose.waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == expected }
    }

    @Test fun nextSampleKeepsPreparationErrorsClearsMeasurementErrorsAndFocusesSource() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        value("Sample dilution factor", "0")
        value("Final spike concentration added", "0")
        measurements.forEach { value(it, "bad") }
        click("Calculate")
        compose.onAllNodesWithText("Enter a valid", substring = true).assertCountEquals(3)
        click("Next sample")
        assertSourceReady()
        measurements.forEach { expect(it, "") }
        expect("Sample dilution factor", "0")
        expect("Final spike concentration added", "0")
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
        compose.onNodeWithText("The final spike concentration must be greater than zero.").assertExists()
        compose.onAllNodesWithText("Enter a valid", substring = true).assertCountEquals(0)
        assertNoResults()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
        compose.onNodeWithText("The final spike concentration must be greater than zero.").assertExists()
        measurements.forEach { expect(it, "") }
    }

    @Test fun nextSampleAlsoRetainsInvalidPreparationTextAndItsErrors() {
        app()
        click("MS/MSD")
        value("Sample dilution factor", "bad")
        value("Final spike concentration added", "bad")
        click("Calculate")
        click("Next sample")
        assertSourceReady()
        expect("Sample dilution factor", "bad")
        expect("Final spike concentration added", "bad")
        compose.onAllNodesWithText("Enter a valid", substring = true).assertCountEquals(2)
        compose.onNodeWithText("Enter the raw diluted source-sample result.").assertDoesNotExist()
        compose.onNodeWithText("Enter the literal MS result.").assertDoesNotExist()
        compose.onNodeWithText("Enter the literal MSD result.").assertDoesNotExist()
    }

    @Test fun loadingPresetAndClearAllClearObsoletePreparationAndMeasurementErrors() {
        store.save("Daily", PresetSettings.MsMsd("10", "0.05", ConcentrationUnit.MILLIGRAM_PER_LITER))
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues()
        click("Calculate")
        click("Load preset")
        compose.onNodeWithText("Daily").performClick()
        assertNoResults()
        msValues()
        click("Calculate")
        value("Sample dilution factor", "0")
        value("Final spike concentration added", "bad")
        value(measurements[1], "bad")
        click("Calculate")
        select("Shared concentration unit", "µg/L (micrograms per liter)")
        compose.onNodeWithText("Units unchanged.", substring = true).assertExists()
        click("Load preset")
        compose.onNodeWithText("Daily").performClick()
        expect("Sample dilution factor", "10")
        expect("Final spike concentration added", "0.05")
        measurements.forEach { expect(it, "") }
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("mg/L")
        compose.onAllNodesWithText("Enter a valid", substring = true).assertCountEquals(0)
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertDoesNotExist()
        compose.onNodeWithText("Units unchanged.", substring = true).assertDoesNotExist()
        assertNoResults()
        restoration.emulateSavedInstanceStateRestore()
        expect("Final spike concentration added", "0.05")
        value("Sample dilution factor", "0")
        value("Final spike concentration added", "0")
        value(measurements[0], "bad")
        click("Calculate")
        click("Next sample")
        click("Clear all")
        expect("Sample dilution factor", "1")
        concentrations.forEach { expect(it, "") }
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPB")
        compose.onAllNodesWithText("must be greater than zero.", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Enter a valid", substring = true).assertCountEquals(0)
        assertNoResults()
        restoration.emulateSavedInstanceStateRestore()
        expect("Sample dilution factor", "1")
        compose.onAllNodesWithText("must be greater than zero.", substring = true).assertCountEquals(0)
        msValues()
        click("Calculate")
        click("Clear all")
        assertNoResults()
    }

    @Test fun summaryShowsCopiesAndRestoresAllFourResultsThenClearsOnUnitChangeAndNextSample() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues()
        field(measurements[2]).performImeAction()
        compose.onNodeWithText("50 PPB").assertIsDisplayed()
        compose.onAllNodesWithTag("MS/MSD summary").assertCountEquals(1)
        summary.forEach { (label, expected) ->
            compose.onNode(hasText(expected) and hasAnyAncestor(hasTestTag("Result card: $label"))).assertExists()
            copy(label, expected)
        }
        compose.onNodeWithText("RPD of measured concentrations.").assertExists()
        click("Calculation Steps")
        click("Dilution")
        click("MS/MSD")
        restoration.emulateSavedInstanceStateRestore()
        summary.forEach { (_, expected) -> compose.onNodeWithText(expected).assertExists() }
        compose.onNodeWithText("Calculation Steps").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        select("Shared concentration unit", "PPM (parts per million)")
        assertNoResults()
        click("Calculate")
        compose.onNodeWithText("0.05 PPM").assertIsDisplayed()
        click("Next sample")
        assertSourceReady()
        assertNoResults()
        expect("Final spike concentration added", "0.05")
        expect("Sample dilution factor", "10")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPM")
    }

    @Test fun undefinedRpdExplainsAndDisablesOnlyItsCopyAndRestoresThenClearsOnEdit() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues(ms = "0", msd = "0")
        click("Calculate")
        copy("Original source concentration", "50 PPB")
        copy("MS recovery", "-10.00%")
        copy("MSD recovery", "-10.00%")
        compose.onNodeWithText("Undefined").assertExists()
        compose.onNodeWithText(MsMsdCalculator.UNDEFINED_RPD_MESSAGE).assertExists()
        compose.onNodeWithContentDescription("Copy MS/MSD RPD").performScrollTo().assertIsNotEnabled()
            .performTouchInput { click() }
        assertEquals("-10.00%", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(MsMsdCalculator.UNDEFINED_RPD_MESSAGE).assertExists()
        compose.onNodeWithContentDescription("Copy MS/MSD RPD").assertIsNotEnabled()
        value(measurements[2], "1")
        assertNoResults()
    }

    @Test fun emptyDilutionFamilyChangesKeepVolumeAndErrorsAndCheckBothAffectedFields() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        value("Stock concentration (C₁)", " ")
        value("Final concentration (C₂)", "  ")
        value("Final solution volume (V₂)", "0")
        click("Calculate")
        select("Unit for Stock concentration (C₁)", "mg/L (milligrams per liter)")
        compose.onNodeWithText("Clear values and change units?").assertDoesNotExist()
        listOf("Stock concentration (C₁)", "Final concentration (C₂)").forEach { expect(it, "") }
        expect("Final solution volume (V₂)", "0")
        compose.onNodeWithText("Final solution volume must be greater than zero.").assertExists()
        compose.onNodeWithText("Enter the stock concentration.").assertDoesNotExist()
        compose.onNodeWithText("Enter the final concentration.").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Unit for Stock concentration (C₁)").assertTextContains("mg/L")
        compose.onNodeWithContentDescription("Unit for Final concentration (C₂)").assertTextContains("mg/L")
        // Selected stock is empty, but resetting would discard invalid target text.
        value("Final concentration (C₂)", "bad")
        select("Unit for Stock concentration (C₁)", "PPM (parts per million)")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Clear values and change units?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        expect("Final concentration (C₂)", "bad")
        compose.onNodeWithContentDescription("Unit for Stock concentration (C₁)").assertTextContains("mg/L")
        select("Unit for Stock concentration (C₁)", "PPM (parts per million)")
        compose.onNodeWithText("Clear and change").performClick()
        expect("Final concentration (C₂)", "")
        expect("Final solution volume (V₂)", "0")
        compose.onNodeWithText("Final solution volume must be greater than zero.").assertExists()
        select("Unit for Final concentration (C₂)", "µg/L (micrograms per liter)")
        compose.onNodeWithText("Clear values and change units?").assertDoesNotExist()
        compose.onNodeWithContentDescription("Unit for Stock concentration (C₁)").assertTextContains("µg/L")
    }

    @Test fun emptyMsFamilyChangeKeepsFactorErrorAndEveryNonblankConcentrationRequiresConfirmation() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        value("Final spike concentration added", " ")
        value(measurements[0], "  ")
        value("Sample dilution factor", "0")
        click("Calculate")
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        compose.onNodeWithText("Clear values and change units?").assertDoesNotExist()
        expect("Sample dilution factor", "0")
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
        compose.onNodeWithText("Enter the final spike concentration.").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("mg/L")
        for (label in concentrations) {
            value(label, "bad")
            select("Shared concentration unit", "PPB (parts per billion)")
            compose.onNodeWithText("Clear values and change units?").assertExists()
            compose.onNodeWithText("Cancel").performClick()
            expect(label, "bad")
            compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("mg/L")
            value(label, "")
        }
        select("Shared concentration unit", "PPB (parts per billion)")
        compose.onNodeWithText("Clear values and change units?").assertDoesNotExist()
        concentrations.forEach { expect(it, "") }
        expect("Sample dilution factor", "0")
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
    }

    @Test fun populatedMsFamilyCancelPreservesSummaryAndConfirmedResetClearsRestorableResults() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues()
        click("Calculate")
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cancel").performClick()
        summary.forEach { (_, expected) -> compose.onNodeWithText(expected).assertExists() }
        expect(measurements[0], "5")
        expect("Final spike concentration added", "50")
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        compose.onNodeWithText("Clear and change").performClick()
        assertNoResults()
        concentrations.forEach { expect(it, "") }
        expect("Sample dilution factor", "10")
        restoration.emulateSavedInstanceStateRestore()
        assertNoResults()
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("mg/L")
    }

    @Test fun converterSkipsEmptyResetButConfirmsInvalidAndPopulatedInputWithRestorableCancellation() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("Unit conversions")
        value("Value to convert", " ")
        click("Calculate")
        select("Category", "Volume")
        compose.onNodeWithText("Clear values and change units?").assertDoesNotExist()
        expect("Value to convert", "")
        compose.onNodeWithText("Enter a value to convert.").assertDoesNotExist()
        compose.onNodeWithContentDescription("Category").assertTextContains("Volume")
        value("Value to convert", "bad")
        click("Calculate")
        select("Category", "Mass")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cancel").performClick()
        expect("Value to convert", "bad")
        compose.onNodeWithText("Enter a valid number", substring = true).assertExists()
        compose.onNodeWithContentDescription("Category").assertTextContains("Volume")
        select("Category", "Mass")
        compose.onNodeWithText("Clear and change").performClick()
        expect("Value to convert", "")
        compose.onNodeWithText("Enter a valid number", substring = true).assertDoesNotExist()
        value("Value to convert", "2")
        click("Calculate")
        select("Category", "Mass concentration")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("2000 µg").assertExists()
        expect("Value to convert", "2")
        select("Category", "Mass concentration")
        compose.onNodeWithText("Clear and change").performClick()
        compose.onNodeWithText("2000 µg").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        expect("Value to convert", "")
        compose.onNodeWithContentDescription("Category").assertTextContains("Mass concentration")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        // Compose idleness does not include the platform dialog window's entrance animation.
        SystemClock.sleep(350)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.forEach { layout ->
                assertFalse("Clipped text: ${layout.layoutInput.text}", layout.multiParagraph.didExceedMaxLines ||
                    (0 until layout.lineCount).any { line ->
                        layout.getLineLeft(line) < -1f || layout.getLineRight(line) > layout.size.width + 1f ||
                            layout.getLineBottom(line) > layout.size.height + 1f
                    })
            }
        }
        val directory = File(context.getExternalFilesDir(null), "workflow-cleanup").apply { mkdirs() }
        val density = checkNotNull(renderedDensity)
        File(directory, "viewport.txt").writeText("Width=" +
            "${compose.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.size.width } / density.density}dp; " +
            "density=${density.density}; fontScale=${density.fontScale}")
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun lightAndDarkWorkflowLayoutsWrapWithAccessibleCopyTargets() {
        app()
        click("MS/MSD")
        for (theme in listOf(false, true)) {
            compose.runOnIdle { dark = theme }
            val mode = if (theme) "dark" else "light"
            click("Clear all")
            compose.onNodeWithText("All concentrations are in PPB.", substring = true).performScrollTo()
            screenshot("preparation-$mode")
            compose.onNodeWithText("Uncorrected measurements").performScrollTo()
            screenshot("measurements-$mode")
            msValues()
            click("Calculate")
            compose.onNodeWithText("50 PPB").assertIsDisplayed()
            screenshot("summary-top-$mode")
            compose.onNodeWithText("RPD of measured concentrations.").performScrollTo()
            screenshot("summary-bottom-$mode")
            summary.forEach { (label, _) -> compose.onNodeWithContentDescription("Copy $label")
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp) }
            value(measurements[0], "123456789123456789123456789")
            click("Calculate")
            screenshot("summary-long-value-$mode")
            msValues(ms = "0", msd = "0")
            click("Calculate")
            compose.onNodeWithText(MsMsdCalculator.UNDEFINED_RPD_MESSAGE).performScrollTo()
            screenshot("undefined-rpd-$mode")
            click("Next sample")
            assertSourceReady()
            screenshot("next-sample-$mode")
            value("Sample dilution factor", "0")
            value("Final spike concentration added", "0")
            click("Calculate")
            click("Next sample")
            compose.onNodeWithText("The final spike concentration must be greater than zero.").performScrollTo()
            screenshot("retained-errors-$mode")
            select("Shared concentration unit", "mg/L (milligrams per liter)")
            screenshot("reset-dialog-$mode")
            compose.onNodeWithText("Cancel").performClick()
            if (compose.onNodeWithText("Formula and assumptions").fetchSemanticsNode()
                    .config[SemanticsProperties.StateDescription] == "Collapsed") {
                click("Formula and assumptions")
            }
            compose.onNodeWithText("Compatible unit changes", substring = true).performScrollTo()
            screenshot("unit-help-$mode")
        }
    }
}
