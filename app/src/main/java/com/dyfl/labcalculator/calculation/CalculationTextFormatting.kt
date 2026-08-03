package com.dyfl.labcalculator.calculation

import java.math.BigDecimal

internal fun BigDecimal.toExactPlainString(): String {
    if (compareTo(BigDecimal.ZERO) == 0) return "0"
    return stripTrailingZeros().toPlainString()
}

internal fun BigDecimal.toGroupedExactString(): String =
    toExactPlainString().withThousandsSeparators()

internal fun String.withThousandsSeparators(): String {
    val sign = if (startsWith('-')) "-" else ""
    val unsigned = removePrefix("-")
    val suffixIndex = unsigned.indexOfFirst { it != '.' && !it.isDigit() }
    val number = if (suffixIndex >= 0) unsigned.substring(0, suffixIndex) else unsigned
    val suffix = if (suffixIndex >= 0) unsigned.substring(suffixIndex) else ""
    val parts = number.split('.', limit = 2)
    val groupedWhole = parts[0]
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    val decimalPart = parts.getOrNull(1)?.let { ".$it" }.orEmpty()
    return sign + groupedWhole + decimalPart + suffix
}
