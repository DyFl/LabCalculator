package com.dyfl.labcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.presets.*
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConcentrationUnitsUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferencesName = "test_units_${UUID.randomUUID()}"
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val store = CalculatorPresetStore(context, preferencesName)
    private var renderedDensity: Density? = null

    @After fun cleanUp() { preferences.edit().clear().commit() }

    private fun app(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            renderedDensity = LocalDensity.current
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme { LabCalculatorApp() }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }
    private fun field(label: String) = compose.onNode(hasSetTextAction() and (hasContentDescription(label) or hasText(label)))
    private fun value(label: String, text: String) = field(label).performScrollTo().performTextReplacement(text)
    private fun expect(label: String, text: String) = field(label).assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text)))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun select(label: String, option: String) {
        compose.onNodeWithContentDescription(label).performScrollTo().performClick()
        compose.onNode(hasText(option) and hasAnyAncestor(isPopup())).performClick()
    }
    private fun msValues() {
        value("Raw diluted source-sample result", "5")
        value("Sample dilution factor", "10")
        value("Final spike concentration added", "50")
        value("Literal MS result", "55")
        value("Literal MSD result", "50")
    }
    private fun textFitsAndScreenshot(name: String) {
        compose.waitForIdle()
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
        val directory = File(context.getExternalFilesDir(null), "concentration-units").apply { mkdirs() }
        val density = checkNotNull(renderedDensity)
        File(directory, "viewport.txt").writeText("Width=" +
            "${compose.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.size.width } / density.density}dp; fontScale=${density.fontScale}")
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun dilutionCompatibleChangesPreserveQuantityClearResultsAndRestore() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        value("Stock concentration (C₁)", "10")
        value("Final concentration (C₂)", "200")
        value("Final solution volume (V₂)", "50")
        click("Calculate")
        select("Unit for Stock concentration (C₁)", "PPB (parts per billion)")
        expect("Stock concentration (C₁)", "10000")
        compose.onNodeWithTag("Result card: Preparation").assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
        select("Unit for Final concentration (C₂)", "PPM (parts per million)")
        expect("Final concentration (C₂)", "0.2")
        click("RPD")
        click("Dilution")
        restoration.emulateSavedInstanceStateRestore()
        expect("Stock concentration (C₁)", "10000")
        expect("Final concentration (C₂)", "0.2")
        click("Calculate")
        compose.onNodeWithText("Transfer 1 mL of stock and make up to 50 mL final solution volume.").assertExists()
    }

    @Test fun dilutionFamilyResetIsExplicitRestorableAndKeepsVolumeErrors() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        value("Stock concentration (C₁)", "10")
        value("Final concentration (C₂)", "200")
        value("Final solution volume (V₂)", "0")
        click("Calculate")
        select("Unit for Stock concentration (C₁)", "mg/L (milligrams per liter)")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Clear values and change units?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        expect("Stock concentration (C₁)", "10")
        select("Unit for Stock concentration (C₁)", "mg/L (milligrams per liter)")
        textFitsAndScreenshot("dilution-family-reset")
        compose.onNodeWithText("Clear and change").performClick()
        expect("Stock concentration (C₁)", "")
        expect("Final concentration (C₂)", "")
        expect("Final solution volume (V₂)", "0")
        compose.onNodeWithText("Final solution volume must be greater than zero.").assertExists()
        select("Unit for Final concentration (C₂)", "µg/L (micrograms per liter)")
        value("Stock concentration (C₁)", "10")
        value("Final concentration (C₂)", "200")
        value("Final solution volume (V₂)", "50")
        click("Calculate")
        textFitsAndScreenshot("dilution-mass-result")
        val instruction = "Transfer 1 mL of stock and make up to 50 mL final solution volume."
        compose.onNodeWithText(instruction).assertExists()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "Before")) }
        compose.onNodeWithContentDescription("Copy Preparation").performScrollTo().performClick()
        compose.waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == instruction }
        click("Calculation Steps")
        compose.onNodeWithText("Divide and cancel µg/L", substring = true).assertExists()
    }

    @Test fun dilutionInvalidInputBlocksUnitChangeAndKeepsUnrelatedErrors() {
        app()
        value("Stock concentration (C₁)", "bad")
        value("Final concentration (C₂)", "200")
        click("Calculate")
        select("Unit for Stock concentration (C₁)", "PPB (parts per billion)")
        expect("Stock concentration (C₁)", "bad")
        compose.onNodeWithContentDescription("Unit for Stock concentration (C₁)").assertTextContains("PPM")
        compose.onNodeWithText("Units unchanged.", substring = true).assertExists()
        compose.onNodeWithText("Enter the final solution volume.").assertExists()
        value("Stock concentration (C₁)", "10")
        select("Unit for Stock concentration (C₁)", "PPB (parts per billion)")
        expect("Stock concentration (C₁)", "10000")
        compose.onNodeWithText("Enter the final solution volume.").assertExists()
    }

    @Test fun sharedMsMsdUnitConvertsAllConcentrationsAndNextSampleKeepsPreparation() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues()
        click("Calculate")
        select("Shared concentration unit", "PPM (parts per million)")
        expect("Raw diluted source-sample result", "0.005")
        expect("Final spike concentration added", "0.05")
        expect("Literal MS result", "0.055")
        expect("Literal MSD result", "0.05")
        expect("Sample dilution factor", "10")
        compose.onAllNodesWithContentDescription("Unit for", substring = true).assertCountEquals(0)
        compose.onNodeWithTag("Result card: Original source concentration").assertDoesNotExist()
        click("Dilution")
        click("MS/MSD")
        restoration.emulateSavedInstanceStateRestore()
        expect("Literal MS result", "0.055")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPM")
        click("Calculate")
        compose.onNodeWithText("0.05 PPM").assertExists()
        compose.onNodeWithText("100.00%").assertExists()
        compose.onNodeWithText("90.00%").assertExists()
        compose.onNodeWithText("9.52%").assertExists()
        click("Next sample")
        expect("Raw diluted source-sample result", "")
        expect("Literal MS result", "")
        expect("Literal MSD result", "")
        expect("Final spike concentration added", "0.05")
        expect("Sample dilution factor", "10")
        click("Clear all")
        expect("Final spike concentration added", "")
        expect("Sample dilution factor", "1")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPB")
    }

    @Test fun sharedMsMsdConversionIsAtomicForInvalidFieldsAndPreservesBlankAndFactorErrors() {
        app()
        click("MS/MSD")
        value("Sample dilution factor", "0")
        value("Final spike concentration added", "50")
        value("Literal MS result", "bad")
        value("Literal MSD result", "50")
        click("Calculate")
        select("Shared concentration unit", "PPM (parts per million)")
        expect("Final spike concentration added", "50")
        expect("Literal MS result", "bad")
        expect("Literal MSD result", "50")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPB")
        compose.onNodeWithText("Units unchanged.", substring = true).assertExists()
        value("Literal MS result", "55")
        select("Shared concentration unit", "PPM (parts per million)")
        expect("Raw diluted source-sample result", "")
        expect("Final spike concentration added", "0.05")
        expect("Literal MS result", "0.055")
        expect("Literal MSD result", "0.05")
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
        compose.onNodeWithText("Enter the raw diluted source-sample result.").assertExists()
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        compose.onNodeWithText("Clear and change").performClick()
        expect("Final spike concentration added", "")
        expect("Sample dilution factor", "0")
        compose.onNodeWithText("The sample dilution factor must be greater than zero.").assertExists()
    }

    @Test fun msMsdMassUnitsCopyRestoreAndRequireReentryAcrossFamilies() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        click("MS/MSD")
        msValues()
        click("Calculate")
        select("Shared concentration unit", "µg/L (micrograms per liter)")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("50 PPB").assertExists()
        select("Shared concentration unit", "µg/L (micrograms per liter)")
        textFitsAndScreenshot("ms-family-reset")
        compose.onNodeWithText("Clear and change").performClick()
        listOf("Raw diluted source-sample result", "Final spike concentration added", "Literal MS result", "Literal MSD result")
            .forEach { expect(it, "") }
        expect("Sample dilution factor", "10")
        msValues()
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        click("Calculate")
        compose.onNodeWithText("0.05 mg/L").assertExists()
        textFitsAndScreenshot("ms-mass-result")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "Before")) }
        compose.onNodeWithContentDescription("Copy Original source concentration").performScrollTo().performClick()
        compose.waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == "0.05 mg/L" }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("0.05 mg/L").assertExists()
        compose.onNodeWithText("Spike added after dilution.", substring = true).performScrollTo().assertExists()
        textFitsAndScreenshot("ms-mass-basis-guidance")
        select("Shared concentration unit", "µg/L (micrograms per liter)")
        compose.onNodeWithText("0.05 mg/L").assertDoesNotExist()
        click("Calculate")
        compose.onNodeWithText("50 µg/L").assertExists()
    }

    @Test fun oldAndNewPresetsLoadWithoutReinterpretingUnitsAndPreserveUnavailableRecords() {
        val old = "1|old|Legacy|MS_MSD|10|50|PPB"
        val future = "2|future|Future|MS_MSD|1|50|PPB"
        preferences.edit().putStringSet("v1_MS_MSD", setOf(old, future)).commit()
        app()
        click("MS/MSD")
        click("Load preset")
        compose.onNodeWithText("Legacy").performClick()
        expect("Final spike concentration added", "50")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("PPB")
        msValues()
        select("Shared concentration unit", "mg/L (milligrams per liter)")
        compose.onNodeWithText("Clear and change").performClick()
        value("Final spike concentration added", "0.05")
        click("Save preset")
        field("Preset name").performTextInput("Mass daily")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { store.load(PresetKind.MS_MSD).presets.size == 2 }
        compose.waitUntil { compose.onAllNodesWithText("Preset name").fetchSemanticsNodes().isEmpty() }
        click("Clear all")
        click("Load preset")
        compose.onNodeWithText("Mass daily").performClick()
        expect("Final spike concentration added", "0.05")
        expect("Sample dilution factor", "10")
        expect("Raw diluted source-sample result", "")
        compose.onNodeWithContentDescription("Shared concentration unit").assertTextContains("mg/L")
        assertTrue(preferences.getStringSet("v1_MS_MSD", emptySet())!!.containsAll(setOf(old, future)))
        assertTrue(store.load(PresetKind.MS_MSD) is PresetLoadResult.PartiallyAvailable)
    }

    @Test fun molarityAndConverterPreserveQuantitiesAndRejectInvalidUnitChanges() {
        app()
        click("Molarity")
        value("Desired Molarity", "0.02")
        value("Final Solution Volume", "250")
        value("Formula Weight", "58.44")
        click("Calculate")
        select("Unit for Final Solution Volume", "L")
        expect("Final Solution Volume", "0.25")
        compose.onNodeWithText("0.29220 g").assertDoesNotExist()
        click("Calculate")
        compose.onNodeWithText("0.29220 g").assertExists()
        value("Final Solution Volume", "bad")
        select("Unit for Final Solution Volume", "mL")
        expect("Final Solution Volume", "bad")
        compose.onNodeWithContentDescription("Unit for Final Solution Volume").assertTextContains("L")
        click("Unit conversions")
        value("Value to convert", "2")
        click("Calculate")
        select("Starting unit", "grams (g)")
        expect("Value to convert", "0.002")
        compose.onNodeWithText("2000 µg").assertDoesNotExist()
        compose.onNodeWithContentDescription("Swap units").performScrollTo().performClick()
        expect("Value to convert", "2000")
        click("Calculate")
        compose.onNodeWithText("0.002 g").assertExists()
        value("Value to convert", "bad")
        click("Calculate")
        compose.onNodeWithContentDescription("Swap units").performScrollTo().performClick()
        expect("Value to convert", "bad")
        compose.onNodeWithContentDescription("Starting unit").assertTextContains("micrograms (µg)")
        select("Destination unit", "milligrams (mg)")
        compose.onNodeWithText("Enter a valid number", substring = true).assertExists()
        select("Category", "Volume")
        compose.onNodeWithText("Cancel").performClick()
        expect("Value to convert", "bad")
        select("Category", "Volume")
        compose.onNodeWithText("Clear and change").performClick()
        expect("Value to convert", "")
        compose.onNodeWithText("Enter a valid number", substring = true).assertDoesNotExist()
    }
}
