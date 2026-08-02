package com.example.labcalculator.calculation

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MsMsdCalculatorTest {
    @Test
    fun `confirmed diluted example keeps spike isolated from dilution factor`() {
        val result = calculateSuccess("5", "10", "50", "55", "50")

        assertDecimalEquals("50", result.calculation.originalSourceConcentration)
        assertDecimalEquals("50", result.calculation.msRecoveredSpikeConcentration)
        assertDecimalEquals("45", result.calculation.msdRecoveredSpikeConcentration)
        assertEquals("100.00%", result.formattedMsRecovery)
        assertEquals("90.00%", result.formattedMsdRecovery)
        assertEquals("9.52%", result.formattedMsMsdRpd)
        assertTrue(result.calculationSections[0].steps.last().contains("not multiplied by 10"))
    }

    @Test
    fun `no dilution example calculates expected recoveries and RPD`() {
        val result = calculateSuccess("10", "1", "40", "48", "50")

        assertDecimalEquals("10", result.calculation.originalSourceConcentration)
        assertEquals("95.00%", result.formattedMsRecovery)
        assertEquals("100.00%", result.formattedMsdRecovery)
        assertEquals("4.08%", result.formattedMsMsdRpd)
    }

    @Test
    fun `changing only dilution factor changes only original source concentration`() {
        val noDilution = calculateSuccess("5", "1", "50", "55", "50")
        val diluted = calculateSuccess("5", "10.5", "50", "55", "50")

        assertDecimalEquals("5", noDilution.calculation.originalSourceConcentration)
        assertDecimalEquals("52.5", diluted.calculation.originalSourceConcentration)
        assertEquals(noDilution.formattedMsRecovery, diluted.formattedMsRecovery)
        assertEquals(noDilution.formattedMsdRecovery, diluted.formattedMsdRecovery)
        assertEquals(noDilution.formattedMsMsdRpd, diluted.formattedMsMsdRpd)
        assertEquals(
            noDilution.calculation.msRecovery,
            diluted.calculation.msRecovery
        )
        assertEquals(
            noDilution.calculation.msdRecovery,
            diluted.calculation.msdRecovery
        )
        assertEquals(noDilution.calculation.msMsdRpd, diluted.calculation.msMsdRpd)
    }

    @Test
    fun `swapping MS and MSD does not change RPD`() {
        val forward = calculateSuccess("5", "10", "50", "55", "50")
        val reversed = calculateSuccess("5", "10", "50", "50", "55")

        assertEquals(forward.calculation.msMsdRpd, reversed.calculation.msMsdRpd)
        assertEquals(forward.formattedMsMsdRpd, reversed.formattedMsMsdRpd)
    }

    @Test
    fun `identical MS and MSD values produce zero RPD`() {
        val result = calculateSuccess("5", "2", "50", "55", "55")

        assertEquals("0.00%", result.formattedMsMsdRpd)
    }

    @Test
    fun `recovery over one hundred percent remains visible`() {
        val result = calculateSuccess("5", "1", "50", "65", "60")

        assertEquals("120.00%", result.formattedMsRecovery)
        assertTrue(result.calculation.msRecovery.roundedValue() > BigDecimal("100"))
    }

    @Test
    fun `negative recovery remains negative and is not rejected`() {
        val result = calculateSuccess("10", "1", "50", "5", "8")

        assertEquals("-10.00%", result.formattedMsRecovery)
        assertEquals("-4.00%", result.formattedMsdRecovery)
    }

    @Test
    fun `RPD above a typical QC limit remains visible`() {
        val result = calculateSuccess("0", "1", "10", "1", "100")

        assertEquals("196.04%", result.formattedMsMsdRpd)
        assertTrue(result.calculation.msMsdRpd.roundedValue() > BigDecimal("100"))
    }

    @Test
    fun `zero spike concentration is rejected`() {
        assertFieldError(
            calculate("5", "1", "0", "55", "50"),
            MsMsdField.FINAL_SPIKE_CONCENTRATION
        )
    }

    @Test
    fun `zero and negative dilution factors are rejected`() {
        assertFieldError(
            calculate("5", "0", "50", "55", "50"),
            MsMsdField.DILUTION_FACTOR
        )
        assertFieldError(
            calculate("5", "-0.5", "50", "55", "50"),
            MsMsdField.DILUTION_FACTOR
        )
    }

    @Test
    fun `zero MS MSD average returns validation error`() {
        assertEquals(
            MsMsdResult.ZeroAverage,
            calculate("5", "1", "50", "10", "-10")
        )
    }

    @Test
    fun `calculation steps use the same intermediate values as the result`() {
        val result = calculateSuccess("5", "10", "50", "55", "50")
        val sections = result.calculationSections.associateBy { it.title }

        assertTrue(sections.getValue("Dilution handling").steps[1].contains("50"))
        assertTrue(sections.getValue("MS recovery").steps[1].contains("50"))
        assertTrue(sections.getValue("MSD recovery").steps[1].contains("45"))
        assertTrue(sections.getValue("MS/MSD RPD").steps[0].contains("= 5"))
        assertTrue(sections.getValue("MS/MSD RPD").steps[1].contains("= 52.5"))
        assertTrue(
            sections.getValue("MS recovery").steps.last()
                .contains(result.formattedMsRecovery)
        )
        assertTrue(
            sections.getValue("MSD recovery").steps.last()
                .contains(result.formattedMsdRecovery)
        )
        assertTrue(
            sections.getValue("MS/MSD RPD").steps.last()
                .contains(result.formattedMsMsdRpd)
        )
    }

    @Test
    fun `blank nonnumeric and nonfinite inputs are rejected without crashing`() {
        val blank = calculate("", " ", "50", "55", "50")
        assertTrue(blank is MsMsdResult.Invalid)
        assertEquals(2, (blank as MsMsdResult.Invalid).errors.size)

        val invalid = calculate("sample", "1", "Infinity", "NaN", "50")
        assertTrue(invalid is MsMsdResult.Invalid)
        assertEquals(3, (invalid as MsMsdResult.Invalid).errors.size)
    }

    private fun calculate(
        rawSource: String,
        dilutionFactor: String,
        spike: String,
        ms: String,
        msd: String
    ): MsMsdResult = MsMsdCalculator.calculate(
        MsMsdInput(rawSource, dilutionFactor, spike, ms, msd)
    )

    private fun calculateSuccess(
        rawSource: String,
        dilutionFactor: String,
        spike: String,
        ms: String,
        msd: String
    ): MsMsdResult.Success {
        val result = calculate(rawSource, dilutionFactor, spike, ms, msd)
        assertTrue("Expected success but received $result", result is MsMsdResult.Success)
        return result as MsMsdResult.Success
    }

    private fun assertFieldError(result: MsMsdResult, field: MsMsdField) {
        assertTrue("Expected validation error but received $result", result is MsMsdResult.Invalid)
        result as MsMsdResult.Invalid
        assertTrue(result.errors.any { it.field == field })
    }

    private fun assertDecimalEquals(expected: String, actual: BigDecimal) {
        assertEquals(0, actual.compareTo(BigDecimal(expected)))
    }
}
