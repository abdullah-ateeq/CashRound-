package com.example.cashround.model

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): AppThemeMode {
            if (name == null) return SYSTEM
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticFeedback: Boolean = true,
    val animationsEnabled: Boolean = true,
    val defaultCurrencyCode: String = "USD",
    val defaultDenominationInput: String = "5",
    val defaultRoundingMode: CashRoundingMode = CashRoundingMode.NEAREST,
    val confirmHistoryDelete: Boolean = true,
    val preserveLastCalculation: Boolean = true,
    val customCurrency: CurrencyOption = CurrencyOption(
        code = "OTR",
        symbol = "¤",
        name = "Custom Currency",
        decimalPlaces = 2
    )
)
