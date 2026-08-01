package com.example.labcalculator.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RpdCalculatorTest {
    @Test
    fun `10 and 12 produces 18 point 18 percent`() {
        val result = requireSuccess(RpdCalculator.calculate("10", "12"))

        assertEquals("18.18%", result.formattedPercent)
        assertTrue(result.calculationSteps[0].contains("|10 − 12| = 2"))
        assertTrue(result.calculationSteps[1].contains("(10 + 12) ÷ 2 = 11"))
        assertTrue(result.calculationSteps[3].contains("18.181818%"))
        assertTrue(result.calculationSteps[3].contains("for display only"))
        assertTrue(result.calculationSteps.last().contains(result.formattedPercent))
    }

    @Test
    fun `5 and 5 produces zero percent`() {
        val result = requireSuccess(RpdCalculator.calculate("5", "5"))
        assertEquals("0.00%", result.formattedPercent)
        assertTrue(result.calculationSteps.last().contains("0.00%"))
    }

    @Test
    fun `100 and 110 produces 9 point 52 percent`() {
        assertEquals(
            "9.52%",
            requireSuccess(RpdCalculator.calculate("100", "110")).formattedPercent
        )
    }

    @Test
    fun `reversing inputs produces the same result`() {
        assertEquals(
            requireSuccess(RpdCalculator.calculate("10", "12")).formattedPercent,
            requireSuccess(RpdCalculator.calculate("12", "10")).formattedPercent
        )
    }

    @Test
    fun `two zero values produce zero-average error`() {
        assertEquals(RpdResult.ZeroAverage, RpdCalculator.calculate("0", "0"))
        assertEquals(
            "RPD cannot be calculated when the average is zero.",
            RpdResult.ZeroAverage.MESSAGE
        )
    }

    @Test
    fun `blank entries are handled without crashing`() {
        val result = RpdCalculator.calculate("", " ")

        assertTrue(result is RpdResult.Invalid)
        assertEquals(2, (result as RpdResult.Invalid).errors.size)
    }

    @Test
    fun `nonnumeric entries are handled without crashing`() {
        val result = RpdCalculator.calculate("sample", "12x")

        assertTrue(result is RpdResult.Invalid)
        assertEquals(2, (result as RpdResult.Invalid).errors.size)
    }

    private fun requireSuccess(result: RpdResult): RpdResult.Success {
        assertTrue("Expected a successful result but received $result", result is RpdResult.Success)
        return result as RpdResult.Success
    }
}
