package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.*
import org.junit.Test

class DilutionPreparationTest {
    private fun calculate(stock: String = "1", target: String = "1", finalMl: String = "1") =
        DilutionCalculator.calculate(DilutionInput(stock, ConcentrationUnit.PPM,
            target, ConcentrationUnit.PPM, finalMl)) as DilutionResult.Success

    @Test fun `unit selection uses exact volume around one mL and zero`() {
        for ((volume, transfer) in listOf("0.9999999999999" to "≈ 1000 µL",
                "1" to "1 mL", "1.0000000000001" to "≈ 1 mL")) {
            val result = calculate(finalMl = volume)
            assertTrue(result.preparationInstruction.startsWith("Transfer $transfer of stock"))
            assertEquals(volume, result.volumeFromStockMl.toExactString())
        }
        val zero = calculate(target = "0", finalMl = "50")
        assertEquals("Transfer 0 mL of stock and make up to 50 mL final solution volume.",
            zero.preparationInstruction)
        assertFalse(zero.isApproximate)
    }

    @Test fun `terminating decimal transfers and final volumes stay exact`() {
        val cases = listOf(
            Triple("0.625", "625 µL", "0.625"),
            Triple("0.000625", "0.625 µL", "0.000625"),
            Triple("2.5", "2.5 mL", "2.5"),
            Triple("0.000123456789123", "0.123456789123 µL", "0.000123456789123")
        )
        for ((volume, transfer, exact) in cases) {
            // Final volume 10 keeps long target values out of the final-volume display.
            val result = calculate(stock = "10", target = volume, finalMl = "10")
            assertEquals("Transfer $transfer of stock and make up to 10 mL final solution volume.",
                result.preparationInstruction)
            assertEquals(exact, result.volumeFromStockMl.toExactString())
            assertFalse(result.isApproximate)
        }
    }

    @Test fun `repeating fractions convert units without parsing or intermediate rounding`() {
        for ((stock, transfer, exactTransfer) in listOf(Triple("3", "333.333", "1000/3"),
                Triple("6", "166.667", "500/3"), Triple("7", "142.857", "1000/7"))) {
            val result = calculate(stock = stock)
            assertEquals(BigInteger.ONE, result.volumeFromStockMl.numerator)
            assertEquals(BigInteger(stock), result.volumeFromStockMl.denominator)
            assertEquals("Transfer ≈ $transfer µL of stock and make up to 1 mL final solution volume.",
                result.preparationInstruction)
            assertTrue(result.isApproximate)
            assertTrue(result.calculationSteps.any { it.contains("$exactTransfer µL") })
        }
    }

    @Test(timeout = 5000) fun `long repeat cycles stay bounded and retain the exact value`() {
        val result = calculate(stock = "1000000007")
        assertEquals("1/1000000007", result.volumeFromStockMl.toExactString())
        assertEquals("Transfer ≈ 0.000001 µL of stock and make up to 1 mL final solution volume.",
            result.preparationInstruction)
        assertTrue(result.calculationSteps.any { it.contains("1000/1000000007 µL") })
    }

    @Test fun `long terminating expansions are approximated only in preparation text`() {
        val result = calculate(target = "0.123456789123456789")
        assertEquals("Transfer ≈ 123.457 µL of stock and make up to 1 mL final solution volume.",
            result.preparationInstruction)
        assertEquals("0.123456789123456789", result.volumeFromStockMl.toExactString())
        assertTrue(result.calculationSteps.any { it.contains("123.456789123456789 µL") })
        val longFinal = calculate(finalMl = "12.3456789123456789")
        assertEquals("Transfer ≈ 12.3457 mL of stock and make up to ≈ 12.3457 mL final solution volume.",
            longFinal.preparationInstruction)
        assertEquals(BigDecimal("12.3456789123456789"), longFinal.finalSolutionVolumeMl)
        assertTrue(longFinal.calculationSteps.any { it.contains("Exact final solution volume: 12.3456789123456789 mL") })
    }

    @Test fun `extreme terminating quantities use exact compact scientific notation`() {
        val tiny = calculate(finalMl = "1e-1000")
        assertEquals("Transfer 1E-997 µL of stock and make up to 1E-1000 mL final solution volume.",
            tiny.preparationInstruction)
        assertFalse(tiny.isApproximate)
        val huge = calculate(finalMl = "1e1000")
        assertEquals("Transfer 1E+1000 mL of stock and make up to 1E+1000 mL final solution volume.",
            huge.preparationInstruction)
        assertFalse(huge.isApproximate)
    }

    @Test fun `converted concentration example and decimal products keep unchanged mathematical results`() {
        val input = DilutionInput("2.5", ConcentrationUnit.PPM, "125", ConcentrationUnit.PPB, "12.5")
        val result = DilutionCalculator.calculate(input) as DilutionResult.Success
        assertEquals("0.625", result.volumeFromStockMl.toExactString())
        assertEquals("Transfer 625 µL of stock and make up to 12.5 mL final solution volume.",
            result.preparationInstruction)
        // Independently check C₁V₁ = C₂V₂ by cross-multiplying the rational result.
        for (stock in listOf("3", "7", "1000000007", "0.000123456789")) {
            val target = "0.000000001"
            val final = "123.456789123456789"
            val value = calculate(stock, target, final).volumeFromStockMl
            assertEquals(0, BigDecimal(stock).multiply(BigDecimal(value.numerator)).compareTo(
                BigDecimal(target).multiply(BigDecimal(final)).multiply(BigDecimal(value.denominator))))
        }
    }
}
