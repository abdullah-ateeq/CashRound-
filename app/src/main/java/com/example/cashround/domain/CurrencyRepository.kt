package com.example.cashround.domain

import com.example.cashround.model.CurrencyOption

object CurrencyRepository {

    val USD = CurrencyOption("USD", "$", "US Dollar", 2)
    val EUR = CurrencyOption("EUR", "€", "Euro", 2)
    val GBP = CurrencyOption("GBP", "£", "British Pound", 2)
    val PKR = CurrencyOption("PKR", "Rs. ", "Pakistani Rupee", 2)
    val AED = CurrencyOption("AED", "AED ", "UAE Dirham", 2)
    val SAR = CurrencyOption("SAR", "SAR ", "Saudi Riyal", 2)
    val CAD = CurrencyOption("CAD", "CA$", "Canadian Dollar", 2)
    val AUD = CurrencyOption("AUD", "A$", "Australian Dollar", 2)
    val INR = CurrencyOption("INR", "₹", "Indian Rupee", 2)
    val JPY = CurrencyOption("JPY", "¥", "Japanese Yen", 0)
    val CNY = CurrencyOption("CNY", "¥", "Chinese Yuan", 2)

    val DEFAULT_CUSTOM = CurrencyOption("OTHER", "¤", "Other (Custom)", 2)

    val supportedCurrencies: List<CurrencyOption> = listOf(
        USD,
        EUR,
        GBP,
        PKR,
        AED,
        SAR,
        CAD,
        AUD,
        INR,
        JPY,
        CNY
    )

    fun findByCode(code: String, customFallback: CurrencyOption = DEFAULT_CUSTOM): CurrencyOption {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: if (code.equals("OTHER", ignoreCase = true) || code.equals(customFallback.code, ignoreCase = true)) {
                customFallback
            } else {
                USD
            }
    }

    /**
     * Returns practical denomination preset inputs (in major units string representation)
     * adapted to the selected currency's decimal places.
     */
    fun getDenominationPresetInputs(currency: CurrencyOption): List<String> {
        return when (currency.decimalPlaces) {
            0 -> listOf("1", "5", "10", "50", "100", "500", "1000", "5000", "10000")
            1 -> listOf("0.1", "0.5", "1", "5", "10", "20", "50", "100")
            else -> listOf("0.05", "0.10", "0.25", "0.50", "1", "5", "10", "20", "50", "100")
        }
    }

    /**
     * Returns denomination presets in minor units for the given currency.
     */
    fun getDenominationPresetsMinorUnits(currency: CurrencyOption): List<Long> {
        return getDenominationPresetInputs(currency).mapNotNull { input ->
            MoneyParser.parseToMinorUnits(input, currency).getOrNull()
        }
    }

    /**
     * Quick amount presets in major unit strings for the selected currency.
     */
    fun getQuickAmountPresetInputs(currency: CurrencyOption): List<String> {
        return if (currency.decimalPlaces == 0) {
            listOf("100", "250", "500", "1000", "5000")
        } else {
            listOf("10", "25", "50", "100", "500")
        }
    }

    /**
     * Secondary quick test amounts for demonstrating rounding behavior.
     */
    fun getQuickTestAmountInputs(currency: CurrencyOption): List<String> {
        return when (currency.decimalPlaces) {
            0 -> listOf("478", "999", "4780")
            1 -> listOf("47.8", "99.9", "125.5")
            else -> listOf("47.83", "99.99", "125.50")
        }
    }

    fun getDefaultAmountInput(currency: CurrencyOption): String {
        return when (currency.decimalPlaces) {
            0 -> "4780"
            1 -> "47.8"
            else -> "47.83"
        }
    }

    fun getDefaultDenominationInput(currency: CurrencyOption): String {
        return when (currency.decimalPlaces) {
            0 -> "500"
            else -> "5"
        }
    }
}
