package com.example.cashround.domain

import com.example.cashround.model.CashRoundResult
import com.example.cashround.model.CurrencyOption
import kotlin.math.abs

object CashRoundFormatter {

    fun formatMoney(
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        return MoneyFormatter.formatMinorUnits(minorUnits, currency)
    }

    fun formatAdjustment(
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        return MoneyFormatter.formatAdjustment(minorUnits, currency)
    }

    fun formatDenomination(
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        return MoneyFormatter.formatDenomination(minorUnits, currency, compactWhole = false)
    }

    fun formatDenominationCompact(
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        return MoneyFormatter.formatDenomination(minorUnits, currency, compactWhole = true)
    }

    fun formatCopyText(
        result: CashRoundResult
    ): String {
        val original = formatMoney(result.originalAmountMinorUnits, result.currency)
        val denomination = formatDenomination(result.denominationMinorUnits, result.currency)
        val method = result.roundingMode.displayName
        val rounded = formatMoney(result.roundedAmountMinorUnits, result.currency)
        val adjustment = formatAdjustment(result.adjustmentMinorUnits, result.currency)

        return buildString {
            appendLine("CashRound Result")
            appendLine()
            appendLine("Original Amount: $original")
            appendLine("Denomination: $denomination")
            appendLine("Method: $method")
            appendLine()
            appendLine("Rounded Amount: $rounded")
            append("Adjustment: $adjustment")
        }
    }

    fun formatShareText(
        result: CashRoundResult
    ): String {
        val original = formatMoney(result.originalAmountMinorUnits, result.currency)
        val denomination = formatDenomination(result.denominationMinorUnits, result.currency)
        val method = result.roundingMode.displayName
        val rounded = formatMoney(result.roundedAmountMinorUnits, result.currency)
        val adjustment = formatAdjustment(result.adjustmentMinorUnits, result.currency)

        return buildString {
            appendLine("CashRound")
            appendLine()
            appendLine("Original: $original")
            appendLine("Denomination: $denomination")
            appendLine("Method: $method")
            appendLine()
            appendLine("Rounded: $rounded")
            append("Adjustment: $adjustment")
        }
    }

    /**
     * Accessibility spoken description for a monetary value (e.g., "47 dollars and 83 cents").
     */
    fun formatAccessibleAmount(
        label: String,
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        val spoken = formatSpokenMoney(abs(minorUnits), currency)
        return "$label, $spoken."
    }

    fun formatAccessibleAdjustment(
        adjustmentMinorUnits: Long,
        currency: CurrencyOption
    ): String {
        return when {
            adjustmentMinorUnits > 0L -> {
                val spoken = formatSpokenMoney(adjustmentMinorUnits, currency)
                "Adjustment, $spoken added."
            }
            adjustmentMinorUnits < 0L -> {
                val spoken = formatSpokenMoney(abs(adjustmentMinorUnits), currency)
                "Adjustment, $spoken removed."
            }
            else -> {
                "Adjustment, zero, already rounded."
            }
        }
    }

    private fun formatSpokenMoney(
        absMinorUnits: Long,
        currency: CurrencyOption
    ): String {
        val decimalPlaces = currency.decimalPlaces.coerceIn(0, 4)
        val scale = MoneyParser.powerOfTen(decimalPlaces)
        val whole = absMinorUnits / scale
        val frac = absMinorUnits % scale

        if (currency.code.equals("USD", ignoreCase = true) && decimalPlaces == 2) {
            val dollarWord = if (whole == 1L) "dollar" else "dollars"
            return if (frac == 0L) {
                "$whole $dollarWord"
            } else {
                val centWord = if (frac == 1L) "cent" else "cents"
                "$whole $dollarWord and $frac $centWord"
            }
        }

        return MoneyFormatter.formatMinorUnits(absMinorUnits, currency)
    }
}
