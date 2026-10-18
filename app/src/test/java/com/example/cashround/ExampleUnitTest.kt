package com.example.cashround

import com.example.cashround.data.InMemoryKeyValueStore
import com.example.cashround.domain.CashCalculationOverflowException
import com.example.cashround.domain.CashRoundCalculator
import com.example.cashround.domain.CashRoundFormatter
import com.example.cashround.domain.CashRoundValidator
import com.example.cashround.domain.CurrencyRepository
import com.example.cashround.domain.MoneyFormatter
import com.example.cashround.domain.MoneyParser
import com.example.cashround.domain.ValidationResult
import com.example.cashround.model.AppThemeMode
import com.example.cashround.model.CalculationState
import com.example.cashround.model.CashRoundHistory
import com.example.cashround.model.CashRoundStatus
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.model.CurrencyOption
import com.example.cashround.repository.CashRoundHistoryRepository
import com.example.cashround.repository.SettingsRepository
import com.example.cashround.ui.CashRoundViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CashRoundComprehensiveTest {

    private val usd = CurrencyRepository.USD
    private val eur = CurrencyRepository.EUR
    private val gbp = CurrencyRepository.GBP
    private val pkr = CurrencyRepository.PKR
    private val aed = CurrencyRepository.AED
    private val sar = CurrencyRepository.SAR
    private val cad = CurrencyRepository.CAD
    private val aud = CurrencyRepository.AUD
    private val inr = CurrencyRepository.INR
    private val jpy = CurrencyRepository.JPY
    private val cny = CurrencyRepository.CNY

    // 1. Round nearest (CRITICAL TEST — NEAREST: $47.83 to $5 -> $50.00, +$2.17)
    @Test
    fun test01_roundNearest_criticalExample() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4783L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.NEAREST,
            currency = usd
        )
        assertEquals(5000L, result.roundedAmountMinorUnits)
        assertEquals(217L, result.adjustmentMinorUnits)
        assertFalse(result.wasAlreadyRounded)
        assertEquals(CashRoundStatus.ROUNDED_UP, result.status)
        assertEquals("$50.00", CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, usd))
        assertEquals("+$2.17", CashRoundFormatter.formatAdjustment(result.adjustmentMinorUnits, usd))
    }

    // 2. Round up (CRITICAL TEST — ROUND UP: $47.83 to $5 -> $50.00, +$2.17)
    @Test
    fun test02_roundUp_criticalExample() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4783L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.UP,
            currency = usd
        )
        assertEquals(5000L, result.roundedAmountMinorUnits)
        assertEquals(217L, result.adjustmentMinorUnits)
        assertFalse(result.wasAlreadyRounded)
        assertEquals(CashRoundStatus.ROUNDED_UP, result.status)
        assertEquals("$50.00", CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, usd))
        assertEquals("+$2.17", CashRoundFormatter.formatAdjustment(result.adjustmentMinorUnits, usd))
    }

    // 3. Round down (CRITICAL TEST — ROUND DOWN: $47.83 to $5 -> $45.00, -$2.83)
    @Test
    fun test03_roundDown_criticalExample() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4783L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.DOWN,
            currency = usd
        )
        assertEquals(4500L, result.roundedAmountMinorUnits)
        assertEquals(-283L, result.adjustmentMinorUnits)
        assertFalse(result.wasAlreadyRounded)
        assertEquals(CashRoundStatus.ROUNDED_DOWN, result.status)
        assertEquals("$45.00", CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, usd))
        assertEquals("-$2.83", CashRoundFormatter.formatAdjustment(result.adjustmentMinorUnits, usd))
    }

    // 4. Already rounded amount (CRITICAL TEST — EXACT MULTIPLE: $50 to $5 -> $50, $0.00, ALREADY_ROUNDED)
    @Test
    fun test04_alreadyRoundedAmount_allModes() {
        for (mode in CashRoundingMode.entries) {
            val result = CashRoundCalculator.calculate(
                amountMinorUnits = 5000L,
                denominationMinorUnits = 500L,
                mode = mode,
                currency = usd
            )
            assertEquals(5000L, result.roundedAmountMinorUnits)
            assertEquals(0L, result.adjustmentMinorUnits)
            assertTrue(result.wasAlreadyRounded)
            assertEquals(CashRoundStatus.ALREADY_ROUNDED, result.status)
            assertEquals("$0.00", CashRoundFormatter.formatAdjustment(result.adjustmentMinorUnits, usd))
        }
    }

    // 5. Amount below denomination (CRITICAL TEST — BELOW DENOMINATION: $3 to $5)
    @Test
    fun test05_amountBelowDenomination() {
        val nearest = CashRoundCalculator.calculate(300L, 500L, CashRoundingMode.NEAREST, usd)
        val down = CashRoundCalculator.calculate(300L, 500L, CashRoundingMode.DOWN, usd)
        val up = CashRoundCalculator.calculate(300L, 500L, CashRoundingMode.UP, usd)

        assertEquals(500L, nearest.roundedAmountMinorUnits)
        assertEquals(200L, nearest.adjustmentMinorUnits)

        assertEquals(0L, down.roundedAmountMinorUnits)
        assertEquals(-300L, down.adjustmentMinorUnits)

        assertEquals(500L, up.roundedAmountMinorUnits)
        assertEquals(200L, up.adjustmentMinorUnits)
    }

    // 6. Amount equal to denomination ($5 to $5)
    @Test
    fun test06_amountEqualToDenomination() {
        for (mode in CashRoundingMode.entries) {
            val res = CashRoundCalculator.calculate(500L, 500L, mode, usd)
            assertEquals(500L, res.roundedAmountMinorUnits)
            assertEquals(0L, res.adjustmentMinorUnits)
            assertTrue(res.wasAlreadyRounded)
            assertEquals(CashRoundStatus.ALREADY_ROUNDED, res.status)
        }
    }

    // 7. Amount above denomination ($12.30 to $5)
    @Test
    fun test07_amountAboveDenomination() {
        val nearest = CashRoundCalculator.calculate(1230L, 500L, CashRoundingMode.NEAREST, usd)
        val down = CashRoundCalculator.calculate(1230L, 500L, CashRoundingMode.DOWN, usd)
        val up = CashRoundCalculator.calculate(1230L, 500L, CashRoundingMode.UP, usd)

        assertEquals(1000L, nearest.roundedAmountMinorUnits)
        assertEquals(1000L, down.roundedAmountMinorUnits)
        assertEquals(1500L, up.roundedAmountMinorUnits)
    }

    // 8. Exact halfway case (CRITICAL TEST — HALF CASE: $47.50 to $5 -> $50.00)
    @Test
    fun test08_exactHalfwayCase_roundsHalfUp() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4750L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.NEAREST,
            currency = usd
        )
        assertEquals(5000L, result.roundedAmountMinorUnits)
        assertEquals(250L, result.adjustmentMinorUnits)
        assertEquals(CashRoundStatus.ROUNDED_UP, result.status)
    }

    // 9. Nearest lower result ($46.20 to $5 -> $45.00)
    @Test
    fun test09_nearestLowerResult() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4620L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.NEAREST,
            currency = usd
        )
        assertEquals(4500L, result.roundedAmountMinorUnits)
        assertEquals(-120L, result.adjustmentMinorUnits)
        assertEquals(CashRoundStatus.ROUNDED_DOWN, result.status)
    }

    // 10. Nearest upper result ($48.90 to $5 -> $50.00)
    @Test
    fun test10_nearestUpperResult() {
        val result = CashRoundCalculator.calculate(
            amountMinorUnits = 4890L,
            denominationMinorUnits = 500L,
            mode = CashRoundingMode.NEAREST,
            currency = usd
        )
        assertEquals(5000L, result.roundedAmountMinorUnits)
        assertEquals(110L, result.adjustmentMinorUnits)
        assertEquals(CashRoundStatus.ROUNDED_UP, result.status)
    }

    // 11. Zero amount (CRITICAL TEST — ZERO AMOUNT: $0 to $5 -> $0, ALREADY_ROUNDED)
    @Test
    fun test11_zeroAmount() {
        for (mode in CashRoundingMode.entries) {
            val result = CashRoundCalculator.calculate(0L, 500L, mode, usd)
            assertEquals(0L, result.roundedAmountMinorUnits)
            assertEquals(0L, result.adjustmentMinorUnits)
            assertTrue(result.wasAlreadyRounded)
            assertEquals(CashRoundStatus.ALREADY_ROUNDED, result.status)
        }
    }

    // 12. Negative amount rejected
    @Test
    fun test12_negativeAmountRejected() {
        val validation = CashRoundValidator.validateAmountInput("-10.00", usd)
        assertTrue(validation.isFailure)
        assertEquals("Cash amount cannot be negative.", validation.exceptionOrNull()?.message)

        try {
            CashRoundCalculator.calculate(-100L, 500L, CashRoundingMode.NEAREST, usd)
            org.junit.Assert.fail("Expected IllegalArgumentException for negative amount")
        } catch (e: IllegalArgumentException) {
            assertEquals("Cash amount cannot be negative.", e.message)
        }
    }

    // 13. Zero denomination rejected
    @Test
    fun test13_zeroDenominationRejected() {
        val validation = CashRoundValidator.validateDenominationInput("0", usd)
        assertTrue(validation.isFailure)
        assertEquals("Denomination must be greater than zero.", validation.exceptionOrNull()?.message)

        try {
            CashRoundCalculator.calculate(4783L, 0L, CashRoundingMode.NEAREST, usd)
            org.junit.Assert.fail("Expected IllegalArgumentException for zero denomination")
        } catch (e: IllegalArgumentException) {
            assertEquals("Denomination must be greater than zero.", e.message)
        }
    }

    // 14. Negative denomination rejected
    @Test
    fun test14_negativeDenominationRejected() {
        val validation = CashRoundValidator.validateDenominationInput("-5", usd)
        assertTrue(validation.isFailure)
        assertEquals("Denomination must be greater than zero.", validation.exceptionOrNull()?.message)

        try {
            CashRoundCalculator.calculate(4783L, -500L, CashRoundingMode.NEAREST, usd)
            org.junit.Assert.fail("Expected IllegalArgumentException for negative denomination")
        } catch (e: IllegalArgumentException) {
            assertEquals("Denomination must be greater than zero.", e.message)
        }
    }

    // 15. Custom denomination (e.g., 2.50, 25, 500, 1000)
    @Test
    fun test15_customDenomination() {
        val denomMinor = MoneyParser.parseToMinorUnits("2.50", usd, isDenomination = true).getOrThrow()
        assertEquals(250L, denomMinor)
        val result = CashRoundCalculator.calculate(4783L, denomMinor, CashRoundingMode.NEAREST, usd)
        // 4783 / 250 = 19 rem 33 -> lower is 19*250 = 4750 ($47.50), upper is 5000 ($50.00). Distance down = 33, up = 217
        assertEquals(4750L, result.roundedAmountMinorUnits)
        assertEquals(-33L, result.adjustmentMinorUnits)
    }

    // 16. Decimal denomination (CRITICAL TEST — DECIMAL DENOMINATION: $47.83 to $0.25 -> $47.75)
    @Test
    fun test16_decimalDenomination() {
        // $47.83 = 4783 cents, $0.25 = 25 cents.
        // 4783 = 191 * 25 + 8. Lower = 4775 ($47.75, dist 8), Upper = 4800 ($48.00, dist 17).
        // Nearest is $47.75.
        val result = CashRoundCalculator.calculate(4783L, 25L, CashRoundingMode.NEAREST, usd)
        assertEquals(4775L, result.roundedAmountMinorUnits)
        assertEquals(-8L, result.adjustmentMinorUnits)
        assertEquals("$47.75", CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, usd))
    }

    // 17. USD currency
    @Test
    fun test17_usdCurrency() {
        val res = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, usd)
        assertEquals("$50.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, usd))
    }

    // 18. EUR currency
    @Test
    fun test18_eurCurrency() {
        val res = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, eur)
        assertEquals("€50.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, eur))
    }

    // 19. GBP currency
    @Test
    fun test19_gbpCurrency() {
        val res = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, gbp)
        assertEquals("£50.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, gbp))
    }

    // 20. PKR currency (CRITICAL TEST — PKR: Rs. 1,247.50 to Rs. 50 -> Rs. 1,250.00, +Rs. 2.50)
    @Test
    fun test20_pkrCurrency_criticalExample() {
        val amount = MoneyParser.parseToMinorUnits("1247.50", pkr).getOrThrow()
        val denom = MoneyParser.parseToMinorUnits("50", pkr).getOrThrow()
        val res = CashRoundCalculator.calculate(amount, denom, CashRoundingMode.NEAREST, pkr)
        assertEquals(125000L, res.roundedAmountMinorUnits)
        assertEquals(250L, res.adjustmentMinorUnits)
        assertEquals("Rs. 1,250.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, pkr))
        assertEquals("+Rs. 2.50", CashRoundFormatter.formatAdjustment(res.adjustmentMinorUnits, pkr))
    }

    // 21. AED currency
    @Test
    fun test21_aedCurrency() {
        val res = CashRoundCalculator.calculate(1825L, 500L, CashRoundingMode.NEAREST, aed)
        assertEquals(2000L, res.roundedAmountMinorUnits)
        assertEquals("AED 20.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, aed))
    }

    // 22. SAR currency
    @Test
    fun test22_sarCurrency() {
        val res = CashRoundCalculator.calculate(1825L, 500L, CashRoundingMode.NEAREST, sar)
        assertEquals(2000L, res.roundedAmountMinorUnits)
        assertEquals("SAR 20.00", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, sar))
    }

    // 23. JPY currency (CRITICAL TEST — JPY: ¥4,780 to ¥500 -> ¥5,000, +¥220)
    @Test
    fun test23_jpyCurrency_criticalExample() {
        val amount = MoneyParser.parseToMinorUnits("4780", jpy).getOrThrow()
        val denom = MoneyParser.parseToMinorUnits("500", jpy).getOrThrow()
        assertEquals(4780L, amount)
        assertEquals(500L, denom)

        val res = CashRoundCalculator.calculate(amount, denom, CashRoundingMode.NEAREST, jpy)
        assertEquals(5000L, res.roundedAmountMinorUnits)
        assertEquals(220L, res.adjustmentMinorUnits)
        assertEquals("¥5,000", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, jpy))
        assertEquals("+¥220", CashRoundFormatter.formatAdjustment(res.adjustmentMinorUnits, jpy))
    }

    // 24. Zero-decimal currency precision validation (JPY rejects 0.50)
    @Test
    fun test24_zeroDecimalCurrencyRejectsDecimals() {
        val parseResult = MoneyParser.parseToMinorUnits("0.50", jpy, isDenomination = true)
        assertTrue(parseResult.isFailure)
    }

    // 25. Two-decimal currency precision validation (USD accepts 2 decimals, rejects 3 decimals)
    @Test
    fun test25_twoDecimalCurrencyPrecision() {
        assertTrue(MoneyParser.parseToMinorUnits("47.83", usd).isSuccess)
        assertTrue(MoneyParser.parseToMinorUnits("47.835", usd).isFailure)
        assertEquals(1000L, CashRoundCalculator.calculate(995L, 100L, CashRoundingMode.NEAREST, cad).roundedAmountMinorUnits)
        assertEquals(1000L, CashRoundCalculator.calculate(995L, 100L, CashRoundingMode.NEAREST, aud).roundedAmountMinorUnits)
        assertEquals(1000L, CashRoundCalculator.calculate(995L, 100L, CashRoundingMode.NEAREST, inr).roundedAmountMinorUnits)
        assertEquals(1000L, CashRoundCalculator.calculate(995L, 100L, CashRoundingMode.NEAREST, cny).roundedAmountMinorUnits)
    }

    // 26. Custom currency (0-4 decimal places, e.g. 3 decimal places like KWD/BHD)
    @Test
    fun test26_customCurrencyThreeDecimals() {
        val custom3Dec = CurrencyOption("BHD", "BD ", "Bahraini Dinar", 3)
        val amount = MoneyParser.parseToMinorUnits("12.345", custom3Dec).getOrThrow()
        val denom = MoneyParser.parseToMinorUnits("0.500", custom3Dec).getOrThrow()
        assertEquals(12345L, amount)
        assertEquals(500L, denom)

        val res = CashRoundCalculator.calculate(amount, denom, CashRoundingMode.NEAREST, custom3Dec)
        assertEquals(12500L, res.roundedAmountMinorUnits)
        assertEquals("BD 12.500", CashRoundFormatter.formatMoney(res.roundedAmountMinorUnits, custom3Dec))
    }

    // 27. Money parsing (commas, leading/trailing spaces, partial decimals)
    @Test
    fun test27_moneyParsing() {
        assertEquals(4783L, MoneyParser.parseToMinorUnits(" 47.83 ", usd).getOrThrow())
        assertEquals(125000L, MoneyParser.parseToMinorUnits("1,250.00", usd).getOrThrow())
        assertEquals(500L, MoneyParser.parseToMinorUnits("5", usd).getOrThrow())
        assertEquals(550L, MoneyParser.parseToMinorUnits("5.5", usd).getOrThrow())
        assertEquals(50L, MoneyParser.parseToMinorUnits(".5", usd).getOrThrow())
        assertTrue(MoneyParser.parseToMinorUnits("abc", usd).isFailure)
        assertTrue(MoneyParser.parseToMinorUnits("", usd).isFailure)
    }

    // 28. Money formatting (thousands separators, symbols, decimals)
    @Test
    fun test28_moneyFormatting() {
        assertEquals("$47.83", MoneyFormatter.formatMinorUnits(4783L, usd))
        assertEquals("$50.00", MoneyFormatter.formatMinorUnits(5000L, usd))
        assertEquals("Rs. 1,250.00", MoneyFormatter.formatMinorUnits(125000L, pkr))
        assertEquals("¥500", MoneyFormatter.formatMinorUnits(500L, jpy))
        assertEquals("¥1,234,567", MoneyFormatter.formatMinorUnits(1234567L, jpy))
    }

    // 29. Adjustment calculation
    @Test
    fun test29_adjustmentCalculation() {
        val upRes = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.UP, usd)
        val downRes = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.DOWN, usd)
        assertEquals(217L, upRes.adjustmentMinorUnits)
        assertEquals(-283L, downRes.adjustmentMinorUnits)
    }

    // 30. Positive adjustment formatting (+$2.17)
    @Test
    fun test30_positiveAdjustmentFormatting() {
        assertEquals("+$2.17", CashRoundFormatter.formatAdjustment(217L, usd))
    }

    // 31. Negative adjustment formatting (-$2.83 and CRITICAL TEST — ROUND DOWN TO ZERO: -$3.00)
    @Test
    fun test31_negativeAdjustmentFormattingAndRoundDownToZero() {
        assertEquals("-$2.83", CashRoundFormatter.formatAdjustment(-283L, usd))

        val downToZero = CashRoundCalculator.calculate(300L, 500L, CashRoundingMode.DOWN, usd)
        assertEquals(0L, downToZero.roundedAmountMinorUnits)
        assertEquals(-300L, downToZero.adjustmentMinorUnits)
        assertEquals("-$3.00", CashRoundFormatter.formatAdjustment(downToZero.adjustmentMinorUnits, usd))
    }

    // 32. Zero adjustment formatting ($0.00)
    @Test
    fun test32_zeroAdjustmentFormatting() {
        assertEquals("$0.00", CashRoundFormatter.formatAdjustment(0L, usd))
        assertEquals("¥0", CashRoundFormatter.formatAdjustment(0L, jpy))
    }

    // 33. Large amounts (CRITICAL TEST — LARGE VALUE)
    @Test
    fun test33_largeAmounts() {
        val largeAmount = 900_000_000_000_023L // $9,000,000,000,000.23
        val denom = 100L // $1.00
        val res = CashRoundCalculator.calculate(largeAmount, denom, CashRoundingMode.NEAREST, usd)
        assertEquals(900_000_000_000_000L, res.roundedAmountMinorUnits)
        assertEquals(-23L, res.adjustmentMinorUnits)
    }

    // 34. Overflow protection (CRITICAL TEST — OVERFLOW)
    @Test
    fun test34_overflowProtection() {
        // Long.MAX_VALUE is 9223372036854775807.
        // With denomination = 1000, quotient = 9223372036854775, remainder = 807.
        // (quotient + 1) * 1000 would exceed Long.MAX_VALUE and must throw CashCalculationOverflowException ("Amount is too large.")
        try {
            CashRoundCalculator.calculate(Long.MAX_VALUE, 1000L, CashRoundingMode.UP, usd)
            org.junit.Assert.fail("Expected CashCalculationOverflowException")
        } catch (e: CashCalculationOverflowException) {
            assertEquals("Amount is too large.", e.message)
        }

        // Also test ViewModel handles overflow gracefully without crashing
        val store = InMemoryKeyValueStore()
        val vm = CashRoundViewModel(CashRoundHistoryRepository(store), SettingsRepository(store))
        vm.onAmountChange("999999999999999999999999999")
        assertEquals("Amount is too large.", vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.result)
    }

    // 35. Quotient/remainder calculation
    @Test
    fun test35_quotientRemainderCalculation() {
        val res = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, usd)
        assertEquals(4500L, res.lowerMultipleMinorUnits)
        assertEquals(5000L, res.upperMultipleMinorUnits)
        assertEquals(283L, res.distanceDownMinorUnits)
        assertEquals(217L, res.distanceUpMinorUnits)
    }

    // 36. Rounding invariant: roundedAmountMinorUnits % denominationMinorUnits == 0
    @Test
    fun test36_roundingInvariant() {
        val amounts = listOf(0L, 1L, 37L, 250L, 499L, 500L, 4750L, 4783L, 999999L)
        val denoms = listOf(1L, 5L, 10L, 25L, 50L, 100L, 500L, 1000L, 10000L)
        for (a in amounts) {
            for (d in denoms) {
                for (mode in CashRoundingMode.entries) {
                    val res = CashRoundCalculator.calculate(a, d, mode, usd)
                    assertEquals(
                        "Invariant failed for A=$a, D=$d, mode=$mode",
                        0L,
                        res.roundedAmountMinorUnits % res.denominationMinorUnits
                    )
                }
            }
        }
    }

    // 37. Adjustment invariant: roundedAmountMinorUnits == originalAmountMinorUnits + adjustmentMinorUnits
    @Test
    fun test37_adjustmentInvariant() {
        val amounts = listOf(0L, 1L, 37L, 250L, 499L, 500L, 4750L, 4783L, 999999L)
        val denoms = listOf(1L, 5L, 10L, 25L, 50L, 100L, 500L, 1000L)
        for (a in amounts) {
            for (d in denoms) {
                for (mode in CashRoundingMode.entries) {
                    val res = CashRoundCalculator.calculate(a, d, mode, usd)
                    assertEquals(
                        res.roundedAmountMinorUnits,
                        res.originalAmountMinorUnits + res.adjustmentMinorUnits
                    )
                }
            }
        }
    }

    // 38. Round-down invariant: roundedAmount <= originalAmount
    @Test
    fun test38_roundDownInvariant() {
        val amounts = listOf(0L, 1L, 299L, 300L, 4783L, 5000L, 123456L)
        val denoms = listOf(5L, 25L, 100L, 500L, 10000L)
        for (a in amounts) {
            for (d in denoms) {
                val res = CashRoundCalculator.calculate(a, d, CashRoundingMode.DOWN, usd)
                assertTrue(res.roundedAmountMinorUnits <= res.originalAmountMinorUnits)
                assertTrue(res.originalAmountMinorUnits - res.roundedAmountMinorUnits < d)
            }
        }
    }

    // 39. Round-up invariant: roundedAmount >= originalAmount
    @Test
    fun test39_roundUpInvariant() {
        val amounts = listOf(0L, 1L, 299L, 300L, 4783L, 5000L, 123456L)
        val denoms = listOf(5L, 25L, 100L, 500L, 10000L)
        for (a in amounts) {
            for (d in denoms) {
                val res = CashRoundCalculator.calculate(a, d, CashRoundingMode.UP, usd)
                assertTrue(res.roundedAmountMinorUnits >= res.originalAmountMinorUnits)
                assertTrue(res.roundedAmountMinorUnits - res.originalAmountMinorUnits < d)
            }
        }
    }

    // 40. Nearest invariant: selected result is no farther than alternative multiple, and exact half chooses upper
    @Test
    fun test40_nearestInvariant() {
        val amounts = listOf(0L, 1L, 249L, 250L, 251L, 4749L, 4750L, 4751L, 4783L, 5000L)
        val denoms = listOf(10L, 25L, 100L, 500L)
        for (a in amounts) {
            for (d in denoms) {
                val res = CashRoundCalculator.calculate(a, d, CashRoundingMode.NEAREST, usd)
                val distSelected = abs(res.roundedAmountMinorUnits - a)
                val distLower = abs(res.lowerMultipleMinorUnits - a)
                val distUpper = abs(res.upperMultipleMinorUnits - a)
                assertTrue(distSelected <= distLower)
                assertTrue(distSelected <= distUpper)
                if (distLower == distUpper && !res.wasAlreadyRounded) {
                    assertEquals(res.upperMultipleMinorUnits, res.roundedAmountMinorUnits)
                }
            }
        }
    }

    // 41. History save
    @Test
    fun test41_historySave() {
        val store = InMemoryKeyValueStore()
        val repo = CashRoundHistoryRepository(store)
        val entry = CashRoundHistory(
            id = "1",
            originalAmountMinorUnits = 4783L,
            denominationMinorUnits = 500L,
            roundedAmountMinorUnits = 5000L,
            adjustmentMinorUnits = 217L,
            currencyCode = "USD",
            currencySymbol = "$",
            roundingMode = CashRoundingMode.NEAREST,
            calculatedAt = 1000L
        )
        val saved = repo.saveCalculation(entry)
        assertEquals(1, saved.size)
        assertEquals("1", saved.first().id)
    }

    // 42. History load
    @Test
    fun test42_historyLoad() {
        val store = InMemoryKeyValueStore()
        val repo = CashRoundHistoryRepository(store)
        repo.saveCalculation(
            CashRoundHistory("a", 4783L, 500L, 5000L, 217L, "USD", "$", CashRoundingMode.NEAREST, 1000L)
        )
        repo.saveCalculation(
            CashRoundHistory("b", 124750L, 5000L, 125000L, 250L, "PKR", "Rs. ", CashRoundingMode.UP, 2000L)
        )

        val loaded = repo.loadHistory()
        assertEquals(2, loaded.size)
        assertEquals("b", loaded[0].id)
        assertEquals("a", loaded[1].id)
    }

    // 43. History limit (Max 10 entries; adding 11th removes oldest)
    @Test
    fun test43_historyLimitTen() {
        val store = InMemoryKeyValueStore()
        val repo = CashRoundHistoryRepository(store)
        for (i in 1..11) {
            repo.saveCalculation(
                CashRoundHistory(
                    id = "id_$i",
                    originalAmountMinorUnits = i * 100L,
                    denominationMinorUnits = 500L,
                    roundedAmountMinorUnits = 500L,
                    adjustmentMinorUnits = 0L,
                    currencyCode = "USD",
                    currencySymbol = "$",
                    roundingMode = CashRoundingMode.NEAREST,
                    calculatedAt = i * 1000L
                )
            )
        }
        val history = repo.loadHistory()
        assertEquals(10, history.size)
        assertEquals("id_11", history.first().id)
        assertEquals("id_2", history.last().id)
        assertFalse(history.any { it.id == "id_1" })
    }

    // 44. History deletion
    @Test
    fun test44_historyDeletion() {
        val store = InMemoryKeyValueStore()
        val repo = CashRoundHistoryRepository(store)
        repo.saveCalculation(
            CashRoundHistory("1", 4783L, 500L, 5000L, 217L, "USD", "$", CashRoundingMode.NEAREST, 1000L)
        )
        repo.saveCalculation(
            CashRoundHistory("2", 3000L, 500L, 3000L, 0L, "USD", "$", CashRoundingMode.DOWN, 2000L)
        )
        val remaining = repo.deleteCalculation("1")
        assertEquals(1, remaining.size)
        assertEquals("2", remaining.first().id)
    }

    // 45. History clear
    @Test
    fun test45_historyClear() {
        val store = InMemoryKeyValueStore()
        val repo = CashRoundHistoryRepository(store)
        repo.saveCalculation(
            CashRoundHistory("1", 4783L, 500L, 5000L, 217L, "USD", "$", CashRoundingMode.NEAREST, 1000L)
        )
        val afterClear = repo.clearHistory()
        assertTrue(afterClear.isEmpty())
        assertTrue(repo.loadHistory().isEmpty())
    }

    // 46. History restore
    @Test
    fun test46_historyRestore() {
        val store = InMemoryKeyValueStore()
        val vm = CashRoundViewModel(CashRoundHistoryRepository(store), SettingsRepository(store))

        val item = CashRoundHistory(
            id = "h1",
            originalAmountMinorUnits = 124750L,
            denominationMinorUnits = 5000L,
            roundedAmountMinorUnits = 120000L,
            adjustmentMinorUnits = -4750L,
            currencyCode = "PKR",
            currencySymbol = "Rs. ",
            roundingMode = CashRoundingMode.DOWN,
            calculatedAt = 5000L,
            currencyDecimalPlaces = 2,
            currencyName = "Pakistani Rupee"
        )
        vm.restoreFromHistory(item)

        val state = vm.uiState.value
        assertEquals("1247.50", state.amountInput)
        assertEquals("50", state.denominationInput)
        assertEquals("PKR", state.currency.code)
        assertEquals(CashRoundingMode.DOWN, state.roundingMode)
        assertNotNull(state.result)
        assertEquals(120000L, state.result?.roundedAmountMinorUnits)
    }

    // 47. Settings persistence (Dark mode, Default currency PKR, etc.)
    @Test
    fun test47_settingsPersistence() {
        val store = InMemoryKeyValueStore()
        val settingsRepo = SettingsRepository(store)
        val historyRepo = CashRoundHistoryRepository(store)

        val vm1 = CashRoundViewModel(historyRepo, settingsRepo)
        vm1.updateThemeMode(AppThemeMode.DARK)
        vm1.updateDefaultCurrency(pkr)
        vm1.updatePreserveLastCalculation(false)

        // Reopen (new ViewModel instance using same store)
        val vm2 = CashRoundViewModel(historyRepo, settingsRepo)
        assertEquals(AppThemeMode.DARK, vm2.uiState.value.settings.themeMode)
        assertEquals("PKR", vm2.uiState.value.settings.defaultCurrencyCode)
        assertEquals("PKR", vm2.uiState.value.currency.code)
    }

    // 48. Last-calculation persistence
    @Test
    fun test48_lastCalculationPersistence() {
        val store = InMemoryKeyValueStore()
        val settingsRepo = SettingsRepository(store)
        val historyRepo = CashRoundHistoryRepository(store)

        val vm1 = CashRoundViewModel(historyRepo, settingsRepo)
        vm1.onCurrencySelected(eur)
        vm1.onAmountChange("88.40")
        vm1.onDenominationPresetSelected("10")
        vm1.onRoundingModeSelected(CashRoundingMode.UP)

        val vm2 = CashRoundViewModel(historyRepo, settingsRepo)
        assertEquals("88.40", vm2.uiState.value.amountInput)
        assertEquals("10", vm2.uiState.value.denominationInput)
        assertEquals("EUR", vm2.uiState.value.currency.code)
        assertEquals(CashRoundingMode.UP, vm2.uiState.value.roundingMode)
        assertEquals(9000L, vm2.uiState.value.result?.roundedAmountMinorUnits)
    }

    // 49. Reset (CRITICAL TEST — RESET)
    @Test
    fun test49_resetRestoresDefaultStateAndPreservesHistory() {
        val store = InMemoryKeyValueStore()
        var currentTime = 1000L
        val vm = CashRoundViewModel(
            CashRoundHistoryRepository(store),
            SettingsRepository(store),
            clock = { currentTime }
        )

        vm.onCurrencySelected(jpy)
        vm.onAmountChange("4780")
        vm.onDenominationPresetSelected("500")
        vm.onRoundingModeSelected(CashRoundingMode.DOWN)
        vm.saveCurrentToHistory()
        assertEquals(1, vm.uiState.value.history.size)

        vm.resetToDefault()
        val state = vm.uiState.value
        assertEquals("47.83", state.amountInput)
        assertEquals("5", state.denominationInput)
        assertEquals("USD", state.currency.code)
        assertEquals(CashRoundingMode.NEAREST, state.roundingMode)
        assertEquals(5000L, state.result?.roundedAmountMinorUnits)
        assertEquals(1, state.history.size) // History remains
    }

    // 50. Copy formatting
    @Test
    fun test50_copyFormatting() {
        val result = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, usd)
        val copyText = CashRoundFormatter.formatCopyText(result)
        val expected = """
            CashRound Result

            Original Amount: $47.83
            Denomination: $5.00
            Method: Nearest

            Rounded Amount: $50.00
            Adjustment: +$2.17
        """.trimIndent()
        assertEquals(expected, copyText)
    }

    // 51. Share formatting
    @Test
    fun test51_shareFormatting() {
        val result = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, usd)
        val shareText = CashRoundFormatter.formatShareText(result)
        val expected = """
            CashRound

            Original: $47.83
            Denomination: $5.00
            Method: Nearest

            Rounded: $50.00
            Adjustment: +$2.17
        """.trimIndent()
        assertEquals(expected, shareText)
    }

    // 52. Quick amount presets
    @Test
    fun test52_quickAmountPresets() {
        val store = InMemoryKeyValueStore()
        val vm = CashRoundViewModel(CashRoundHistoryRepository(store), SettingsRepository(store))
        val presets = CurrencyRepository.getQuickAmountPresetInputs(usd)
        assertEquals(listOf("10", "25", "50", "100", "500"), presets)

        vm.onQuickAmountPresetSelected("25")
        assertEquals("25", vm.uiState.value.amountInput)
        assertEquals(2500L, vm.uiState.value.result?.roundedAmountMinorUnits)
    }

    // 53. Denomination presets
    @Test
    fun test53_denominationPresets() {
        val usdPresets = CurrencyRepository.getDenominationPresetInputs(usd)
        assertEquals(
            listOf("0.05", "0.10", "0.25", "0.50", "1", "5", "10", "20", "50", "100"),
            usdPresets
        )
        val jpyPresets = CurrencyRepository.getDenominationPresetInputs(jpy)
        assertTrue(jpyPresets.none { it.contains(".") })
    }

    // 54. Mode switching & deterministic calculation
    @Test
    fun test54_modeSwitchingAndDeterministic() {
        val store = InMemoryKeyValueStore()
        val vm = CashRoundViewModel(CashRoundHistoryRepository(store), SettingsRepository(store))
        // Default: $47.83, $5, Nearest -> $50.00
        assertEquals(5000L, vm.uiState.value.result?.roundedAmountMinorUnits)

        vm.onRoundingModeSelected(CashRoundingMode.DOWN)
        assertEquals(4500L, vm.uiState.value.result?.roundedAmountMinorUnits)

        vm.onRoundingModeSelected(CashRoundingMode.UP)
        assertEquals(5000L, vm.uiState.value.result?.roundedAmountMinorUnits)

        // Deterministic check across 50 runs
        repeat(50) {
            val r = CashRoundCalculator.calculate(4783L, 500L, CashRoundingMode.NEAREST, usd)
            assertEquals(5000L, r.roundedAmountMinorUnits)
            assertEquals(217L, r.adjustmentMinorUnits)
        }
    }

    // 55. Rapid calculation & double-save protection
    @Test
    fun test55_rapidCalculationAndDoubleSaveProtection() {
        val store = InMemoryKeyValueStore()
        var fakeClock = 10_000L
        val vm = CashRoundViewModel(
            CashRoundHistoryRepository(store),
            SettingsRepository(store),
            clock = { fakeClock }
        )

        // Rapidly change inputs without saving; history must remain empty
        vm.onAmountChange("12.34")
        vm.onAmountChange("56.78")
        vm.onAmountChange("47.83")
        assertTrue(vm.uiState.value.history.isEmpty())
        assertEquals(CalculationState.Calculated, vm.uiState.value.calculationState)

        // First explicit save
        assertTrue(vm.saveCurrentToHistory())
        assertEquals(1, vm.uiState.value.history.size)

        // Immediate double tap within 100ms on identical calculation should not duplicate
        fakeClock += 100L
        vm.saveCurrentToHistory()
        assertEquals(1, vm.uiState.value.history.size)

        // Save a different calculation
        fakeClock += 1000L
        vm.onAmountChange("99.99")
        vm.saveCurrentToHistory()
        assertEquals(2, vm.uiState.value.history.size)
    }

    @Test
    fun testCurrencyChangePrecisionReviewNotice() {
        val store = InMemoryKeyValueStore()
        val vm = CashRoundViewModel(CashRoundHistoryRepository(store), SettingsRepository(store))
        // Start with $47.83 in USD, switch to JPY (0 decimal places)
        vm.onCurrencySelected(jpy)
        assertEquals(
            "Currency changed. Review the amount and denomination precision.",
            vm.uiState.value.currencyChangeNotice
        )
        // Because "47.83" has 2 decimals and JPY has 0, validator flags precision error rather than silently converting
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.result)
    }
}