package com.dyfl.labcalculator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.dyfl.labcalculator.ui.theme.LabTabHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LabAppChrome(
    titles: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    var showCalculators by remember { mutableStateOf(false) }
    val tabsScroll = rememberScrollState()
    val tabRequesters = remember(titles.size) { List(titles.size) { BringIntoViewRequester() } }
    LaunchedEffect(selectedTabIndex) {
        withFrameNanos { }
        tabRequesters[selectedTabIndex].bringIntoView()
    }
    val tabColor = MaterialTheme.colorScheme.background
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            TopAppBar(
                title = {
                    Text("Lab Calculator", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    Box {
                        IconButton(onClick = { showCalculators = true }) {
                            Icon(LabIcons.Grid, contentDescription = "All calculators")
                        }
                        DropdownMenu(expanded = showCalculators,
                            onDismissRequest = { showCalculators = false }) {
                            titles.forEachIndexed { index, title ->
                                DropdownMenuItem(
                                    text = { Text(title, fontWeight = if (index == selectedTabIndex)
                                        FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.semantics { selected = index == selectedTabIndex },
                                    onClick = {
                                        onTabSelected(index)
                                        showCalculators = false
                                    }
                                )
                            }
                        }
                    }
                },
                expandedHeight = maxOf(48.dp, 48.dp * LocalDensity.current.fontScale),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
            Box(Modifier.fillMaxWidth()) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    scrollState = tabsScroll,
                    containerColor = tabColor,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 12.dp,
                    minTabWidth = 64.dp,
                    indicator = {},
                    divider = {}
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            modifier = Modifier.bringIntoViewRequester(tabRequesters[index])
                                .testTag("Tab: $title").padding(horizontal = 2.dp, vertical = 4.dp)
                                .clip(CircleShape).background(if (selectedTabIndex == index)
                                    MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            selected = selectedTabIndex == index,
                            onClick = { onTabSelected(index) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(title, modifier = Modifier.padding(horizontal = LabTabHorizontalPadding),
                                style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        )
                    }
                }
                if (tabsScroll.canScrollBackward) TabEdgeFade(tabColor,
                    if (rtl) Alignment.CenterEnd else Alignment.CenterStart, fadeFromLeft = !rtl)
                if (tabsScroll.canScrollForward) TabEdgeFade(tabColor,
                    if (rtl) Alignment.CenterStart else Alignment.CenterEnd, fadeFromLeft = rtl)
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.TabEdgeFade(
    color: Color, alignment: Alignment, fadeFromLeft: Boolean
) {
    // A drawing-only overlay leaves tab taps and swipe gestures available underneath.
    Box(Modifier.align(alignment).width(18.dp).height(48.dp).testTag("Tab scroll cue").drawBehind {
        drawRect(Brush.horizontalGradient(
            if (fadeFromLeft) listOf(color, color.copy(alpha = 0f))
            else listOf(color.copy(alpha = 0f), color)
        ))
    })
}
