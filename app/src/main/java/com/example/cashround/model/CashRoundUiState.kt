package com.example.cashround.model

data class CashRoundUiState(
    val amountInput: String = "47.83",
    val denominationInput: String = "5",
    val currency: CurrencyOption = CurrencyOption("USD", "$", "US Dollar", 2),
    val roundingMode: CashRoundingMode = CashRoundingMode.NEAREST,
    val result: CashRoundResult? = null,
    val history: List<CashRoundHistory> = emptyList(),
    val errorMessage: String? = null,
    val isCalculating: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    val isCustomDenominationSelected: Boolean = false,
    val currencyChangeNotice: String? = null,
    val settings: AppSettings = AppSettings()
)
