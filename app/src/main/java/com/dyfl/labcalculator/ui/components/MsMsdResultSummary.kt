package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.ui.theme.LabResultBackground
import com.dyfl.labcalculator.ui.theme.LabResultContent
import com.dyfl.labcalculator.ui.theme.LabPanelShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading

/** One summary with wrapping, selectable values and a full touch target for each copy action. */
@Composable
internal fun MsMsdResultSummary(
    originalSource: String,
    msRecovery: String,
    msdRecovery: String,
    rpd: String,
    rpdUnavailableReason: String? = null
) {
    val results = listOf("Original source concentration" to originalSource,
        "MS recovery" to msRecovery, "MSD recovery" to msdRecovery, "MS/MSD RPD" to rpd)
    Card(
        modifier = Modifier.fillMaxWidth().resultScrollTarget(originalSource).testTag("MS/MSD summary")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = LabPanelShape,
        colors = CardDefaults.cardColors(containerColor = LabResultBackground)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            results.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = LabResultContent.copy(alpha = 0.2f))
                val unavailableReason = if (index == 3) rpdUnavailableReason else null
                Column(Modifier.fillMaxWidth()
                    .testTag("Result card: $label")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.labelLarge, color = LabResultContent)
                            SelectionContainer {
                                Text(value, style = (if (index == 0) MaterialTheme.typography.headlineMedium
                                    else MaterialTheme.typography.headlineSmall).copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold, color = LabResultContent)
                            }
                        }
                        LabCopyButton(label, value, enabled = unavailableReason == null)
                    }
                    if (index == 3) {
                        Text(unavailableReason ?: "RPD of measured concentrations.",
                            style = MaterialTheme.typography.bodySmall, color = LabResultContent)
                    }
                }
            }
        }
    }
}
