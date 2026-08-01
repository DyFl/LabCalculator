package com.example.labcalculator.calculation

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterTest {
    @Test
    fun `milligrams convert to micrograms exactly`() {
        assertConversion("1", MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM, "1000")
    }

    @Test
    fun `micrograms convert back to milligrams exactly`() {
        assertConversion("1000", MetricUnit.MICROGRAM, MetricUnit.MILLIGRAM, "1")
    }

    @Test
    fun `liters and milliliters convert in both directions`() {
        assertConversion("2.5", MetricUnit.LITER, MetricUnit.MILLILITER, "2500")
        assertConversion("2500", MetricUnit.MILLILITER, MetricUnit.LITER, "2.5")
    }

    @Test
    fun `mass concentration units convert exactly`() {
        assertConversion(
            "0.25",
            MetricUnit.MILLIGRAM_PER_LITER,
            MetricUnit.MICROGRAM_PER_LITER,
            "250"
        )
        assertConversion(
            "250",
            MetricUnit.MICROGRAM_PER_LITER,
            MetricUnit.MILLIGRAM_PER_LITER,
            "0.25"
        )
    }

    @Test
    fun `zero converts to plain zero`() {
        assertConversion("0", MetricUnit.GRAM, MetricUnit.MICROGRAM, "0")
    }

    @Test
    fun `small decimal remains readable without scientific notation`() {
        assertConversion("0.000001", MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM, "0.001")
    }

    @Test
    fun `large value converts without losing precision`() {
        assertConversion(
            "1000000",
            MetricUnit.GRAM,
            MetricUnit.MICROGRAM,
            "1000000000000"
        )
    }

    @Test
    fun `different categories are rejected`() {
        val result = UnitConverter.convert("1", MetricUnit.GRAM, MetricUnit.LITER)

        assertTrue(result is UnitConversionResult.Invalid)
    }

    @Test
    fun `blank and nonnumeric conversion values are rejected`() {
        assertTrue(
            UnitConverter.convert("", MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM) is
                UnitConversionResult.Invalid
        )
        assertTrue(
            UnitConverter.convert("not a number", MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM) is
                UnitConversionResult.Invalid
        )
    }

    @Test
    fun `explanation uses an exact grouped factor`() {
        assertEquals(
            "1 mg = 1,000 µg.",
            UnitConverter.explanation(MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM)
        )
    }

    @Test
    fun `25 milligrams steps use same factor and result as conversion`() {
        val result = UnitConverter.convert("25", MetricUnit.MILLIGRAM, MetricUnit.MICROGRAM)
        assertTrue(result is UnitConversionResult.Success)
        result as UnitConversionResult.Success

        assertEquals("25000", result.formattedValue)
        assertTrue(result.calculationSteps[0].contains("1 mg = 1,000 µg"))
        assertTrue(result.calculationSteps[1].contains("25 mg × (1,000 µg ÷ 1 mg)"))
        assertTrue(result.calculationSteps.last().contains("25,000 µg"))
    }

    private fun assertConversion(
        value: String,
        from: MetricUnit,
        to: MetricUnit,
        expected: String
    ) {
        val result = UnitConverter.convert(value, from, to)
        assertTrue("Expected a successful conversion but received $result", result is UnitConversionResult.Success)
        result as UnitConversionResult.Success
        assertEquals(0, result.exactValue.compareTo(BigDecimal(expected)))
        assertEquals(expected, result.formattedValue)
        assertTrue(result.calculationSteps.isNotEmpty())
        assertTrue(
            result.calculationSteps.last().replace(",", "")
                .contains("$expected ${to.symbol}")
        )
    }
}
