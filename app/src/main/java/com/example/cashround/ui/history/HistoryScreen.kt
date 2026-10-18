package com.example.cashround.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashround.domain.CashRoundFormatter
import com.example.cashround.domain.CurrencyRepository
import com.example.cashround.model.CashRoundHistory
import com.example.cashround.model.CurrencyOption
import com.example.cashround.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<CashRoundHistory>,
    confirmDelete: Boolean,
    hapticsEnabled: Boolean,
    onItemSelected: (CashRoundHistory) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDialogVisibilityChanged: (Boolean) -> Unit = {}
) {
    val view = LocalView.current
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    fun updateDialogState(deleteId: String?, clearAll: Boolean) {
        pendingDeleteId = deleteId
        showClearAllDialog = clearAll
        onDialogVisibilityChanged(deleteId != null || clearAll)
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { updateDialogState(null, false) },
            title = { Text("Delete Calculation?") },
            text = { Text("Remove this rounding calculation from your recent roundings?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToRemove = pendingDeleteId
                        updateDialogState(null, false)
                        if (idToRemove != null) {
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            onDeleteItem(idToRemove)
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogState(null, false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { updateDialogState(null, false) },
            title = { Text("Clear History") },
            text = { Text("Clear all recent rounding calculations?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateDialogState(null, false)
                        HapticHelper.performHaptic(view, hapticsEnabled)
                        onClearAll()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogState(null, false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECENT ROUNDINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap any item to restore and recalculate (Last ${history.size}/10)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        HapticHelper.performHaptic(view, hapticsEnabled)
                        updateDialogState(null, true)
                    }
                ) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recent roundings saved yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap SAVE on the Home screen to store up to 10 calculations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = {
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            onItemSelected(item)
                        },
                        onDeleteClick = {
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            if (confirmDelete) {
                                updateDialogState(item.id, false)
                            } else {
                                onDeleteItem(item.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: CashRoundHistory,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currency = CurrencyRepository.supportedCurrencies.find {
        it.code.equals(item.currencyCode, ignoreCase = true)
    } ?: CurrencyOption(
        code = item.currencyCode,
        symbol = item.currencySymbol,
        name = item.currencyName,
        decimalPlaces = item.currencyDecimalPlaces
    )

    val originalFormatted = CashRoundFormatter.formatMoney(item.originalAmountMinorUnits, currency)
    val roundedFormatted = CashRoundFormatter.formatMoney(item.roundedAmountMinorUnits, currency)
    val denomFormatted = CashRoundFormatter.formatDenominationCompact(item.denominationMinorUnits, currency)
    val adjustmentFormatted = CashRoundFormatter.formatAdjustment(item.adjustmentMinorUnits, currency)

    val timeStr = remember(item.calculatedAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.calculatedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$originalFormatted → $roundedFormatted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.roundingMode.displayName} · $denomFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = adjustmentFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.currencyCode} · $timeStr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete calculation",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
