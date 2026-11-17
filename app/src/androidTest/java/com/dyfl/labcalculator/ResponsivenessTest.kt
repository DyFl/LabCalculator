package com.dyfl.labcalculator

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.ui.LabCalculateActions
import com.dyfl.labcalculator.ui.LabCalculatorApp
import com.dyfl.labcalculator.ui.LabNumberTextField
import com.dyfl.labcalculator.ui.LabScreen
import com.dyfl.labcalculator.ui.LocalPresetStore
import com.dyfl.labcalculator.ui.theme.LabCalculatorTheme
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResponsivenessTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "test_responsiveness_${UUID.randomUUID()}"
    private val store = CalculatorPresetStore(context, preferencesName)

    @After fun cleanUp() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun lastTabTapWinsBeforeTheNextComposition() {
        compose.setContent {
            CompositionLocalProvider(LocalPresetStore provides store) {
                LabCalculatorTheme { LabCalculatorApp() }
            }
        }
        val field = compose.onNode(hasSetTextAction() and hasContentDescription("Stock concentration (C₁)"))
        field.performTextReplacement("10")
        // Two taps in one UI turn reproduce a return tap arriving before recomposition.
        val dilution = compose.onNodeWithTag("Tab: Dilution").fetchSemanticsNode()
            .config[SemanticsActions.OnClick].action!!
        val rpd = compose.onNodeWithTag("Tab: RPD").fetchSemanticsNode()
            .config[SemanticsActions.OnClick].action!!
        compose.runOnIdle {
            rpd()
            dilution()
        }
        compose.onNodeWithTag("Tab: Dilution").assertIsSelected()
        field.assertTextContains("10")
        compose.runOnIdle { rpd() }
        compose.onNodeWithTag("Tab: RPD").assertIsSelected()
    }

    @Test fun resizingViewportDoesNotRecomposeAnUnchangedForm() {
        val compositions = AtomicInteger()
        var panelHeight by mutableStateOf(620.dp)
        compose.setContent {
            val calculate = remember { {} }
            val form: @Composable ColumnScope.() -> Unit = remember {
                {
                    SideEffect { compositions.incrementAndGet() }
                    LabNumberTextField("", {}, label = "Probe value")
                    LabCalculateActions(onClear = {})
                }
            }
            LabCalculatorTheme {
                Box(Modifier.height(panelHeight)) { LabScreen(onCalculate = calculate, content = form) }
            }
        }
        compose.waitForIdle()
        val before = compositions.get()
        for (height in 600 downTo 400 step 20) {
            compose.runOnIdle { panelHeight = height.dp }
            compose.waitForIdle()
        }
        val extraCompositions = compositions.get() - before
        File(context.getExternalFilesDir(null), "responsiveness-resize-probe.txt").writeText(
            "viewportChanges=11; additionalFormCompositions=$extraCompositions\n")
        // Diagnostic snapshots may record the old behavior without treating it as a passing regression.
        if (InstrumentationRegistry.getArguments().getString("recordOnly") != "true") {
            assertEquals("Keyboard/viewport resizing must not invalidate the unchanged form", 0, extraCompositions)
        }
    }

    @Test fun changingCalculateCallbackKeepsFormStableAndUsesTheLatestInputs() {
        val compositions = AtomicInteger()
        var inputVersion by mutableStateOf(0)
        var calculatedVersion = -1
        compose.setContent {
            val version = inputVersion
            val form: @Composable ColumnScope.() -> Unit = remember {
                {
                    SideEffect { compositions.incrementAndGet() }
                    LabNumberTextField("", {}, label = "Probe value")
                    LabCalculateActions(onClear = {})
                }
            }
            LabCalculatorTheme {
                LabScreen(onCalculate = { calculatedVersion = version }, content = form)
            }
        }
        compose.waitForIdle()
        val before = compositions.get()
        for (version in 1..10) {
            compose.runOnIdle { inputVersion = version }
            compose.waitForIdle()
        }
        val extraCompositions = compositions.get() - before
        File(context.getExternalFilesDir(null), "responsiveness-callback-probe.txt").writeText(
            "callbackChanges=10; additionalFormCompositions=$extraCompositions\n")
        if (InstrumentationRegistry.getArguments().getString("recordOnly") != "true") {
            assertEquals("A fresh Calculate callback must not invalidate unchanged form content", 0, extraCompositions)
        }
        compose.onNodeWithText("Calculate").performScrollTo().performClick()
        assertEquals("Calculate must still use the latest input snapshot", 10, calculatedVersion)
    }
}
