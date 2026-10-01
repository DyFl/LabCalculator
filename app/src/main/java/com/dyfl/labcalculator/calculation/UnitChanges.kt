package com.dyfl.labcalculator.calculation

import java.math.BigDecimal

internal sealed interface UnitChangeResult {
    data class Converted(val values: List<String>) : UnitChangeResult
    data class Blocked(val message: String) : UnitChangeResult
    data object ResetRequired : UnitChangeResult
}

/** Quantity-preserving, atomic changes. Blank fields remain blank; no rounded display is reused. */
internal object UnitChanges {
    fun concentrations(values: List<String>, from: ConcentrationUnit, to: ConcentrationUnit): UnitChangeResult =
        if (from.family != to.family) UnitChangeResult.ResetRequired
        else convert(values, from.baseMultiplier, to.baseMultiplier)

    fun metric(values: List<String>, from: MetricUnit, to: MetricUnit): UnitChangeResult =
        if (from.category != to.category) UnitChangeResult.ResetRequired
        else convert(values, from.baseUnitMultiplier, to.baseUnitMultiplier)

    private fun convert(values: List<String>, from: BigDecimal, to: BigDecimal): UnitChangeResult {
        // Build the complete replacement before exposing any changed value to the UI.
        val converted = mutableListOf<String>()
        for (text in values) {
            if (text.isBlank()) {
                converted += text
                continue
            }
            try {
                val value = parseLabDecimal(text.trim()).multiply(from).divide(to).stripTrailingZeros()
                val plain = value.toPlainString()
                val replacement = if (plain.length <= MAX_NUMBER_INPUT_LENGTH) plain else value.toString()
                // Conversion must remain editable and supported by the same parser as calculation.
                parseLabDecimal(replacement)
                converted += replacement
            } catch (_: NumberFormatException) {
                return UnitChangeResult.Blocked("Units unchanged. Fix invalid or out-of-range numbers " +
                    "in the populated fields before converting. " +
                    "Blank fields may stay blank.")
            }
        }
        return UnitChangeResult.Converted(converted)
    }
}

internal const val CONCENTRATION_CHANGE_GUIDANCE =
    "Compatible unit changes convert entered quantities exactly. Blank fields stay blank; " +
        "invalid numbers block the change. Changing concentration family clears concentrations for re-entry."

internal const val MS_MSD_BASIS_GUIDANCE =
    "Spike added after dilution. Enter uncorrected source, MS and MSD measurements on the same " +
        "dilution and concentration basis. Enter the final spike concentration added to each diluted aliquot. " +
        "Assumes negligible spike volume, so the native sample concentration is unchanged."
