package com.example.cashround.model

enum class CashRoundingMode(val displayName: String, val shortName: String) {
    NEAREST("Nearest", "NEAREST"),
    UP("Round Up", "UP"),
    DOWN("Round Down", "DOWN");

    companion object {
        fun fromName(name: String?): CashRoundingMode {
            if (name == null) return NEAREST
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: NEAREST
        }
    }
}

typealias RoundingMode = CashRoundingMode
