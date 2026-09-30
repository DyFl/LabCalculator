package com.dyfl.labcalculator.calculation

import java.math.BigDecimal

const val MAX_NUMBER_INPUT_LENGTH = 256

/** Bounds arithmetic, decimal expansion, and Android saved-state size for pasted numbers. */
internal fun parseLabDecimal(text: String): BigDecimal {
    if (text.length > MAX_NUMBER_INPUT_LENGTH) {
        throw NumberFormatException("Enter a number of at most $MAX_NUMBER_INPUT_LENGTH characters.")
    }
    val value = try {
        BigDecimal(text)
    } catch (_: NumberFormatException) {
        throw NumberFormatException("Enter a valid number using digits and a decimal point (for example, 1.25).")
    }
    if (value.scale() !in -1000..1000) {
        throw NumberFormatException("Number is outside the supported range (decimal scale −1000 to 1000).")
    }
    return value
}
