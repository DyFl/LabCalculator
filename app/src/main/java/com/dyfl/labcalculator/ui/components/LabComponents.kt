package com.dyfl.labcalculator.ui

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabResultBackground
import com.dyfl.labcalculator.ui.theme.LabText
import com.dyfl.labcalculator.ui.theme.LabResultBottomMargin
import com.dyfl.labcalculator.ui.theme.LabPanelShape
import com.dyfl.labcalculator.ui.theme.LabResultContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CALCULATION_STEP_SEPARATOR = "\u001F"
private const val CALCULATION_SECTION_SEPARATOR = "\u001D"
private const val CALCULATION_SECTION_TITLE_SEPARATOR = "\u001E"

internal data class CalculationStepsSection(
    val title: String,
    val steps: List<String>
)

internal fun encodeCalculationSteps(steps: List<String>): String =
    steps.joinToString(CALCULATION_STEP_SEPARATOR)

internal fun decodeCalculationSteps(encodedSteps: String): List<String> =
    if (encodedSteps.isEmpty()) emptyList() else encodedSteps.split(CALCULATION_STEP_SEPARATOR)

internal fun encodeCalculationStepSections(
    sections: List<CalculationStepsSection>
): String = sections.joinToString(CALCULATION_SECTION_SEPARATOR) { section ->
    section.title + CALCULATION_SECTION_TITLE_SEPARATOR + encodeCalculationSteps(section.steps)
}

internal fun decodeCalculationStepSections(
    encodedSections: String
): List<CalculationStepsSection> {
    if (encodedSections.isEmpty()) return emptyList()

    return encodedSections.split(CALCULATION_SECTION_SEPARATOR).map { encodedSection ->
        val parts = encodedSection.split(CALCULATION_SECTION_TITLE_SEPARATOR, limit = 2)
        CalculationStepsSection(
            title = parts.first(),
            steps = decodeCalculationSteps(parts.getOrElse(1) { "" })
        )
    }
}

/** Details remain available without taking space from the inputs on first use. */
@Composable
internal fun LabFormulaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = LabExpandableCard("Formula and assumptions", modifier, content = content)

/** The entire header toggles the details, including its animated chevron. */
@Composable
private fun LabExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    copyText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "Details chevron")
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button,
                onClickLabel = if (expanded) "Collapse $title" else "Expand $title") {
                expanded = !expanded
            }.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .heightIn(min = 52.dp).padding(horizontal = 4.dp,
                    vertical = if (copyText == null) 12.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium, color = LabMutedText)
            if (copyText != null) LabCopyButton("calculation steps", copyText, tonal = false)
            Icon(LabIcons.Chevron, contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).rotate(rotation), tint = LabMutedText)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/** Results copy the displayed value and units; the first result scrolls into view. */
@Composable
internal fun LabResultCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    copyEnabled: Boolean = true,
    scrollIntoView: Boolean = true,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall
) {
    Card(
        modifier = modifier.fillMaxWidth().resultScrollTarget(value, scrollIntoView)
            .testTag("Result card: $label").semantics { liveRegion = LiveRegionMode.Polite },
        shape = LabPanelShape,
        colors = CardDefaults.cardColors(containerColor = LabResultBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    color = LabResultContent)
                LabCopyButton(label, value, enabled = copyEnabled)
            }
            SelectionContainer {
                Text(value, style = valueStyle.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold, color = LabResultContent)
            }
            if (supportingText != null) {
                Spacer(Modifier.height(8.dp))
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = LabResultContent)
            }
        }
    }
}

/** Shared full-size copy control; clipboard contents and feedback use the displayed text. */
@Composable
internal fun LabCopyButton(label: String, value: String, enabled: Boolean = true, tonal: Boolean = true) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalCopySnackbar.current
    val copy: () -> Unit = {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, value)))
            snackbar?.currentSnackbarData?.dismiss()
            snackbar?.showSnackbar("Copied")
        }
    }
    if (tonal) {
        FilledTonalIconButton(onClick = copy, enabled = enabled, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                contentColor = LabResultContent)) {
            Icon(LabIcons.Copy, contentDescription = "Copy $label")
        }
    } else {
        IconButton(onClick = copy, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(LabIcons.Copy, contentDescription = "Copy $label")
        }
    }
}

/** Scroll newly calculated results after their layout and keyboard resize have settled. */
@Composable
internal fun Modifier.resultScrollTarget(value: String, scrollIntoView: Boolean = true): Modifier {
    val calculationRequest = LocalCalculationRequest.current
    val resultRequester = remember { BringIntoViewRequester() }
    var resultSize by remember { mutableStateOf(IntSize.Zero) }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val viewportHeight = LocalCalculatorViewportHeight.current
    LaunchedEffect(calculationRequest, value) {
        if (scrollIntoView && calculationRequest > 0) {
            // Wait for keyboard resizing and the new result's layout before scrolling.
            snapshotFlow { ime.getBottom(density) }.first { it == 0 }
            withFrameNanos { }
            resultRequester.bringIntoView(Rect(0f, 0f, resultSize.width.toFloat(),
                minOf(viewportHeight.value.toFloat(),
                    resultSize.height + with(density) { LabResultBottomMargin.toPx() })))
        }
    }
    return bringIntoViewRequester(resultRequester).onSizeChanged { resultSize = it }
}

/** Working stays selectable and is expanded only when requested. */
@Composable
internal fun CalculationStepsCard(
    modifier: Modifier = Modifier,
    steps: List<String> = emptyList(),
    sections: List<CalculationStepsSection> = emptyList()
) {
    if (steps.isEmpty() && sections.isEmpty()) return
    val copyText = if (sections.isEmpty()) numberedStepsText(steps) else
        sections.joinToString("\n\n") { it.title + "\n" + numberedStepsText(it.steps) }
    LabExpandableCard("Calculation Steps", modifier, copyText = copyText) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (sections.isEmpty()) NumberedCalculationSteps(steps)
                else sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = LabBlue)
                        NumberedCalculationSteps(section.steps)
                    }
                }
            }
        }
    }
}

private fun numberedStepsText(steps: List<String>): String =
    steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")

@Composable
private fun NumberedCalculationSteps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = LabText)
        }
    }
}
