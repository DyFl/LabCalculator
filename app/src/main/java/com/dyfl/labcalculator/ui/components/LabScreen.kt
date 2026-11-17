package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.ui.theme.LabActionPadding
import com.dyfl.labcalculator.ui.theme.LabPanelShape
import com.dyfl.labcalculator.ui.theme.LabActionShape
import com.dyfl.labcalculator.ui.theme.LabContentMaxWidth
import com.dyfl.labcalculator.ui.theme.LabPagePadding
import kotlinx.coroutines.flow.first

internal val LocalCalculate = staticCompositionLocalOf<() -> Unit> { {} }
internal val LocalCalculationFocus = staticCompositionLocalOf<FocusRequester?> { null }
internal val LocalCalculationRequest = staticCompositionLocalOf { 0 }
internal val LocalCopySnackbar = staticCompositionLocalOf<SnackbarHostState?> { null }
internal val LocalNumberFieldFocus = staticCompositionLocalOf<MutableList<FocusRequester>?> { null }
internal class LabValidationTarget(val requester: BringIntoViewRequester, val hasError: () -> Boolean)
internal val LocalValidationTargets = staticCompositionLocalOf<MutableList<LabValidationTarget>?> { null }
private val UnlimitedViewportHeight: State<Int> = mutableIntStateOf(Int.MAX_VALUE)
internal val LocalCalculatorViewportHeight = staticCompositionLocalOf { UnlimitedViewportHeight }

/** Every tab gets one scroll container, IME handling and copy feedback. */
@Composable
internal fun LabScreen(
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val calculateFocus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val fields = remember { mutableListOf<FocusRequester>() }
    val validationTargets = remember { mutableListOf<LabValidationTarget>() }
    // Publish a stable holder; reading its changing value in composition invalidates every field.
    val viewportHeight = remember { mutableIntStateOf(Int.MAX_VALUE) }
    var calculationRequest by remember { mutableIntStateOf(0) }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(calculationRequest) {
        if (calculationRequest > 0) {
            snapshotFlow { ime.getBottom(density) }.first { it == 0 }
            withFrameNanos { }
            validationTargets.firstOrNull { it.hasError() }?.requester?.bringIntoView()
        }
    }
    val currentCalculate by rememberUpdatedState(onCalculate)
    val calculate: () -> Unit = remember(focusManager, keyboard, calculateFocus) {
        {
            focusManager.clearFocus(force = true)
            // Keep focus on the primary action so Android cannot reopen the next visible editor.
            calculateFocus.requestFocus()
            keyboard?.hide()
            currentCalculate()
            calculationRequest++
        }
    }
    CompositionLocalProvider(
        LocalCalculate provides calculate,
        LocalCalculationFocus provides calculateFocus,
        LocalCalculationRequest provides calculationRequest,
        LocalCopySnackbar provides snackbar,
        LocalNumberFieldFocus provides fields,
        LocalValidationTargets provides validationTargets,
        LocalCalculatorViewportHeight provides viewportHeight
    ) {
        Column(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding().imePadding()
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .testTag("Calculator scroll").onSizeChanged { viewportHeight.intValue = it.height }
                    .verticalScroll(scrollState)
                    .padding(LabPagePadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(Modifier.widthIn(max = LabContentMaxWidth).fillMaxWidth(), content = content)
            }
            // Reserve space while feedback is visible so it cannot cover the steps toggle.
            SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
internal fun LabInputsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LabPanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Labels, widths and shape are shared; screens supply their existing clear callback. */
@Composable
internal fun LabCalculateActions(onClear: () -> Unit, onNextSample: (() -> Unit)? = null) {
    val calculate = LocalCalculate.current
    val calculateFocus = LocalCalculationFocus.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = calculate,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).then(
                    if (calculateFocus != null) Modifier.focusRequester(calculateFocus)
                        .focusProperties { canFocus = true } else Modifier),
                shape = LabActionShape,
                contentPadding = LabActionPadding
            ) { Text("Calculate", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = onClear,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = LabActionShape,
                contentPadding = LabActionPadding
            ) { Text("Clear all") }
        }
        if (onNextSample != null) {
            FilledTonalButton(
                onClick = onNextSample,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp),
                shape = LabActionShape
            ) { Text("Next sample") }
        }
    }
}
