package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationAuditTest {
    private val dilution = DilutionInput("10", ConcentrationUnit.PPM, "200", ConcentrationUnit.PPB, "50")
    private val spike = MsMsdInput("5", "10", "50", "55", "50")

    @Test(timeout = 5000)
    fun `long repeating dilution returns an exact fraction promptly`() {
        val result = DilutionCalculator.calculate(dilution.copy(
            stockConcentration = "1000000007", finalConcentration = "1",
            finalUnit = ConcentrationUnit.PPM, finalSolutionVolumeMl = "1"
        )) as DilutionResult.Success
        assertEquals("1/1000000007", result.volumeFromStockMl.toExactString())
        assertTrue(result.calculationSteps.any { it.contains("exact fraction") })
    }

    @Test
    fun `dilution preserves a nonrepeating prefix and a multi-digit period`() {
        fun volume(stock: String) = (DilutionCalculator.calculate(dilution.copy(
            stockConcentration = stock, finalConcentration = "1",
            finalUnit = ConcentrationUnit.PPM, finalSolutionVolumeMl = "1"
        )) as DilutionResult.Success).volumeFromStockMl.toExactString()
        assertEquals("1/6", volume("6"))
        assertEquals("1/7", volume("7"))
    }

    @Test
    fun `dilution decimal equal-concentration and extreme valid cases`() {
        val cases = listOf(
            dilution.copy(stockConcentration = "2.5", finalConcentration = "125", finalSolutionVolumeMl = "12.5") to "0.625",
            dilution.copy(stockConcentration = "1000", stockUnit = ConcentrationUnit.PPB,
                finalConcentration = "1", finalUnit = ConcentrationUnit.PPM) to "50",
            dilution.copy(stockConcentration = "1e100", finalConcentration = "1e100",
                finalUnit = ConcentrationUnit.PPM, finalSolutionVolumeMl = "1e-100") to BigDecimal("1e-100").toPlainString()
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, (DilutionCalculator.calculate(input) as DilutionResult.Success).volumeFromStockMl.toExactString())
        }
    }

    @Test(timeout = 5000)
    fun `all calculator fields reject malformed and resource-exhausting values`() {
        val invalid = listOf("", " ", "NaN", "Infinity", "1,25", "1,000", "1.2.3", "--1",
            "1e2147483647", "1e-2147483647", "1e1001", "1e-1001", "9".repeat(257))
        invalid.forEach { value ->
            assertEquals(3, (DilutionCalculator.calculate(dilution.copy(stockConcentration = value,
                finalConcentration = value, finalSolutionVolumeMl = value)) as DilutionResult.Invalid).errors.size)
            assertEquals(2, (RpdCalculator.calculate(value, value) as RpdResult.Invalid).errors.size)
            assertEquals(5, (MsMsdCalculator.calculate(MsMsdInput(value, value, value, value, value))
                as MsMsdResult.Invalid).errors.size)
            assertTrue(UnitConverter.convert(value, MetricUnit.GRAM, MetricUnit.MICROGRAM) is UnitConversionResult.Invalid)
            assertEquals(3, (MolarityMassCalculator.calculate(MolarityMassInput(value, value,
                MolarityVolumeUnit.LITER, value)) as MolarityMassResult.Invalid).errors.size)
        }
    }

    @Test
    fun `RPD is invariant across very small and large concentration scales`() {
        listOf("0.000000000001" to "0.0000000000012", "1e100" to "1.2e100", " 0.1 " to "+0.12")
            .forEach { (a, b) ->
                assertEquals("18.18%", (RpdCalculator.calculate(a, b) as RpdResult.Success).formattedPercent)
            }
        assertEquals("200.00%", (RpdCalculator.calculate("0", "0.1") as RpdResult.Success).formattedPercent)
        assertEquals("600.00%", (RpdCalculator.calculate("-1", "2") as RpdResult.Success).formattedPercent)
        assertEquals(RpdResult.ZeroAverage, RpdCalculator.calculate("-0.1", "0.1"))
    }

    @Test
    fun `MS MSD decimal and scaled examples use uncorrected concentrations`() {
        for (scale in listOf("0.001", "1", "1e100", "1e-100")) {
            fun scaled(value: String) = BigDecimal(value).multiply(BigDecimal(scale)).toString()
            val result = MsMsdCalculator.calculate(MsMsdInput(scaled("5"), "2.5",
                scaled("50"), scaled("55"), scaled("50"))) as MsMsdResult.Success
            assertEquals(0, BigDecimal("12.5").multiply(BigDecimal(scale))
                .compareTo(result.calculation.originalSourceConcentration))
            assertEquals("100.00%", result.formattedMsRecovery)
            assertEquals("90.00%", result.formattedMsdRecovery)
            assertEquals("9.52%", result.formattedMsMsdRpd)
        }
        val zeroRecovery = MsMsdCalculator.calculate(spike.copy(msResult = "5", msdResult = "5")) as MsMsdResult.Success
        assertEquals("0.00%", zeroRecovery.formattedMsRecovery)
        assertEquals("0.00%", zeroRecovery.formattedMsMsdRpd)
        val undefinedRpd = MsMsdCalculator.calculate(spike.copy(msResult = "0", msdResult = "0")) as MsMsdResult.Success
        assertEquals("50 PPB", undefinedRpd.formattedOriginalSourceConcentration)
        assertEquals("-10.00%", undefinedRpd.formattedMsRecovery)
        assertEquals("Undefined", undefinedRpd.formattedMsMsdRpd)
    }

    @Test
    fun `every metric pair agrees with independent SI exponents`() {
        val groups = listOf(
            listOf(MetricUnit.MICROGRAM, MetricUnit.MILLIGRAM, MetricUnit.GRAM),
            listOf(MetricUnit.MICROLITER, MetricUnit.MILLILITER, MetricUnit.LITER),
            listOf(MetricUnit.NANOGRAM_PER_LITER, MetricUnit.MICROGRAM_PER_LITER,
                MetricUnit.MILLIGRAM_PER_LITER, MetricUnit.GRAM_PER_LITER)
        )
        for (units in groups) for ((fromIndex, from) in units.withIndex()) {
            for ((toIndex, to) in units.withIndex()) for (input in listOf("0", "-1.25", "1e-100", "1e100")) {
                val expected = BigDecimal(input).scaleByPowerOfTen(3 * (fromIndex - toIndex))
                val actual = UnitConverter.convert(input, from, to) as UnitConversionResult.Success
                assertEquals("$input ${from.symbol} to ${to.symbol}", 0, expected.compareTo(actual.exactValue))
                assertEquals(0, expected.compareTo(BigDecimal(actual.formattedValue)))
            }
        }
        for (from in MetricUnit.entries) for (to in MetricUnit.entries) {
            if (from.category != to.category) assertTrue(UnitConverter.convert("1", from, to) is UnitConversionResult.Invalid)
        }
    }

    @Test
    fun `numeric limits include their boundaries`() {
        listOf("1e1000", "1e-1000", "9".repeat(256)).forEach {
            assertEquals(BigDecimal(it), parseLabDecimal(it))
        }
    }
}
