package com.example.cashround.domain

import com.example.cashround.model.CurrencyOption

sealed class ValidationResult {
    data class Valid(
        val amountMinorUnits: Long,
        val denominationMinorUnits: Long
    ) : ValidationResult()

    data class Invalid(val errorMessage: String) : ValidationResult()
}

object CashRoundValidator {

    const val ERROR_NEGATIVE_AMOUNT = "Cash amount cannot be negative."
    const val ERROR_INVALID_DENOMINATION = "Denomination must be greater than zero."
    const val ERROR_CUSTOM_DENOMINATION = "Enter a denomination greater than zero."
    const val ERROR_AMOUNT_TOO_LARGE = "Amount is too large."
    const val NOTICE_CURRENCY_CHANGED = "Currency changed. Review the amount and denomination precision."

    fun validateCurrency(currency: CurrencyOption): Result<CurrencyOption> {
        if (currency.code.isBlank()) {
            return Result.failure(IllegalArgumentException("Currency code cannot be empty."))
        }
        if (currency.symbol.isBlank()) {
            return Result.failure(IllegalArgumentException("Currency symbol cannot be empty."))
        }
        if (currency.decimalPlaces !in 0..4) {
            return Result.failure(IllegalArgumentException("Decimal places must be between 0 and 4."))
        }
        return Result.success(currency)
    }

    fun validateAmountInput(rawAmount: String, currency: CurrencyOption): Result<Long> {
        val parseResult = MoneyParser.parseToMinorUnits(rawAmount, currency, isDenomination = false)
        return parseResult.fold(
            onSuccess = { minorUnits ->
                if (minorUnits < 0L) {
                    Result.failure(IllegalArgumentException(ERROR_NEGATIVE_AMOUNT))
                } else {
                    Result.success(minorUnits)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    fun validateDenominationInput(
        rawDenomination: String,
        currency: CurrencyOption,
        isCustom: Boolean = false
    ): Result<Long> {
        val parseResult = MoneyParser.parseToMinorUnits(rawDenomination, currency, isDenomination = true)
        return parseResult.fold(
            onSuccess = { minorUnits ->
                if (minorUnits <= 0L) {
                    val msg = if (isCustom) ERROR_CUSTOM_DENOMINATION else ERROR_INVALID_DENOMINATION
                    Result.failure(IllegalArgumentException(msg))
                } else {
                    Result.success(minorUnits)
                }
            },
            onFailure = { error ->
                if (error is MoneyParseException.NegativeValue) {
                    Result.failure(IllegalArgumentException(ERROR_INVALID_DENOMINATION))
                } else if (error is MoneyParseException.EmptyInput && isCustom) {
                    Result.failure(IllegalArgumentException(ERROR_CUSTOM_DENOMINATION))
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    fun validateMinorUnits(
        amountMinorUnits: Long,
        denominationMinorUnits: Long
    ): Result<Unit> {
        if (amountMinorUnits < 0L) {
            return Result.failure(IllegalArgumentException(ERROR_NEGATIVE_AMOUNT))
        }
        if (denominationMinorUnits <= 0L) {
            return Result.failure(IllegalArgumentException(ERROR_INVALID_DENOMINATION))
        }
        return Result.success(Unit)
    }

    fun validateInputs(
        rawAmount: String,
        rawDenomination: String,
        currency: CurrencyOption,
        isCustomDenomination: Boolean = false
    ): ValidationResult {
        val currencyCheck = validateCurrency(currency)
        if (currencyCheck.isFailure) {
            return ValidationResult.Invalid(
                currencyCheck.exceptionOrNull()?.message ?: "Invalid currency."
            )
        }

        val amountResult = validateAmountInput(rawAmount, currency)
        if (amountResult.isFailure) {
            return ValidationResult.Invalid(
                amountResult.exceptionOrNull()?.message ?: "Enter a valid cash amount."
            )
        }

        val denomResult = validateDenominationInput(rawDenomination, currency, isCustomDenomination)
        if (denomResult.isFailure) {
            return ValidationResult.Invalid(
                denomResult.exceptionOrNull()?.message ?: ERROR_INVALID_DENOMINATION
            )
        }

        return ValidationResult.Valid(
            amountMinorUnits = amountResult.getOrThrow(),
            denominationMinorUnits = denomResult.getOrThrow()
        )
    }
}
