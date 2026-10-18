package com.example.cashround.model

data class CashRoundHistory(
    val id: String,
    val originalAmountMinorUnits: Long,
    val denominationMinorUnits: Long,
    val roundedAmountMinorUnits: Long,
    val adjustmentMinorUnits: Long,
    val currencyCode: String,
    val currencySymbol: String,
    val roundingMode: CashRoundingMode,
    val calculatedAt: Long,
    val currencyDecimalPlaces: Int = 2,
    val currencyName: String = currencyCode
)
