package com.example.cashround.domain

import com.example.cashround.model.CurrencyOption
import kotlin.math.abs

object MoneyFormatter {

    /**
     * Formats minor units using pure integer arithmetic into a localized string with
     * currency symbol, thousands separators, and exact decimal places.
     */
    fun formatMinorUnits(
        minorUnits: Long,
        currency: CurrencyOption,
        trimTrailingZerosIfWhole: Boolean = false
    ): String {
        val isNegative = minorUnits < 0L
        val decimalPlaces = currency.decimalPlaces.coerceIn(0, 4)
        val scale = MoneyParser.powerOfTen(decimalPlaces)

        // Handle Long.MIN_VALUE safely using unsigned/quotient-remainder arithmetic
        val absWhole = abs(minorUnits / scale)
        val absFrac = abs(minorUnits % scale)

        val formattedWhole = formatWithThousandsSeparator(absWhole)
        val numberPart = if (decimalPlaces == 0) {
            formattedWhole
        } else {
            val fracString = absFrac.toString().padStart(decimalPlaces, '0')
            if (trimTrailingZerosIfWhole && absFrac == 0L) {
                formattedWhole
            } else {
                "$formattedWhole.$fracString"
            }
        }

        val signPrefix = if (isNegative) "-" else ""
        return "$signPrefix${currency.symbol}$numberPart"
    }

    /**
     * Formats minor units as a plain input string without currency symbol or thousands separators,
     * suitable for restoring into text fields.
     */
    fun formatPlainInput(
        minorUnits: Long,
        currency: CurrencyOption,
        stripTrailingZeroDecimals: Boolean = false
    ): String {
        val isNegative = minorUnits < 0L
        val decimalPlaces = currency.decimalPlaces.coerceIn(0, 4)
        val scale = MoneyParser.powerOfTen(decimalPlaces)

        val absWhole = abs(minorUnits / scale)
        val absFrac = abs(minorUnits % scale)

        val numberPart = if (decimalPlaces == 0) {
            absWhole.toString()
        } else {
            val fracString = absFrac.toString().padStart(decimalPlaces, '0')
            if (stripTrailingZeroDecimals && absFrac == 0L) {
                absWhole.toString()
            } else if (stripTrailingZeroDecimals) {
                val trimmedFrac = fracString.trimEnd('0')
                "$absWhole.$trimmedFrac"
            } else {
                "$absWhole.$fracString"
            }
        }

        val signPrefix = if (isNegative) "-" else ""
        return "$signPrefix$numberPart"
    }

    fun formatAdjustment(
        adjustmentMinorUnits: Long,
        currency: CurrencyOption
    ): String {
        return when {
            adjustmentMinorUnits > 0L -> "+${formatMinorUnits(adjustmentMinorUnits, currency)}"
            adjustmentMinorUnits < 0L -> "-${formatMinorUnits(abs(adjustmentMinorUnits), currency)}"
            else -> formatMinorUnits(0L, currency)
        }
    }

    fun formatDenomination(
        denominationMinorUnits: Long,
        currency: CurrencyOption,
        compactWhole: Boolean = false
    ): String {
        return formatMinorUnits(
            minorUnits = denominationMinorUnits,
            currency = currency,
            trimTrailingZerosIfWhole = compactWhole
        )
    }

    private fun formatWithThousandsSeparator(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val sb = StringBuilder()
        var count = 0
        for (i in raw.length - 1 downTo 0) {
            sb.append(raw[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString()
    }
}
