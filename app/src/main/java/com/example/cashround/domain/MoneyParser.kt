package com.example.cashround.domain

import com.example.cashround.model.CurrencyOption

sealed class MoneyParseException(override val message: String) : IllegalArgumentException(message) {
    class EmptyInput(message: String = "Enter a valid amount.") : MoneyParseException(message)
    class NegativeValue(message: String = "Cash amount cannot be negative.") : MoneyParseException(message)
    class InvalidFormat(message: String = "Invalid number format.") : MoneyParseException(message)
    class PrecisionExceeded(decimalPlaces: Int) : MoneyParseException(
        if (decimalPlaces == 0) {
            "This currency does not support decimal places."
        } else {
            "Maximum $decimalPlaces decimal places allowed for this currency."
        }
    )
    class Overflow(message: String = "Amount is too large.") : MoneyParseException(message)
}

object MoneyParser {

    /**
     * Safe maximum minor units to prevent overflow during rounding calculations.
     * Long.MAX_VALUE is ~9.22e18; keeping inputs within Long.MAX_VALUE / 4 guarantees
     * safe head-room for (quotient + 1) * denomination and remainder comparisons,
     * while still supporting up to Long.MAX_VALUE with exact overflow checks.
     */
    const val MAX_SAFE_MINOR_UNITS: Long = Long.MAX_VALUE / 2

    fun powerOfTen(exponent: Int): Long {
        require(exponent in 0..18) { "Invalid decimal places: $exponent" }
        var result = 1L
        repeat(exponent) {
            result = Math.multiplyExact(result, 10L)
        }
        return result
    }

    /**
     * Parses a user-entered string amount into minor units (Long) according to currency precision.
     * Never uses floating-point arithmetic.
     */
    fun parseToMinorUnits(
        rawInput: String,
        currency: CurrencyOption,
        isDenomination: Boolean = false
    ): Result<Long> {
        val trimmed = rawInput.trim().replace(",", "").replace("_", "")
        if (trimmed.isEmpty()) {
            val msg = if (isDenomination) {
                "Enter a denomination greater than zero."
            } else {
                "Enter a cash amount."
            }
            return Result.failure(MoneyParseException.EmptyInput(msg))
        }

        if (trimmed.startsWith("-")) {
            val msg = if (isDenomination) {
                "Denomination must be greater than zero."
            } else {
                "Cash amount cannot be negative."
            }
            return Result.failure(MoneyParseException.NegativeValue(msg))
        }

        val normalized = if (trimmed.startsWith("+")) trimmed.substring(1) else trimmed
        if (normalized.isEmpty() || normalized == ".") {
            return Result.failure(MoneyParseException.InvalidFormat("Enter a valid numeric value."))
        }

        val parts = normalized.split('.')
        if (parts.size > 2) {
            return Result.failure(MoneyParseException.InvalidFormat("Enter a valid numeric value."))
        }

        val wholePartStr = parts[0].ifEmpty { "0" }
        val fracPartStr = if (parts.size == 2) parts[1] else ""

        if (!wholePartStr.all { it.isDigit() } || !fracPartStr.all { it.isDigit() }) {
            return Result.failure(MoneyParseException.InvalidFormat("Enter a valid numeric value."))
        }

        val allowedDecimals = currency.decimalPlaces.coerceIn(0, 4)

        // Check if fractional digits beyond allowed precision contain any non-zero digit,
        // or if the user entered more fractional digits than allowed for the currency.
        if (fracPartStr.length > allowedDecimals) {
            // Allow trailing zeros such as "5.00" when converting or if someone typed ".00" on 0-decimal?
            // No, the spec states: "JPY with zero decimal places should not allow 0.50 as a valid denomination"
            // and "Currency changed. Review the amount and denomination precision."
            // What if fracPartStr is all zeros (e.g. "50.00" in 2-decimal currency has length 2 <= 2)?
            // For 0-decimal currency, any decimal part at all (even .50) exceeds precision 0.
            return Result.failure(MoneyParseException.PrecisionExceeded(allowedDecimals))
        }

        return try {
            val scaleFactor = powerOfTen(allowedDecimals)
            val wholeUnits = wholePartStr.toLongOrNull()
                ?: return Result.failure(MoneyParseException.Overflow("Amount is too large."))

            val scaledWhole = Math.multiplyExact(wholeUnits, scaleFactor)

            val paddedFrac = fracPartStr.padEnd(allowedDecimals, '0')
            val fracUnits = if (paddedFrac.isEmpty()) 0L else {
                paddedFrac.toLongOrNull()
                    ?: return Result.failure(MoneyParseException.Overflow("Amount is too large."))
            }

            val totalMinorUnits = Math.addExact(scaledWhole, fracUnits)
            Result.success(totalMinorUnits)
        } catch (_: ArithmeticException) {
            Result.failure(MoneyParseException.Overflow("Amount is too large."))
        } catch (_: NumberFormatException) {
            Result.failure(MoneyParseException.Overflow("Amount is too large."))
        }
    }
}
