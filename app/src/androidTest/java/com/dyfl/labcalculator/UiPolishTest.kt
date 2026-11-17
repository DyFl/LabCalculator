package com.dyfl.labcalculator

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LabAppChrome
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiPolishTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferencesName = "test_polish_${UUID.randomUUID()}"
    private val store = CalculatorPresetStore(context, preferencesName)
    private var dark by mutableStateOf(false)
    private var renderedDensity: Density? = null
    private var keyboard: SoftwareKeyboardController? = null
    private var inputModeManager: InputModeManager? = null
    @Volatile private var keyboardBottom = 0

    @After fun cleanUp() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun app() = compose.setContent {
        renderedDensity = LocalDensity.current
        keyboard = LocalSoftwareKeyboardController.current
        inputModeManager = LocalInputModeManager.current
        keyboardBottom = WindowInsets.ime.getBottom(LocalDensity.current)
        CompositionLocalProvider(LocalPresetStore provides store) {
            LabCalculatorTheme(darkTheme = dark) { LabCalculatorApp() }
        }
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun scrollToHeading() = compose.onNodeWithTag("Calculator heading").performScrollTo()
    private fun resultFits(label: String) {
        val viewport = compose.onNodeWithTag("Calculator scroll").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("Result card: $label").fetchSemanticsNode()
        val density = context.resources.displayMetrics.density
        assertTrue("Result top clipped: $label", card.positionInRoot.y >= viewport.top - 1f)
        if (card.size.height + 16f * density <= viewport.height + 1f) {
            assertTrue("Result bottom or 16dp margin clipped: $label",
                card.positionInRoot.y + card.size.height + 16f * density <= viewport.bottom + 1f)
        } else {
            // Large text and copy feedback can make a result taller than the viewport.
            // Keep its heading/copy action visible and allow the rest to scroll.
            compose.onNodeWithContentDescription("Copy $label").assertIsDisplayed()
            compose.onNodeWithTag("Calculator scroll").assert(hasScrollAction())
        }
    }
    private fun textFits() {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { layout ->
                    // Paragraph width can retain the parent's constraint after intrinsic sizing.
                    // Compare the painted line bounds, allowing one pixel for rounding.
                    val clippedLine = (0 until layout.lineCount).any { line ->
                        layout.getLineLeft(line) < -1f ||
                            layout.getLineRight(line) > layout.size.width + 1f ||
                            layout.getLineBottom(line) > layout.size.height + 1f
                    }
                    assertFalse("Clipped text: ${layout.layoutInput.text}; size=${layout.size}; " +
                        "constraints=${layout.layoutInput.constraints}; paragraphHeight=${layout.multiParagraph.height}",
                        clippedLine || layout.multiParagraph.didExceedMaxLines)
                    val density = checkNotNull(renderedDensity)
                    if (density.fontScale <= 1.3f &&
                        compose.onRoot().fetchSemanticsNode().size.width / density.density >= 360f &&
                        layout.layoutInput.text.text in listOf("Stock concentration (C₁)",
                            "Final concentration (C₂)", "Final solution volume (V₂)")) {
                        assertTrue("Subscript label must fit on one line: ${layout.layoutInput.text}",
                            layout.lineCount == 1)
                    }
                }
            }
    }

    private fun screenshot(name: String) {
        compose.runOnIdle { keyboard?.hide() }
        compose.waitUntil(5_000) { keyboardBottom == 0 }
        compose.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "ui-polish-fourth").apply { mkdirs() }
        if (name == "tab-0-empty-light") {
            val root = compose.onRoot().fetchSemanticsNode()
            val density = checkNotNull(renderedDensity)
            File(directory, "viewport.txt").writeText(
                "UI viewport: ${root.size.width / density.density}dp wide; " +
                    "density=${density.density}; fontScale=${density.fontScale}")
        }
        if (captureScreenshotsEnabled()) File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        textFits()
    }

    private fun infoFits() {
        val node = compose.onNodeWithText("All concentrations are in", substring = true,
            useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertTrue("MS/MSD unit note must fit in two lines; " +
            "lines=${layouts.single().lineCount}, width=${layouts.single().size.width}",
            layouts.single().lineCount <= 2)
    }

    @Test fun imeNextSkipsSelectorsAndDoneCalculatesAndShowsCopiedSnackbar() {
        app()
        compose.runOnIdle { checkNotNull(inputModeManager).requestInputMode(InputMode.Touch) }
        compose.onNodeWithText("No saved presets").assertExists()
        compose.onNodeWithText("Load preset").assertIsNotEnabled()
        field("Stock concentration (C₁)").performTextInput("10")
        field("Stock concentration (C₁)").performImeAction()
        field("Final concentration (C₂)").assertIsFocused().performTextInput("200")
        field("Final concentration (C₂)").performImeAction()
        field("Final solution volume (V₂)").assertIsFocused().performTextInput("50")
        compose.runOnIdle { assertEquals(InputMode.Touch, checkNotNull(inputModeManager).inputMode) }
        field("Final solution volume (V₂)").performImeAction()
        compose.onNodeWithText("Transfer 1 mL of stock and make up to 50 mL final solution volume.").assertIsDisplayed()
        resultFits("Preparation")
        field("Final solution volume (V₂)").assertIsNotFocused()
        compose.onNodeWithText("Calculate").assertIsFocused()
        compose.onNodeWithContentDescription("Copy Preparation").performClick()
        compose.onNodeWithText("Copied").assertIsDisplayed()
        click("Calculation Steps")
        compose.onNodeWithText("Calculation Steps").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        click("Calculation Steps")
        compose.onNodeWithText("Calculation Steps").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        // Recalculating unchanged inputs must bring the existing result back into view.
        field("Stock concentration (C₁)").performScrollTo()
        click("Calculate")
        compose.onNodeWithText("Transfer 1 mL of stock and make up to 50 mL final solution volume.").assertIsDisplayed()
        resultFits("Preparation")
    }

    @Test fun allTabsInheritActionsImeAndUnclippedSmallScreenLayouts() {
        app()
        val cases = listOf(
            Triple("Dilution", listOf("Stock concentration (C₁)" to "10",
                "Final concentration (C₂)" to "200", "Final solution volume (V₂)" to "50"), "Transfer 1 mL of stock and make up to 50 mL final solution volume."),
            Triple("RPD", listOf("Original Sample Result" to "10", "Replicate Sample Result" to "12"), "18.18%"),
            Triple("Unit conversions", listOf("Value to convert" to "1"), "1000 µg"),
            Triple("MS/MSD", listOf("Raw diluted source-sample result" to "5", "Sample dilution factor" to "10",
                "Final spike concentration added" to "50", "Literal MS result" to "55", "Literal MSD result" to "50"), "50 PPB"),
            Triple("Molarity", listOf("Desired Molarity" to "1", "Final Solution Volume" to "1000",
                "Formula Weight" to "58.44"), "58.440 g")
        )
        cases.forEachIndexed { index, (tab, values, result) ->
            click(tab)
            val filePrefix = "tab-$index"
            val firstInvalidLabel = if (tab == "MS/MSD") "Final spike concentration added"
                else values.first().first
            scrollToHeading()
            screenshot("$filePrefix-empty-light")
            click("Calculate")
            field(firstInvalidLabel).assertIsDisplayed().assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            screenshot("$filePrefix-error-light")
            click("Clear all")
            values.forEach { (label, value) -> field(label).performTextReplacement(value) }
            scrollToHeading()
            values.forEach { (label, _) ->
                compose.onAllNodesWithText(label, useUnmergedTree = true).assertCountEquals(1)
            }
            screenshot("$filePrefix-filled-light")
            field(values.last().first).performImeAction()
            values.forEach { (label, _) -> field(label).assertIsNotFocused() }
            compose.onNodeWithText(result).assertIsDisplayed()
            resultFits(listOf("Preparation", "Relative Percent Difference", "Converted result",
                "Original source concentration", "Required Mass")[index])
            screenshot("$filePrefix-result-light")
            compose.runOnIdle { dark = true }
            screenshot("$filePrefix-result-dark")
            click("Clear all")
            scrollToHeading()
            screenshot("$filePrefix-empty-dark")
            click("Calculate")
            field(firstInvalidLabel).assertIsDisplayed().assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            screenshot("$filePrefix-error-dark")
            click("Clear all")
            values.forEach { (label, value) -> field(label).performTextReplacement(value) }
            scrollToHeading()
            screenshot("$filePrefix-filled-dark")
            if (tab == "MS/MSD") {
                compose.onNodeWithText("All concentrations are in", substring = true)
                    .performScrollTo()
                infoFits()
                screenshot("ms-msd-info-dark")
            }
            compose.runOnIdle { dark = false }
            click("Clear all")
            compose.onAllNodesWithText("Clear").assertCountEquals(0)
        }
    }

    @Test fun selectedTabScrollsFullyIntoViewWithoutASwipe() {
        var selected by mutableStateOf(0)
        val titles = listOf("Dilution", "RPD", "Unit conversions", "MS/MSD", "Molarity")
        compose.setContent {
            renderedDensity = LocalDensity.current
            LabCalculatorTheme {
                LabAppChrome(titles, selected, { selected = it }) { Text("Selected calculator") }
            }
        }
        listOf(4, 0, 3, 1, 2).forEach { index ->
            compose.runOnIdle { selected = index }
            compose.waitForIdle()
            val tab = compose.onNodeWithTag("Tab: ${titles[index]}").fetchSemanticsNode()
            val width = compose.onRoot().fetchSemanticsNode().size.width
            assertTrue("Selected tab left clipped", tab.positionInRoot.x >= -1f)
            assertTrue("Selected tab right clipped", tab.positionInRoot.x + tab.size.width <= width + 1f)
            val allTabsWidth = titles.sumOf {
                compose.onNodeWithTag("Tab: $it").fetchSemanticsNode().size.width
            }
            if (allTabsWidth > width) {
                assertTrue("Scrollable tabs need a visible edge cue",
                    compose.onAllNodesWithTag("Tab scroll cue").fetchSemanticsNodes().isNotEmpty())
            }
            textFits()
        }
    }

    @Test fun sharedMsMsdUnitConvertsValuesAndConversionSwapWorks() {
        app()
        click("MS/MSD")
        field("Raw diluted source-sample result").performTextReplacement("5")
        compose.onNodeWithContentDescription("Shared concentration unit")
            .performScrollTo().performClick()
        compose.onNodeWithText("PPM (parts per million)").performClick()
        field("Raw diluted source-sample result").assertTextContains("0.005")
        compose.onAllNodesWithContentDescription("Unit for", substring = true).assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Shared concentration unit").assertCountEquals(1)
        compose.onNodeWithText("All concentrations are in PPM.", substring = true).assertExists()
        infoFits()

        click("Unit conversions")
        compose.onNodeWithContentDescription("Category").performScrollTo().performClick()
        compose.onNodeWithText("Mass concentration").performClick()
        compose.onNodeWithText("Clear and change").assertDoesNotExist()
        compose.onNodeWithContentDescription("Starting unit").performScrollTo().performClick()
        compose.onNodeWithText("grams per liter (g/L)").performClick()
        textFits()
        compose.onNodeWithContentDescription("Swap units")
            .performScrollTo().assertHeightIsEqualTo(androidx.compose.ui.unit.Dp(48f))
            .assertWidthIsEqualTo(androidx.compose.ui.unit.Dp(48f))
        val swap = compose.onNodeWithContentDescription("Swap units").fetchSemanticsNode()
        val width = compose.onRoot().fetchSemanticsNode().size.width
        assertTrue("Swap must be centered", kotlin.math.abs(swap.positionInRoot.x + swap.size.width / 2f - width / 2f) <= 1f)
        compose.onNodeWithContentDescription("Swap units").performClick()
        field("Value to convert").performTextReplacement("1")
        field("Value to convert").performImeAction()
        compose.onNodeWithText("0.000000001 g/L").assertIsDisplayed()
        resultFits("Converted result")
        screenshot("conversion-long-units-result-light")
    }
}
