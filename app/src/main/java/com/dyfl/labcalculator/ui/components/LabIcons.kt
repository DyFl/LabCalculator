package com.dyfl.labcalculator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Local vectors keep these shared controls independent of an extra icon dependency.
internal object LabIcons {
    val Info = icon("Info", "M11,17H13V11H11V17ZM12,2C6.48,2 2,6.48 2,12S6.48,22 12,22S22,17.52 22,12S17.52,2 12,2ZM12,20C7.59,20 4,16.41 4,12S7.59,4 12,4S20,7.59 20,12S16.41,20 12,20ZM11,9H13V7H11V9Z")
    val SwapVert = icon("Swap vert", "M16,3L12,7H15V14H17V7H20L16,3ZM8,21L12,17H9V10H7V17H4L8,21Z")
    val Copy = icon("Copy", "M16,1H4C2.9,1 2,1.9 2,3V17H4V3H16V1ZM19,5H8C6.9,5 6,5.9 6,7V21C6,22.1 6.9,23 8,23H19C20.1,23 21,22.1 21,21V7C21,5.9 20.1,5 19,5ZM19,21H8V7H19V21Z")
    val FolderOpen = icon("Folder open", "M20,6H12L10,4H4C2.9,4 2,4.9 2,6V18C2,19.1 2.9,20 4,20H20C21.1,20 22,19.1 22,18V8C22,6.9 21.1,6 20,6ZM4,6H9.17L11.17,8H20V10H6L4,18V6ZM20,18H6L8,12H22L20,18Z")
    val Save = icon("Save", "M17,3H5C3.9,3 3,3.9 3,5V19C3,20.1 3.9,21 5,21H19C20.1,21 21,20.1 21,19V7L17,3ZM19,19H5V5H16.17L19,7.83V19ZM7,5H15V9H7V5ZM7,13H17V17H7V13Z")
    val Chevron = icon("Expand", "M7.41,8.59L12,13.17L16.59,8.59L18,10L12,16L6,10L7.41,8.59Z")

    private fun icon(name: String, path: String) = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black)).build()
}
