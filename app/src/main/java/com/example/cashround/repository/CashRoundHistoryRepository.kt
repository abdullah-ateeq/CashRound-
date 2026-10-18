package com.example.cashround.repository

import android.content.SharedPreferences
import com.example.cashround.data.KeyValueStore
import com.example.cashround.data.SharedPreferencesStore
import com.example.cashround.model.CashRoundHistory
import com.example.cashround.model.CashRoundingMode

class CashRoundHistoryRepository(
    private val store: KeyValueStore
) {
    constructor(prefs: SharedPreferences) : this(SharedPreferencesStore(prefs))

    companion object {
        const val KEY_HISTORY = "cash_round_history_entries_v1"
        const val MAX_HISTORY_ITEMS = 10
        private const val ENTRY_SEPARATOR = "\n"
        private const val FIELD_SEPARATOR = "|"
    }

    fun loadHistory(): List<CashRoundHistory> {
        val raw = store.getString(KEY_HISTORY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return raw.split(ENTRY_SEPARATOR)
            .mapNotNull { line -> deserializeEntry(line) }
            .take(MAX_HISTORY_ITEMS)
    }

    /**
     * Saves a calculation to the history list (newest first), enforcing a maximum of 10 entries.
     * Prevents immediate consecutive duplicate saves of identical parameters.
     */
    fun saveCalculation(entry: CashRoundHistory): List<CashRoundHistory> {
        val current = loadHistory().toMutableList()

        // Rapid double-save protection: if the top entry has identical calculation values, replace its timestamp/id or return
        val top = current.firstOrNull()
        if (top != null &&
            top.originalAmountMinorUnits == entry.originalAmountMinorUnits &&
            top.denominationMinorUnits == entry.denominationMinorUnits &&
            top.roundedAmountMinorUnits == entry.roundedAmountMinorUnits &&
            top.adjustmentMinorUnits == entry.adjustmentMinorUnits &&
            top.currencyCode == entry.currencyCode &&
            top.roundingMode == entry.roundingMode &&
            (entry.calculatedAt - top.calculatedAt) in 0..500
        ) {
            return current
        }

        current.removeAll { it.id == entry.id }
        current.add(0, entry)

        val trimmed = if (current.size > MAX_HISTORY_ITEMS) {
            current.take(MAX_HISTORY_ITEMS)
        } else {
            current.toList()
        }

        persistHistory(trimmed)
        return trimmed
    }

    fun deleteCalculation(id: String): List<CashRoundHistory> {
        val updated = loadHistory().filterNot { it.id == id }
        persistHistory(updated)
        return updated
    }

    fun clearHistory(): List<CashRoundHistory> {
        store.remove(KEY_HISTORY)
        return emptyList()
    }

    private fun persistHistory(items: List<CashRoundHistory>) {
        if (items.isEmpty()) {
            store.remove(KEY_HISTORY)
            return
        }
        val serialized = items.joinToString(ENTRY_SEPARATOR) { serializeEntry(it) }
        store.putString(KEY_HISTORY, serialized)
    }

    private fun serializeEntry(item: CashRoundHistory): String {
        val safeSymbol = item.currencySymbol.replace(FIELD_SEPARATOR, " ").replace(ENTRY_SEPARATOR, " ")
        val safeName = item.currencyName.replace(FIELD_SEPARATOR, " ").replace(ENTRY_SEPARATOR, " ")
        return listOf(
            item.id,
            item.originalAmountMinorUnits.toString(),
            item.denominationMinorUnits.toString(),
            item.roundedAmountMinorUnits.toString(),
            item.adjustmentMinorUnits.toString(),
            item.currencyCode,
            safeSymbol,
            item.roundingMode.name,
            item.calculatedAt.toString(),
            item.currencyDecimalPlaces.toString(),
            safeName
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun deserializeEntry(line: String): CashRoundHistory? {
        if (line.isBlank()) return null
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size < 9) return null
        return try {
            val id = parts[0]
            val original = parts[1].toLong()
            val denom = parts[2].toLong()
            val rounded = parts[3].toLong()
            val adjustment = parts[4].toLong()
            val code = parts[5]
            val symbol = parts[6]
            val mode = CashRoundingMode.fromName(parts[7])
            val calculatedAt = parts[8].toLong()
            val decimals = parts.getOrNull(9)?.toIntOrNull() ?: if (code == "JPY") 0 else 2
            val name = parts.getOrNull(10) ?: code

            CashRoundHistory(
                id = id,
                originalAmountMinorUnits = original,
                denominationMinorUnits = denom,
                roundedAmountMinorUnits = rounded,
                adjustmentMinorUnits = adjustment,
                currencyCode = code,
                currencySymbol = symbol,
                roundingMode = mode,
                calculatedAt = calculatedAt,
                currencyDecimalPlaces = decimals,
                currencyName = name
            )
        } catch (_: Exception) {
            null
        }
    }
}
