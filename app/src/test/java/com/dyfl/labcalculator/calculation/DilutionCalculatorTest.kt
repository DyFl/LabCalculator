package com.dyfl.labcalculator.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DilutionCalculatorTest {
    @Test
    fun `10 ppm stock and 200 ppb final in 50 mL requires 1 mL`() {
        val result = calculate(
            stock = "10",
            stockUnit = ConcentrationUnit.PPM,
            final = "200",
            finalUnit = ConcentrationUnit.PPB,
            volume = "50"
        )

        assertSuccessfulCalculation(result, "1")
        val steps = (result as DilutionResult.Success).calculationSteps
        assertTrue(steps.any { it.contains("10,000 PPB") })
        assertTrue(steps.any { it.contains("200 PPB × 50 mL") })
    }

    @Test
    fun `1 ppm stock and 50 ppb final in 50 mL requires 2 point 5 mL`() {
        val result = calculate(
            stock = "1",
            stockUnit = ConcentrationUnit.PPM,
            final = "50",
            finalUnit = ConcentrationUnit.PPB,
            volume = "50"
        )

        assertSuccessfulCalculation(result, "2.5")
    }

    @Test
    fun `repeating result retains an exact fraction`() {
        val result = calculate(
            stock = "3",
            stockUnit = ConcentrationUnit.PPM,
            final = "1",
            finalUnit = ConcentrationUnit.PPM,
            volume = "1"
        )

        assertSuccessfulCalculation(result, "1/3")
        assertTrue(
            (result as DilutionResult.Success).calculationSteps.any {
                it.contains("1000/3 µL (exact fraction; nonterminating decimal)")
            }
        )
    }

    @Test
    fun `blank fields return an error for every field`() {
        val result = calculate(stock = "", final = " ", volume = "")

        val errors = requireInvalid(result)
        assertEquals(
            setOf(
                DilutionField.STOCK_CONCENTRATION,
                DilutionField.FINAL_CONCENTRATION,
                DilutionField.FINAL_SOLUTION_VOLUME
            ),
            errors.map { it.field }.toSet()
        )
    }

    @Test
    fun `invalid text is rejected`() {
        val result = calculate(stock = "ten", final = "1", volume = "50")

        assertTrue(
            requireInvalid(result).any {
                it.field == DilutionField.STOCK_CONCENTRATION &&
                    it.message.contains("valid number")
            }
        )
    }

    @Test
    fun `zero stock concentration is rejected instead of dividing by zero`() {
        val result = calculate(stock = "0", final = "0", volume = "50")

        assertTrue(
            requireInvalid(result).any { it.field == DilutionField.STOCK_CONCENTRATION }
        )
    }

    @Test
    fun `negative concentrations and nonpositive volume are rejected`() {
        val result = calculate(stock = "-1", final = "-2", volume = "0")

        val fields = requireInvalid(result).map { it.field }.toSet()
        assertEquals(
            setOf(
                DilutionField.STOCK_CONCENTRATION,
                DilutionField.FINAL_CONCENTRATION,
                DilutionField.FINAL_SOLUTION_VOLUME
            ),
            fields
        )
    }

    @Test
    fun `final concentration cannot exceed converted stock concentration`() {
        val result = calculate(
            stock = "1",
            stockUnit = ConcentrationUnit.PPM,
            final = "1001",
            finalUnit = ConcentrationUnit.PPB,
            volume = "50"
        )

        assertTrue(
            requireInvalid(result).any { it.field == DilutionField.FINAL_CONCENTRATION }
        )
    }

    @Test
    fun `stock edit invalidates the cross-field error and preserves an unrelated volume error`() {
        val input = DilutionInput("1", ConcentrationUnit.PPM, "2000", ConcentrationUnit.PPB, "50")
        val comparisonErrors = requireInvalid(DilutionCalculator.calculate(input))
        val volumeErrors = requireInvalid(DilutionCalculator.calculate(input.copy(
            stockConcentration = "3", finalSolutionVolumeMl = "0")))
        val errors = (comparisonErrors + volumeErrors).associate { it.field to it.message }

        val remaining = DilutionCalculator.errorsAfterEdit(errors, DilutionField.STOCK_CONCENTRATION)

        assertEquals(volumeErrors.associate { it.field to it.message }, remaining)
        assertSuccessfulCalculation(DilutionCalculator.calculate(input.copy(stockConcentration = "3")),
            "100/3")
    }

    @Test
    fun `stock edit preserves independent target validation`() {
        for (target in listOf("", "-1", "invalid")) {
            val errors = requireInvalid(calculate(stock = "0", final = target, volume = "0"))
                .associate { it.field to it.message }
            assertEquals(errors - DilutionField.STOCK_CONCENTRATION,
                DilutionCalculator.errorsAfterEdit(errors, DilutionField.STOCK_CONCENTRATION))
        }
    }

    @Test
    fun `volume edit does not invalidate a concentration comparison`() {
        val errors = requireInvalid(calculate(stock = "1", final = "2", volume = "50"))
            .associate { it.field to it.message }
        assertEquals(errors,
            DilutionCalculator.errorsAfterEdit(errors, DilutionField.FINAL_SOLUTION_VOLUME))
        assertTrue(DilutionCalculator.errorsAfterEdit(errors, DilutionField.FINAL_CONCENTRATION).isEmpty())
    }

    @Test
    fun `zero final concentration returns zero stock volume`() {
        val result = calculate(stock = "10", final = "0", volume = "50")

        assertSuccessfulCalculation(result, "0")
    }

    private fun calculate(
        stock: String,
        stockUnit: ConcentrationUnit = ConcentrationUnit.PPM,
        final: String,
        finalUnit: ConcentrationUnit = ConcentrationUnit.PPM,
        volume: String
    ): DilutionResult = DilutionCalculator.calculate(
        DilutionInput(
            stockConcentration = stock,
            stockUnit = stockUnit,
            finalConcentration = final,
            finalUnit = finalUnit,
            finalSolutionVolumeMl = volume
        )
    )

    private fun requireInvalid(result: DilutionResult): List<DilutionError> {
        assertTrue("Expected an invalid result but received $result", result is DilutionResult.Invalid)
        return (result as DilutionResult.Invalid).errors
    }

    private fun assertSuccessfulCalculation(result: DilutionResult, expectedVolume: String) {
        assertTrue("Expected a successful result but received $result", result is DilutionResult.Success)
        result as DilutionResult.Success
        assertEquals(expectedVolume, result.volumeFromStockMl.toExactString())
        assertTrue(result.calculationSteps.isNotEmpty())
        assertTrue(result.calculationSteps.any { it.contains("$expectedVolume mL") })
    }
}
