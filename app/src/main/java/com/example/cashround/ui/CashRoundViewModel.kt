package com.example.cashround.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cashround.domain.CashCalculationOverflowException
import com.example.cashround.domain.CashRoundCalculator
import com.example.cashround.domain.CashRoundFormatter
import com.example.cashround.domain.CashRoundValidator
import com.example.cashround.domain.CurrencyRepository
import com.example.cashround.domain.MoneyFormatter
import com.example.cashround.domain.ValidationResult
import com.example.cashround.model.AppSettings
import com.example.cashround.model.AppThemeMode
import com.example.cashround.model.CalculationState
import com.example.cashround.model.CashRoundHistory
import com.example.cashround.model.CashRoundUiState
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.model.CurrencyOption
import com.example.cashround.repository.CashRoundHistoryRepository
import com.example.cashround.repository.LastCalculationSnapshot
import com.example.cashround.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class CashRoundViewModel(
    private val historyRepository: CashRoundHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    companion object {
        const val DEFAULT_AMOUNT_USD = "47.83"
        const val DEFAULT_DENOMINATION_USD = "5"
    }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<CashRoundUiState> = _uiState.asStateFlow()

    init {
        recalculateInternal()
    }

    private fun createInitialState(): CashRoundUiState {
        val settings = settingsRepository.loadSettings()
        val history = historyRepository.loadHistory()
        val savedLast = if (settings.preserveLastCalculation) {
            settingsRepository.loadLastCalculation()
        } else {
            null
        }

        val initialCurrency = if (savedLast != null) {
            CurrencyRepository.findByCode(savedLast.currencyCode, settings.customCurrency)
        } else {
            CurrencyRepository.findByCode(settings.defaultCurrencyCode, settings.customCurrency)
        }

        val initialAmount = savedLast?.amountInput ?: if (initialCurrency.decimalPlaces == 0) {
            "4780"
        } else {
            DEFAULT_AMOUNT_USD
        }

        val initialDenom = savedLast?.denominationInput ?: if (initialCurrency.decimalPlaces == 0 && settings.defaultDenominationInput == DEFAULT_DENOMINATION_USD) {
            "5"
        } else {
            settings.defaultDenominationInput
        }

        val initialMode = savedLast?.roundingMode ?: settings.defaultRoundingMode
        val presets = CurrencyRepository.getDenominationPresetInputs(initialCurrency)
        val isCustom = savedLast?.isCustomDenomination ?: (initialDenom !in presets)

        return CashRoundUiState(
            amountInput = initialAmount,
            denominationInput = initialDenom,
            currency = initialCurrency,
            roundingMode = initialMode,
            result = null,
            history = history,
            errorMessage = null,
            isCalculating = false,
            calculationState = CalculationState.Idle,
            isCustomDenominationSelected = isCustom,
            currencyChangeNotice = null,
            settings = settings
        )
    }

    fun onAmountChange(newAmount: String) {
        _uiState.update {
            it.copy(
                amountInput = newAmount,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    fun onQuickAmountPresetSelected(presetAmount: String) {
        _uiState.update {
            it.copy(
                amountInput = presetAmount,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    fun onDenominationPresetSelected(presetDenomination: String) {
        _uiState.update {
            it.copy(
                denominationInput = presetDenomination,
                isCustomDenominationSelected = false,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    fun onSelectCustomDenominationMode() {
        _uiState.update {
            it.copy(
                isCustomDenominationSelected = true,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    fun onCustomDenominationChange(customDenomination: String) {
        _uiState.update {
            it.copy(
                denominationInput = customDenomination,
                isCustomDenominationSelected = true,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    fun onRoundingModeSelected(mode: CashRoundingMode) {
        _uiState.update {
            it.copy(roundingMode = mode)
        }
        recalculateInternal()
    }

    fun onCurrencySelected(newCurrency: CurrencyOption) {
        val previousCurrency = _uiState.value.currency
        val precisionChanged = previousCurrency.decimalPlaces != newCurrency.decimalPlaces
        val currentDenom = _uiState.value.denominationInput
        val newPresets = CurrencyRepository.getDenominationPresetInputs(newCurrency)
        val isCustom = _uiState.value.isCustomDenominationSelected || (currentDenom !in newPresets)

        val notice = if (precisionChanged || previousCurrency.code != newCurrency.code) {
            CashRoundValidator.NOTICE_CURRENCY_CHANGED
        } else {
            null
        }

        _uiState.update {
            it.copy(
                currency = newCurrency,
                isCustomDenominationSelected = isCustom,
                currencyChangeNotice = notice
            )
        }
        recalculateInternal()
    }

    fun onCustomCurrencyConfigured(
        code: String,
        symbol: String,
        name: String,
        decimalPlaces: Int
    ) {
        val sanitizedCode = code.trim().uppercase().ifEmpty { "OTR" }.take(6)
        val sanitizedSymbol = symbol.trim().ifEmpty { sanitizedCode }.take(6)
        val sanitizedName = name.trim().ifEmpty { "Custom Currency" }
        val sanitizedDecimals = decimalPlaces.coerceIn(0, 4)

        val customOption = CurrencyOption(
            code = sanitizedCode,
            symbol = sanitizedSymbol,
            name = sanitizedName,
            decimalPlaces = sanitizedDecimals
        )

        val updatedSettings = _uiState.value.settings.copy(customCurrency = customOption)
        settingsRepository.saveSettings(updatedSettings)

        _uiState.update {
            it.copy(settings = updatedSettings)
        }
        onCurrencySelected(customOption)
    }

    fun dismissCurrencyChangeNotice() {
        _uiState.update { it.copy(currencyChangeNotice = null) }
    }

    fun recalculate() {
        recalculateInternal()
    }

    private fun recalculateInternal() {
        val state = _uiState.value
        val validation = CashRoundValidator.validateInputs(
            rawAmount = state.amountInput,
            rawDenomination = state.denominationInput,
            currency = state.currency,
            isCustomDenomination = state.isCustomDenominationSelected
        )

        when (validation) {
            is ValidationResult.Invalid -> {
                _uiState.update {
                    it.copy(
                        result = null,
                        errorMessage = validation.errorMessage,
                        isCalculating = false,
                        calculationState = CalculationState.Error(validation.errorMessage)
                    )
                }
            }
            is ValidationResult.Valid -> {
                try {
                    val result = CashRoundCalculator.calculate(
                        amountMinorUnits = validation.amountMinorUnits,
                        denominationMinorUnits = validation.denominationMinorUnits,
                        mode = state.roundingMode,
                        currency = state.currency
                    )
                    _uiState.update {
                        it.copy(
                            result = result,
                            errorMessage = null,
                            isCalculating = false,
                            calculationState = CalculationState.Calculated
                        )
                    }
                    persistLastCalculationIfEnabled()
                } catch (e: CashCalculationOverflowException) {
                    val msg = e.message ?: CashRoundValidator.ERROR_AMOUNT_TOO_LARGE
                    _uiState.update {
                        it.copy(
                            result = null,
                            errorMessage = msg,
                            isCalculating = false,
                            calculationState = CalculationState.Error(msg)
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    val msg = e.message ?: "Invalid calculation parameters."
                    _uiState.update {
                        it.copy(
                            result = null,
                            errorMessage = msg,
                            isCalculating = false,
                            calculationState = CalculationState.Error(msg)
                        )
                    }
                }
            }
        }
    }

    private fun persistLastCalculationIfEnabled() {
        val state = _uiState.value
        if (state.settings.preserveLastCalculation) {
            settingsRepository.saveLastCalculation(
                LastCalculationSnapshot(
                    amountInput = state.amountInput,
                    denominationInput = state.denominationInput,
                    currencyCode = state.currency.code,
                    roundingMode = state.roundingMode,
                    isCustomDenomination = state.isCustomDenominationSelected
                )
            )
        }
    }

    /**
     * Saves the current valid calculation to history (only when user explicitly taps SAVE).
     * Returns true if saved, false if no valid result was available.
     */
    fun saveCurrentToHistory(): Boolean {
        val currentResult = _uiState.value.result ?: return false
        val now = clock()
        val entry = CashRoundHistory(
            id = UUID.randomUUID().toString(),
            originalAmountMinorUnits = currentResult.originalAmountMinorUnits,
            denominationMinorUnits = currentResult.denominationMinorUnits,
            roundedAmountMinorUnits = currentResult.roundedAmountMinorUnits,
            adjustmentMinorUnits = currentResult.adjustmentMinorUnits,
            currencyCode = currentResult.currency.code,
            currencySymbol = currentResult.currency.symbol,
            roundingMode = currentResult.roundingMode,
            calculatedAt = now,
            currencyDecimalPlaces = currentResult.currency.decimalPlaces,
            currencyName = currentResult.currency.name
        )
        val updatedHistory = historyRepository.saveCalculation(entry)
        _uiState.update { it.copy(history = updatedHistory) }
        return true
    }

    fun deleteHistoryItem(id: String) {
        val updatedHistory = historyRepository.deleteCalculation(id)
        _uiState.update { it.copy(history = updatedHistory) }
    }

    fun clearAllHistory() {
        val cleared = historyRepository.clearHistory()
        _uiState.update { it.copy(history = cleared) }
    }

    fun restoreFromHistory(item: CashRoundHistory) {
        val matchingStandard = CurrencyRepository.supportedCurrencies.find {
            it.code.equals(item.currencyCode, ignoreCase = true)
        }
        val restoredCurrency = matchingStandard ?: CurrencyOption(
            code = item.currencyCode,
            symbol = item.currencySymbol,
            name = item.currencyName,
            decimalPlaces = item.currencyDecimalPlaces
        )

        val amountInput = MoneyFormatter.formatPlainInput(
            minorUnits = item.originalAmountMinorUnits,
            currency = restoredCurrency,
            stripTrailingZeroDecimals = false
        )
        val denomInput = MoneyFormatter.formatPlainInput(
            minorUnits = item.denominationMinorUnits,
            currency = restoredCurrency,
            stripTrailingZeroDecimals = true
        )

        val presets = CurrencyRepository.getDenominationPresetInputs(restoredCurrency)
        val matchedPreset = presets.find { preset ->
            val presetMinor = CashRoundValidator.validateDenominationInput(preset, restoredCurrency).getOrNull()
            presetMinor == item.denominationMinorUnits
        }

        val finalDenomInput = matchedPreset ?: denomInput
        val isCustom = matchedPreset == null

        _uiState.update {
            it.copy(
                amountInput = amountInput,
                denominationInput = finalDenomInput,
                currency = restoredCurrency,
                roundingMode = item.roundingMode,
                isCustomDenominationSelected = isCustom,
                currencyChangeNotice = null
            )
        }
        recalculateInternal()
    }

    /**
     * Resets calculator to the specification's baseline defaults:
     * Amount: $47.83, Denomination: $5, Method: Nearest, Currency: USD.
     * Does not clear history.
     */
    fun resetToDefault() {
        val usd = CurrencyRepository.USD
        _uiState.update {
            it.copy(
                amountInput = DEFAULT_AMOUNT_USD,
                denominationInput = DEFAULT_DENOMINATION_USD,
                currency = usd,
                roundingMode = CashRoundingMode.NEAREST,
                isCustomDenominationSelected = false,
                currencyChangeNotice = null,
                errorMessage = null
            )
        }
        recalculateInternal()
    }

    fun getCopyText(): String? {
        val res = _uiState.value.result ?: return null
        return CashRoundFormatter.formatCopyText(res)
    }

    fun getShareText(): String? {
        val res = _uiState.value.result ?: return null
        return CashRoundFormatter.formatShareText(res)
    }

    // Settings updates
    fun updateThemeMode(themeMode: AppThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        updateSettings { it.copy(dynamicColor = enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        updateSettings { it.copy(hapticFeedback = enabled) }
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        updateSettings { it.copy(animationsEnabled = enabled) }
    }

    fun updateDefaultCurrency(currencyOption: CurrencyOption) {
        updateSettings { it.copy(defaultCurrencyCode = currencyOption.code) }
        onCurrencySelected(currencyOption)
    }

    fun updateDefaultDenomination(denominationInput: String) {
        updateSettings { it.copy(defaultDenominationInput = denominationInput) }
        val presets = CurrencyRepository.getDenominationPresetInputs(_uiState.value.currency)
        if (denominationInput in presets) {
            onDenominationPresetSelected(denominationInput)
        } else {
            onCustomDenominationChange(denominationInput)
        }
    }

    fun updateDefaultRoundingMode(roundingMode: CashRoundingMode) {
        updateSettings { it.copy(defaultRoundingMode = roundingMode) }
        onRoundingModeSelected(roundingMode)
    }

    fun updateConfirmHistoryDelete(enabled: Boolean) {
        updateSettings { it.copy(confirmHistoryDelete = enabled) }
    }

    fun updatePreserveLastCalculation(enabled: Boolean) {
        updateSettings { it.copy(preserveLastCalculation = enabled) }
        if (enabled) {
            persistLastCalculationIfEnabled()
        } else {
            settingsRepository.clearLastCalculation()
        }
    }

    fun resetSettingsToDefaults() {
        val defaults = settingsRepository.resetSettings()
        _uiState.update { it.copy(settings = defaults) }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_uiState.value.settings)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    class Factory(
        private val historyRepository: CashRoundHistoryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CashRoundViewModel::class.java)) {
                return CashRoundViewModel(historyRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
