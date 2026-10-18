package com.example.cashround.model

data class CashRoundResult(
    val originalAmountMinorUnits: Long,
    val denominationMinorUnits: Long,
    val roundedAmountMinorUnits: Long,
    val adjustmentMinorUnits: Long,
    val roundingMode: CashRoundingMode,
    val currency: CurrencyOption,
    val wasAlreadyRounded: Boolean
) {
    val status: CashRoundStatus
        get() = when {
            wasAlreadyRounded || adjustmentMinorUnits == 0L -> CashRoundStatus.ALREADY_ROUNDED
            adjustmentMinorUnits > 0L -> CashRoundStatus.ROUNDED_UP
            else -> CashRoundStatus.ROUNDED_DOWN
        }

    val lowerMultipleMinorUnits: Long
        get() = if (denominationMinorUnits > 0L) {
            (originalAmountMinorUnits / denominationMinorUnits) * denominationMinorUnits
        } else {
            originalAmountMinorUnits
        }

    val upperMultipleMinorUnits: Long
        get() {
            if (denominationMinorUnits <= 0L) return originalAmountMinorUnits
            val remainder = originalAmountMinorUnits % denominationMinorUnits
            val lower = lowerMultipleMinorUnits
            return if (remainder == 0L) {
                lower
            } else {
                try {
                    Math.addExact(lower, denominationMinorUnits)
                } catch (_: ArithmeticException) {
                    lower
                }
            }
        }

    val distanceDownMinorUnits: Long
        get() = originalAmountMinorUnits - lowerMultipleMinorUnits

    val distanceUpMinorUnits: Long
        get() = upperMultipleMinorUnits - originalAmountMinorUnits
}
