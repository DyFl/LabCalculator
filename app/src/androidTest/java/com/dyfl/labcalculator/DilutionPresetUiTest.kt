package com.dyfl.labcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.presets.CalculatorPreset
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.PresetCodec
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

class DilutionPresetUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferencesName = "test_dilution_status_${UUID.randomUUID()}"
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val store = CalculatorPresetStore(context, preferencesName)
    private var renderedDensity: Density? = null

    @After fun cleanUp() { preferences.edit().clear().commit() }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun app(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            renderedDensity = LocalDensity.current
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme { LabCalculatorApp() }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun textFits() {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.forEach { layout ->
                assertFalse("Clipped text: ${layout.layoutInput.text}",
                    layout.multiParagraph.didExceedMaxLines ||
                        (0 until layout.lineCount).any { line ->
                            layout.getLineLeft(line) < -1f ||
                                layout.getLineRight(line) > layout.size.width + 1f ||
                                layout.getLineBottom(line) > layout.size.height + 1f
                        })
            }
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        textFits()
        val directory = File(context.getExternalFilesDir(null), "dilution-preset-output").apply { mkdirs() }
        val density = checkNotNull(renderedDensity)
        File(directory, "viewport.txt").writeText(
            "UI viewport: ${compose.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.size.width } / density.density}dp wide; " +
                "density=${density.density}; fontScale=${density.fontScale}")
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun repeatingPreparationCopiesRestoresAndClearsWithExactWorking() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "Before")) }
        field("Stock concentration (C₁)").performTextInput("3")
        // Default target is PPB, so 1000 PPB = 1 PPM.
        field("Final concentration (C₂)").performTextInput("1000")
        field("Final solution volume (V₂)").performTextInput("1")
        click("Calculate")
        val instruction = "Transfer ≈ 333.333 µL of stock and make up to 1 mL final solution volume."
        compose.onNodeWithText(instruction).assertIsDisplayed()
        screenshot("repeating-preparation")
        compose.onNodeWithContentDescription("Copy Preparation").performClick()
        compose.waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == instruction }
        click("Calculation Steps")
        compose.onNodeWithText("Exact stock transfer: 1000/3 µL", substring = true)
            .performScrollTo().assertIsDisplayed()
        screenshot("repeating-exact-steps")
        click("RPD")
        click("Dilution")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(instruction).assertExists()
        compose.onNodeWithText("Exact stock transfer: 1000/3 µL", substring = true).assertExists()
        field("Final solution volume (V₂)").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText(instruction).assertDoesNotExist()
        compose.onNodeWithText("Calculation Steps").assertDoesNotExist()
    }

    @Test fun unavailableAndUnreadableStorageNeverClaimsToBeEmpty() {
        val future = "2|future|Future|DILUTION|10|PPM|200|PPB|50"
        preferences.edit().putStringSet("v1_DILUTION", setOf(future)).commit()
        app()
        compose.onNodeWithText("No saved presets").assertDoesNotExist()
        compose.onNodeWithText("Saved presets are stored, but none can be loaded by this app.").assertExists()
        compose.onNodeWithText("Load preset").assertIsNotEnabled()
        screenshot("unavailable-presets")
        compose.runOnIdle { preferences.edit().putString("v1_DILUTION", "unexpected").commit() }
        compose.onNodeWithText("Saved preset storage could not be read. Existing data has been kept.").assertExists()
        compose.onNodeWithText("No saved presets").assertDoesNotExist()
        screenshot("unreadable-presets")
        assertEquals("unexpected", preferences.all["v1_DILUTION"])
    }

    @Test fun availablePresetsStillLoadAndDeleteWhileUnavailableRecordsRemain() {
        val future = "2|future|Future|DILUTION|10|PPM|200|PPB|50"
        val preset = CalculatorPreset("daily-id", "Daily", PresetSettings.Dilution(
            DilutionInput("10", ConcentrationUnit.PPM, "125", ConcentrationUnit.PPB, "50")))
        preferences.edit().putStringSet("v1_DILUTION", setOf(future, PresetCodec.encode(preset))).commit()
        app()
        val partial = "Some saved presets cannot be loaded by this app. Available presets can still be used."
        compose.onNodeWithText(partial).assertExists()
        compose.onNodeWithText("Load preset").assertIsEnabled()
        screenshot("partial-presets")
        click("Load preset")
        compose.onNode(hasText(partial) and hasAnyAncestor(isDialog())).assertExists()
        screenshot("partial-presets-dialog")
        compose.onNodeWithText("Daily").performClick()
        click("Calculate")
        compose.onNodeWithText("Transfer 625 µL of stock and make up to 50 mL final solution volume.").assertExists()
        click("Load preset")
        compose.onNodeWithContentDescription("Delete Daily").performClick()
        compose.onNode(hasText("Saved presets are stored, but none can be loaded by this app.") and
            hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("No saved presets").assertDoesNotExist()
        assertEquals(setOf(future), preferences.getStringSet("v1_DILUTION", null))
    }
}
