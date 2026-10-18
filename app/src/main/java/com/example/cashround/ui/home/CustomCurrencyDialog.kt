package com.example.cashround.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cashround.model.CurrencyOption

@Composable
fun CustomCurrencyDialog(
    initialCurrency: CurrencyOption,
    onDismiss: () -> Unit,
    onConfirm: (code: String, symbol: String, name: String, decimalPlaces: Int) -> Unit
) {
    var code by remember { mutableStateOf(initialCurrency.code.takeIf { it != "OTHER" } ?: "OTR") }
    var symbol by remember { mutableStateOf(initialCurrency.symbol) }
    var name by remember { mutableStateOf(initialCurrency.name) }
    var decimalPlaces by remember { mutableIntStateOf(initialCurrency.decimalPlaces.coerceIn(0, 4)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom Currency",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(6).uppercase() },
                    label = { Text("Currency Code (e.g., CHF)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.take(6) },
                    label = { Text("Symbol (e.g., Fr., ¤)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    label = { Text("Currency Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Decimal Places (0–4)",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..4).forEach { places ->
                        FilterChip(
                            selected = decimalPlaces == places,
                            onClick = { decimalPlaces = places },
                            label = { Text(places.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        code.ifBlank { "OTR" },
                        symbol.ifBlank { "¤" },
                        name.ifBlank { "Custom Currency" },
                        decimalPlaces
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
