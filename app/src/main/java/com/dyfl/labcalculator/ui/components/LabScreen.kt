package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.ui.theme.LabActionPadding
import com.dyfl.labcalculator.ui.theme.LabControlShape

internal val LocalCalculate = staticCompositionLocalOf<() -> Unit> { {} }
internal val LocalCalculationRequest = staticCompositionLocalOf { 0 }
internal val LocalCopySnackbar = staticCompositionLocalOf<SnackbarHostState?> { null }
internal val LocalNumberFieldFocus = staticCompositionLocalOf<MutableList<FocusRequester>?> { null }

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
    val snackbar = remember { SnackbarHostState() }
    val fields = remember { mutableListOf<FocusRequester>() }
    var calculationRequest by remember { mutableIntStateOf(0) }
    val calculate: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onCalculate()
        calculationRequest++
    }
    CompositionLocalProvider(
        LocalCalculate provides calculate,
        LocalCalculationRequest provides calculationRequest,
        LocalCopySnackbar provides snackbar,
        LocalNumberFieldFocus provides fields
    ) {
        Column(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding().imePadding()
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .testTag("Calculator scroll").verticalScroll(scrollState).padding(16.dp),
                content = content
            )
            // Reserve space while feedback is visible so it cannot cover the steps toggle.
            SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
internal fun LabInputsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LabControlShape,
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
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = calculate,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = LabControlShape,
                contentPadding = LabActionPadding
            ) { Text("Calculate", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = LabControlShape,
                contentPadding = LabActionPadding
            ) { Text("Clear all", fontWeight = FontWeight.Bold) }
        }
        if (onNextSample != null) {
            TextButton(
                onClick = onNextSample,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp),
                shape = LabControlShape
            ) { Text("Next sample") }
        }
    }
}
