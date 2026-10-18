package com.example.cashround.repository

import android.content.SharedPreferences
import com.example.cashround.data.KeyValueStore
import com.example.cashround.data.SharedPreferencesStore
import com.example.cashround.model.AppSettings
import com.example.cashround.model.AppThemeMode
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.model.CurrencyOption

data class LastCalculationSnapshot(
    val amountInput: String,
    val denominationInput: String,
    val currencyCode: String,
    val roundingMode: CashRoundingMode,
    val isCustomDenomination: Boolean = false
)

class SettingsRepository(
    private val store: KeyValueStore
) {
    constructor(prefs: SharedPreferences) : this(SharedPreferencesStore(prefs))

    companion object {
        const val KEY_THEME_MODE = "settings_theme_mode"
        const val KEY_DYNAMIC_COLOR = "settings_dynamic_color"
        const val KEY_HAPTIC_FEEDBACK = "settings_haptic_feedback"
        const val KEY_ANIMATIONS_ENABLED = "settings_animations_enabled"
        const val KEY_DEFAULT_CURRENCY = "settings_default_currency"
        const val KEY_DEFAULT_DENOMINATION = "settings_default_denomination"
        const val KEY_DEFAULT_ROUNDING_MODE = "settings_default_rounding_mode"
        const val KEY_CONFIRM_HISTORY_DELETE = "settings_confirm_history_delete"
        const val KEY_PRESERVE_LAST_CALC = "settings_preserve_last_calculation"

        const val KEY_CUSTOM_CURRENCY_CODE = "settings_custom_currency_code"
        const val KEY_CUSTOM_CURRENCY_SYMBOL = "settings_custom_currency_symbol"
        const val KEY_CUSTOM_CURRENCY_NAME = "settings_custom_currency_name"
        const val KEY_CUSTOM_CURRENCY_DECIMALS = "settings_custom_currency_decimals"

        const val KEY_LAST_AMOUNT = "last_calc_amount"
        const val KEY_LAST_DENOMINATION = "last_calc_denomination"
        const val KEY_LAST_CURRENCY = "last_calc_currency"
        const val KEY_LAST_ROUNDING_MODE = "last_calc_rounding_mode"
        const val KEY_LAST_IS_CUSTOM_DENOM = "last_calc_is_custom_denom"
    }

    fun loadSettings(): AppSettings {
        val defaults = AppSettings()
        val customCurrency = CurrencyOption(
            code = store.getString(KEY_CUSTOM_CURRENCY_CODE, defaults.customCurrency.code) ?: defaults.customCurrency.code,
            symbol = store.getString(KEY_CUSTOM_CURRENCY_SYMBOL, defaults.customCurrency.symbol) ?: defaults.customCurrency.symbol,
            name = store.getString(KEY_CUSTOM_CURRENCY_NAME, defaults.customCurrency.name) ?: defaults.customCurrency.name,
            decimalPlaces = store.getInt(KEY_CUSTOM_CURRENCY_DECIMALS, defaults.customCurrency.decimalPlaces).coerceIn(0, 4)
        )
        return AppSettings(
            themeMode = AppThemeMode.fromName(store.getString(KEY_THEME_MODE, defaults.themeMode.name)),
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            hapticFeedback = store.getBoolean(KEY_HAPTIC_FEEDBACK, defaults.hapticFeedback),
            animationsEnabled = store.getBoolean(KEY_ANIMATIONS_ENABLED, defaults.animationsEnabled),
            defaultCurrencyCode = store.getString(KEY_DEFAULT_CURRENCY, defaults.defaultCurrencyCode) ?: defaults.defaultCurrencyCode,
            defaultDenominationInput = store.getString(KEY_DEFAULT_DENOMINATION, defaults.defaultDenominationInput) ?: defaults.defaultDenominationInput,
            defaultRoundingMode = CashRoundingMode.fromName(store.getString(KEY_DEFAULT_ROUNDING_MODE, defaults.defaultRoundingMode.name)),
            confirmHistoryDelete = store.getBoolean(KEY_CONFIRM_HISTORY_DELETE, defaults.confirmHistoryDelete),
            preserveLastCalculation = store.getBoolean(KEY_PRESERVE_LAST_CALC, defaults.preserveLastCalculation),
            customCurrency = customCurrency
        )
    }

    fun saveSettings(settings: AppSettings) {
        store.putString(KEY_THEME_MODE, settings.themeMode.name)
        store.putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
        store.putBoolean(KEY_HAPTIC_FEEDBACK, settings.hapticFeedback)
        store.putBoolean(KEY_ANIMATIONS_ENABLED, settings.animationsEnabled)
        store.putString(KEY_DEFAULT_CURRENCY, settings.defaultCurrencyCode)
        store.putString(KEY_DEFAULT_DENOMINATION, settings.defaultDenominationInput)
        store.putString(KEY_DEFAULT_ROUNDING_MODE, settings.defaultRoundingMode.name)
        store.putBoolean(KEY_CONFIRM_HISTORY_DELETE, settings.confirmHistoryDelete)
        store.putBoolean(KEY_PRESERVE_LAST_CALC, settings.preserveLastCalculation)

        store.putString(KEY_CUSTOM_CURRENCY_CODE, settings.customCurrency.code)
        store.putString(KEY_CUSTOM_CURRENCY_SYMBOL, settings.customCurrency.symbol)
        store.putString(KEY_CUSTOM_CURRENCY_NAME, settings.customCurrency.name)
        store.putInt(KEY_CUSTOM_CURRENCY_DECIMALS, settings.customCurrency.decimalPlaces.coerceIn(0, 4))
    }

    fun resetSettings(): AppSettings {
        val defaults = AppSettings()
        saveSettings(defaults)
        return defaults
    }

    fun saveLastCalculation(snapshot: LastCalculationSnapshot) {
        store.putString(KEY_LAST_AMOUNT, snapshot.amountInput)
        store.putString(KEY_LAST_DENOMINATION, snapshot.denominationInput)
        store.putString(KEY_LAST_CURRENCY, snapshot.currencyCode)
        store.putString(KEY_LAST_ROUNDING_MODE, snapshot.roundingMode.name)
        store.putBoolean(KEY_LAST_IS_CUSTOM_DENOM, snapshot.isCustomDenomination)
    }

    fun loadLastCalculation(): LastCalculationSnapshot? {
        val amount = store.getString(KEY_LAST_AMOUNT, null) ?: return null
        val denom = store.getString(KEY_LAST_DENOMINATION, null) ?: return null
        val currencyCode = store.getString(KEY_LAST_CURRENCY, null) ?: return null
        val modeStr = store.getString(KEY_LAST_ROUNDING_MODE, null) ?: return null
        val isCustom = store.getBoolean(KEY_LAST_IS_CUSTOM_DENOM, false)

        return LastCalculationSnapshot(
            amountInput = amount,
            denominationInput = denom,
            currencyCode = currencyCode,
            roundingMode = CashRoundingMode.fromName(modeStr),
            isCustomDenomination = isCustom
        )
    }

    fun clearLastCalculation() {
        store.remove(KEY_LAST_AMOUNT)
        store.remove(KEY_LAST_DENOMINATION)
        store.remove(KEY_LAST_CURRENCY)
        store.remove(KEY_LAST_ROUNDING_MODE)
        store.remove(KEY_LAST_IS_CUSTOM_DENOM)
    }
}
