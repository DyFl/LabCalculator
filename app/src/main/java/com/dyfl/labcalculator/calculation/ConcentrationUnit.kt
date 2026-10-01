package com.dyfl.labcalculator.calculation

import java.math.BigDecimal

enum class ConcentrationFamily(val label: String) {
    PARTS_PER("Parts per: matching ratio basis"),
    MASS_PER_VOLUME("Mass per volume")
}

/** Parts-per ratios never imply mass per volume or a density. Enum names preserve v1 presets. */
enum class ConcentrationUnit(
    val label: String,
    val description: String,
    val family: ConcentrationFamily,
    internal val baseMultiplier: BigDecimal
) {
    PPM("PPM", "PPM (parts per million)", ConcentrationFamily.PARTS_PER, BigDecimal("1000")),
    PPB("PPB", "PPB (parts per billion)", ConcentrationFamily.PARTS_PER, BigDecimal.ONE),
    MILLIGRAM_PER_LITER("mg/L", "mg/L (milligrams per liter)", ConcentrationFamily.MASS_PER_VOLUME,
        MetricUnit.MILLIGRAM_PER_LITER.baseUnitMultiplier.divide(MetricUnit.MICROGRAM_PER_LITER.baseUnitMultiplier)),
    MICROGRAM_PER_LITER("µg/L", "µg/L (micrograms per liter)", ConcentrationFamily.MASS_PER_VOLUME,
        BigDecimal.ONE);

    val baseUnit: ConcentrationUnit
        get() = if (family == ConcentrationFamily.PARTS_PER) PPB else MICROGRAM_PER_LITER

    internal fun toBase(value: BigDecimal): BigDecimal = value.multiply(baseMultiplier)

    fun convert(value: BigDecimal, to: ConcentrationUnit): BigDecimal {
        require(family == to.family) { "Concentration families are incompatible; re-enter concentrations." }
        return toBase(value).divide(to.baseMultiplier)
    }
}
