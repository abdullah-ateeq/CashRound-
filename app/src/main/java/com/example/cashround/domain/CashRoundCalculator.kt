package com.example.cashround.domain

import com.example.cashround.model.CashRoundResult
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.model.CurrencyOption

class CashCalculationOverflowException(
    override val message: String = CashRoundValidator.ERROR_AMOUNT_TOO_LARGE
) : ArithmeticException(message)

object CashRoundCalculator {

    /**
     * Authoritative cash denomination rounding calculation using only Long integer arithmetic.
     *
     * For amount A >= 0 and denomination D > 0:
     * A = q * D + r, where q = A / D and r = A % D.
     *
     * - DOWN: q * D
     * - UP: if r == 0 then q * D else (q + 1) * D
     * - NEAREST: if r < D - r then q * D else (q + 1) * D (rounds half up when 2 * r == D)
     *
     * Guarantees:
     * 1. roundedAmountMinorUnits % denominationMinorUnits == 0
     * 2. roundedAmountMinorUnits == originalAmountMinorUnits + adjustmentMinorUnits
     * 3. Overflow throws CashCalculationOverflowException ("Amount is too large.")
     */
    fun calculate(
        amountMinorUnits: Long,
        denominationMinorUnits: Long,
        mode: CashRoundingMode,
        currency: CurrencyOption
    ): CashRoundResult {
        if (amountMinorUnits < 0L) {
            throw IllegalArgumentException(CashRoundValidator.ERROR_NEGATIVE_AMOUNT)
        }
        if (denominationMinorUnits <= 0L) {
            throw IllegalArgumentException(CashRoundValidator.ERROR_INVALID_DENOMINATION)
        }

        val quotient = amountMinorUnits / denominationMinorUnits
        val remainder = amountMinorUnits % denominationMinorUnits
        val wasAlreadyRounded = remainder == 0L

        val lowerMultiple = try {
            Math.multiplyExact(quotient, denominationMinorUnits)
        } catch (_: ArithmeticException) {
            throw CashCalculationOverflowException()
        }

        val roundedMinorUnits: Long = when {
            wasAlreadyRounded -> lowerMultiple
            mode == CashRoundingMode.DOWN -> lowerMultiple
            mode == CashRoundingMode.UP -> {
                computeUpperMultiple(quotient, denominationMinorUnits)
            }
            mode == CashRoundingMode.NEAREST -> {
                // Compare distance to lower (remainder) vs distance to upper (denomination - remainder)
                // without risking overflow from remainder * 2:
                // remainder * 2 < denomination  <==>  remainder < denomination - remainder
                val distanceToUpper = denominationMinorUnits - remainder
                if (remainder < distanceToUpper) {
                    lowerMultiple
                } else {
                    // Exact halfway (remainder == distanceToUpper) or closer to upper rounds UP
                    computeUpperMultiple(quotient, denominationMinorUnits)
                }
            }
            else -> lowerMultiple
        }

        val adjustmentMinorUnits = try {
            Math.subtractExact(roundedMinorUnits, amountMinorUnits)
        } catch (_: ArithmeticException) {
            throw CashCalculationOverflowException()
        }

        return CashRoundResult(
            originalAmountMinorUnits = amountMinorUnits,
            denominationMinorUnits = denominationMinorUnits,
            roundedAmountMinorUnits = roundedMinorUnits,
            adjustmentMinorUnits = adjustmentMinorUnits,
            roundingMode = mode,
            currency = currency,
            wasAlreadyRounded = wasAlreadyRounded
        )
    }

    private fun computeUpperMultiple(quotient: Long, denominationMinorUnits: Long): Long {
        return try {
            val nextQuotient = Math.addExact(quotient, 1L)
            Math.multiplyExact(nextQuotient, denominationMinorUnits)
        } catch (_: ArithmeticException) {
            throw CashCalculationOverflowException()
        }
    }
}
