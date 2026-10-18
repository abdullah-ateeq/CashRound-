package com.example.cashround.model

sealed interface CalculationState {
    data object Idle : CalculationState
    data object Calculated : CalculationState
    data class Error(val message: String) : CalculationState
}
