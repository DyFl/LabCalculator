package com.dyfl.labcalculator.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyfl.labcalculator.ui.theme.LabBlue
import com.dyfl.labcalculator.ui.theme.LabControlShape
import com.dyfl.labcalculator.ui.theme.LabMutedText
import com.dyfl.labcalculator.ui.theme.LabResultBackground
import kotlinx.coroutines.launch

/** One summary with wrapping, selectable values and a full touch target for each copy action. */
@Composable
internal fun MsMsdResultSummary(
    originalSource: String,
    msRecovery: String,
    msdRecovery: String,
    rpd: String,
    rpdUnavailableReason: String? = null
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalCopySnackbar.current
    val results = listOf("Original source concentration" to originalSource,
        "MS recovery" to msRecovery, "MSD recovery" to msdRecovery, "MS/MSD RPD" to rpd)
    Card(
        modifier = Modifier.fillMaxWidth().resultScrollTarget(originalSource).testTag("MS/MSD summary"),
        shape = LabControlShape,
        colors = CardDefaults.cardColors(containerColor = LabResultBackground)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            results.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider()
                val unavailableReason = if (index == 3) rpdUnavailableReason else null
                Column(Modifier.fillMaxWidth()
                    .testTag("Result card: $label")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = LabMutedText)
                            SelectionContainer {
                                Text(value, style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold, color = LabBlue)
                            }
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, value)))
                                    snackbar?.currentSnackbarData?.dismiss()
                                    snackbar?.showSnackbar("Copied")
                                }
                            },
                            enabled = unavailableReason == null,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(LabIcons.Copy, contentDescription = "Copy $label") }
                    }
                    if (index == 3) {
                        Text(unavailableReason ?: "RPD of measured concentrations.",
                            style = MaterialTheme.typography.bodySmall, color = LabMutedText)
                    }
                }
            }
        }
    }
}
