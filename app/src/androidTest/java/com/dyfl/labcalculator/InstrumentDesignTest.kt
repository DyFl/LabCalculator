package com.dyfl.labcalculator

import android.content.Context
import android.content.ClipboardManager
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.DilutionCalculator
import com.dyfl.labcalculator.calculation.DilutionResult
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Workflow regressions for the instrument redesign; screenshots use real production screens. */
class InstrumentDesignTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferencesName = "test_instrument_${UUID.randomUUID()}"
    private val store = CalculatorPresetStore(context, preferencesName)
    private var dark by mutableStateOf(false)
    private var viewportDescription = ""

    @After fun cleanUp() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun app(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            val density = LocalDensity.current
            viewportDescription = "density=${density.density}; fontScale=${density.fontScale}"
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme(darkTheme = dark) { LabCalculatorApp() }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun top() = compose.onNodeWithTag("Calculator heading").performScrollTo()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "instrument-design").apply { mkdirs() }
        File(directory, "viewport.txt").writeText(viewportDescription +
            "; widthPx=${compose.onRoot().fetchSemanticsNode().size.width}")
        if (captureScreenshotsEnabled()) File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun dilutionStatesInBothThemes() {
        app()
        listOf(false, true).forEach { isDark ->
            compose.runOnIdle { dark = isDark }
            val theme = if (isDark) "dark" else "light"
            top()
            screenshot("dilution-empty-$theme")
            click("Calculate")
            field("Stock concentration (C₁)").assertIsDisplayed()
            screenshot("dilution-error-$theme")
            click("Clear all")
            field("Stock concentration (C₁)").performTextReplacement("10")
            field("Final concentration (C₂)").performTextReplacement("200")
            field("Final solution volume (V₂)").performTextReplacement("50")
            top()
            screenshot("dilution-filled-$theme")
            field("Final solution volume (V₂)").performImeAction()
            compose.onNodeWithText("Transfer 1 mL of stock and make up to 50 mL final solution volume.")
                .assertIsDisplayed()
            compose.onNodeWithContentDescription("Copy Preparation")
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            screenshot("dilution-result-$theme")
            click("Clear all")
        }
    }

    @Test fun allCalculatorsMenuNavigatesAndRetainsIndependentInputs() {
        val restoration = StateRestorationTester(compose)
        app(restoration)
        field("Stock concentration (C₁)").performTextReplacement("10")
        compose.onNodeWithContentDescription("All calculators").performClick()
        compose.onNode(hasText("Molarity") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithTag("Tab: Molarity").assertIsSelected()
        field("Formula Weight").performTextReplacement("58.44")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("Tab: Molarity").assertIsSelected()
        compose.onNodeWithContentDescription("All calculators").performClick()
        compose.onNode(hasText("Dilution") and hasAnyAncestor(isPopup())).performClick()
        field("Stock concentration (C₁)").assertTextContains("10")
        compose.onNodeWithTag("Tab: Dilution").assertIsSelected()
    }

    @Test fun invalidCalculationRevealsFirstErrorAndEditingStillClearsResults() {
        app()
        field("Stock concentration (C₁)").performTextReplacement("10")
        field("Final concentration (C₂)").performTextReplacement("200")
        field("Final solution volume (V₂)").performTextReplacement("50")
        field("Final solution volume (V₂)").performImeAction()
        field("Stock concentration (C₁)").performScrollTo().performTextClearance()
        compose.onNodeWithTag("Result card: Preparation").assertDoesNotExist()
        click("Calculate")
        val viewport = compose.onNodeWithTag("Calculator scroll").fetchSemanticsNode().boundsInRoot
        val firstError = field("Stock concentration (C₁)").fetchSemanticsNode().boundsInRoot
        assertTrue("First invalid input should be visible after Calculate",
            firstError.top >= viewport.top - 1f && firstError.bottom <= viewport.bottom + 1f)
        field("Stock concentration (C₁)").assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test fun copyingStepsKeepsDetailsCollapsedAndIncludesExactWorking() {
        app()
        field("Stock concentration (C₁)").performTextReplacement("3")
        field("Final concentration (C₂)").performTextReplacement("1000")
        field("Final solution volume (V₂)").performTextReplacement("1")
        field("Final solution volume (V₂)").performImeAction()
        val result = DilutionCalculator.calculate(DilutionInput(
            "3", ConcentrationUnit.PPM, "1000", ConcentrationUnit.PPB, "1")) as DilutionResult.Success
        val expected = result.calculationSteps.mapIndexed { index, step -> "${index + 1}. $step" }
            .joinToString("\n")
        compose.onNodeWithContentDescription("Copy calculation steps").performScrollTo()
            .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        compose.waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == expected }
        compose.onNodeWithText("Calculation Steps").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithText("Copied").assertIsDisplayed()
    }

    @Test fun switchingCalculatorClearsFocusAndPreservesEnteredValues() {
        app()
        // SetText avoids coupling this focus regression to the test host's IME session.
        field("Stock concentration (C₁)").performTextReplacement("10")
        field("Stock concentration (C₁)").assertIsFocused()
        click("RPD")
        field("Original Sample Result").assertIsNotFocused()
        field("Original Sample Result").performTextReplacement("12")
        field("Original Sample Result").assertIsFocused()
        click("MS/MSD")
        field("Sample dilution factor").assertIsNotFocused()
        click("Dilution")
        field("Stock concentration (C₁)").assertTextContains("10").assertIsNotFocused()
        click("RPD")
        field("Original Sample Result").assertTextContains("12").assertIsNotFocused()
    }

    @Test fun focusedNumericInputRemainsVisibleWhenTheViewportShrinks() {
        var panelHeight by mutableStateOf(620.dp)
        compose.setContent {
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme {
                    Box(Modifier.height(panelHeight)) { LabCalculatorApp() }
                }
            }
        }
        field("Final solution volume (V₂)").performTextReplacement("50")
        field("Final solution volume (V₂)").assertIsFocused()
        compose.runOnIdle { panelHeight = 360.dp }
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("Calculator scroll").fetchSemanticsNode().boundsInRoot
        val input = field("Final solution volume (V₂)").fetchSemanticsNode().boundsInRoot
        assertTrue("Focused field and guidance must fit in the resized viewport",
            input.top >= viewport.top - 1f && input.bottom <= viewport.bottom + 1f)
        field("Final solution volume (V₂)").assertTextContains("50").assertIsFocused()
    }
}
