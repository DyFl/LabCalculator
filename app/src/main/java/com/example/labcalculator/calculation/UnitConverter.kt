package com.example.labcalculator.calculation

import java.math.BigDecimal

enum class UnitCategory(val displayName: String) {
    MASS("Mass"),
    VOLUME("Volume"),
    MASS_CONCENTRATION("Mass concentration")
}

enum class MetricUnit(
    val displayName: String,
    val symbol: String,
    val category: UnitCategory,
    internal val baseUnitMultiplier: BigDecimal
) {
    MICROGRAM("micrograms", "µg", UnitCategory.MASS, BigDecimal.ONE),
    MILLIGRAM("milligrams", "mg", UnitCategory.MASS, BigDecimal("1000")),
    GRAM("grams", "g", UnitCategory.MASS, BigDecimal("1000000")),

    MICROLITER("microliters", "µL", UnitCategory.VOLUME, BigDecimal.ONE),
    MILLILITER("milliliters", "mL", UnitCategory.VOLUME, BigDecimal("1000")),
    LITER("liters", "L", UnitCategory.VOLUME, BigDecimal("1000000")),

    NANOGRAM_PER_LITER(
        "nanograms per liter",
        "ng/L",
        UnitCategory.MASS_CONCENTRATION,
        BigDecimal.ONE
    ),
    MICROGRAM_PER_LITER(
        "micrograms per liter",
        "µg/L",
        UnitCategory.MASS_CONCENTRATION,
        BigDecimal("1000")
    ),
    MILLIGRAM_PER_LITER(
        "milligrams per liter",
        "mg/L",
        UnitCategory.MASS_CONCENTRATION,
        BigDecimal("1000000")
    ),
    GRAM_PER_LITER(
        "grams per liter",
        "g/L",
        UnitCategory.MASS_CONCENTRATION,
        BigDecimal("1000000000")
    );

    companion object {
        fun forCategory(category: UnitCategory): List<MetricUnit> =
            entries.filter { it.category == category }
    }
}

sealed interface UnitConversionResult {
    data class Success(
        val exactValue: BigDecimal,
        val formattedValue: String,
        val calculationSteps: List<String>
    ) : UnitConversionResult

    data class Invalid(val message: String) : UnitConversionResult
}

/** Pure metric conversion using exact powers-of-ten factors. */
object UnitConverter {
    fun convert(
        valueText: String,
        fromUnit: MetricUnit,
        toUnit: MetricUnit
    ): UnitConversionResult {
        if (fromUnit.category != toUnit.category) {
            return UnitConversionResult.Invalid(
                "Choose starting and destination units from the same category."
            )
        }

        val trimmedValue = valueText.trim()
        if (trimmedValue.isEmpty()) {
            return UnitConversionResult.Invalid("Enter a value to convert.")
        }

        val value = try {
            BigDecimal(trimmedValue)
        } catch (_: NumberFormatException) {
            return UnitConversionResult.Invalid(
                "Enter a valid number using digits and a decimal point."
            )
        }

        val valueInBaseUnits = value.multiply(fromUnit.baseUnitMultiplier)
        val converted = valueInBaseUnits.divide(toUnit.baseUnitMultiplier)
        val formattedValue = converted.toExactPlainString()
        val factor = conversionFactor(fromUnit, toUnit)
        val factorDisplay = factor.toExactPlainString()
        val calculationSteps = listOf(
            "Conversion factor: 1 ${fromUnit.symbol} = " +
                "${factorDisplay.withThousandsSeparators()} ${toUnit.symbol}.",
            "${value.toGroupedExactString()} ${fromUnit.symbol} × " +
                "(${factorDisplay.withThousandsSeparators()} ${toUnit.symbol} ÷ " +
                "1 ${fromUnit.symbol}).",
            "The ${fromUnit.symbol} units cancel.",
            "Result = ${formattedValue.withThousandsSeparators()} ${toUnit.symbol}."
        )

        return UnitConversionResult.Success(
            exactValue = converted,
            formattedValue = formattedValue,
            calculationSteps = calculationSteps
        )
    }

    fun explanation(fromUnit: MetricUnit, toUnit: MetricUnit): String {
        val result = convert("1", fromUnit, toUnit)
        if (result !is UnitConversionResult.Success) return ""

        return "1 ${fromUnit.symbol} = ${result.formattedValue.withThousandsSeparators()} " +
            "${toUnit.symbol}."
    }

    private fun conversionFactor(fromUnit: MetricUnit, toUnit: MetricUnit): BigDecimal =
        fromUnit.baseUnitMultiplier.divide(toUnit.baseUnitMultiplier)
}
