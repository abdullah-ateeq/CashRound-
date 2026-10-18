package com.example.cashround.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import com.example.cashround.domain.CurrencyRepository
import com.example.cashround.model.AppSettings
import com.example.cashround.model.AppThemeMode
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.model.CurrencyOption
import com.example.cashround.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onDefaultCurrencyChange: (CurrencyOption) -> Unit,
    onDefaultDenominationChange: (String) -> Unit,
    onDefaultRoundingModeChange: (CashRoundingMode) -> Unit,
    onConfirmHistoryDeleteChange: (Boolean) -> Unit,
    onPreserveLastCalculationChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetSettings: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onDialogVisibilityChanged: (Boolean) -> Unit = {}
) {
    val view = LocalView.current
    val haptics = settings.hapticFeedback

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    fun updateDialogs(clearHistory: Boolean, resetSettings: Boolean) {
        showClearHistoryDialog = clearHistory
        showResetSettingsDialog = resetSettings
        onDialogVisibilityChanged(clearHistory || resetSettings)
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { updateDialogs(false, false) },
            title = { Text("Clear History") },
            text = { Text("Clear all recent rounding calculations?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateDialogs(false, false)
                        HapticHelper.performHaptic(view, haptics)
                        onClearHistory()
                        onShowSnackbar("Cleared all history")
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogs(false, false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { updateDialogs(false, false) },
            title = { Text("Reset Settings") },
            text = { Text("Restore all settings to their default values?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateDialogs(false, false)
                        HapticHelper.performHaptic(view, haptics)
                        onResetSettings()
                        onShowSnackbar("Settings restored to defaults")
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogs(false, false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize theme, default cash parameters, and preferences.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // APPEARANCE & FEEDBACK
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "APPEARANCE & EXPERIENCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AppThemeMode.entries.size
                            ),
                            selected = settings.themeMode == mode,
                            onClick = {
                                HapticHelper.performHaptic(view, haptics)
                                onThemeModeChange(mode)
                            }
                        ) {
                            Text(mode.displayName)
                        }
                    }
                }

                HorizontalDivider()

                SettingsSwitchRow(
                    title = "Dynamic Color",
                    subtitle = "Use Material You wallpaper colors on Android 12+",
                    checked = settings.dynamicColor,
                    onCheckedChange = {
                        HapticHelper.performHaptic(view, haptics)
                        onDynamicColorChange(it)
                    }
                )

                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile feedback when selecting presets and modes",
                    checked = settings.hapticFeedback,
                    onCheckedChange = {
                        HapticHelper.performHaptic(view, it)
                        onHapticFeedbackChange(it)
                    }
                )

                SettingsSwitchRow(
                    title = "Animations",
                    subtitle = "Subtle transitions when rounded results change",
                    checked = settings.animationsEnabled,
                    onCheckedChange = {
                        HapticHelper.performHaptic(view, haptics)
                        onAnimationsChange(it)
                    }
                )
            }
        }

        // DEFAULT ROUNDING PREFERENCES
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CALCULATION DEFAULTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Default Currency
                val selectedCurrency = CurrencyRepository.findByCode(
                    settings.defaultCurrencyCode,
                    settings.customCurrency
                )
                var currencyExpanded by remember { mutableStateOf(false) }

                Text(
                    text = "Default Currency",
                    style = MaterialTheme.typography.titleMedium
                )

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedCurrency.code} — ${selectedCurrency.name} (${selectedCurrency.symbol.trim()})",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        CurrencyRepository.supportedCurrencies.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text("${option.code} — ${option.name} (${option.symbol.trim()})")
                                },
                                onClick = {
                                    currencyExpanded = false
                                    HapticHelper.performHaptic(view, haptics)
                                    onDefaultCurrencyChange(option)
                                }
                            )
                        }
                    }
                }

                // Default Denomination
                Text(
                    text = "Default Denomination",
                    style = MaterialTheme.typography.titleMedium
                )

                val denomChoices = CurrencyRepository.getDenominationPresetInputs(selectedCurrency)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    denomChoices.forEach { denom ->
                        FilterChip(
                            selected = settings.defaultDenominationInput == denom,
                            onClick = {
                                HapticHelper.performHaptic(view, haptics)
                                onDefaultDenominationChange(denom)
                            },
                            label = { Text("${selectedCurrency.symbol}$denom") }
                        )
                    }
                }

                // Default Rounding Mode
                Text(
                    text = "Default Rounding Mode",
                    style = MaterialTheme.typography.titleMedium
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CashRoundingMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CashRoundingMode.entries.size
                            ),
                            selected = settings.defaultRoundingMode == mode,
                            onClick = {
                                HapticHelper.performHaptic(view, haptics)
                                onDefaultRoundingModeChange(mode)
                            }
                        ) {
                            Text(mode.displayName)
                        }
                    }
                }
            }
        }

        // HISTORY & STATE PERSISTENCE
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "DATA & HISTORY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingsSwitchRow(
                    title = "Preserve Last Calculation",
                    subtitle = "Restore last amount, denomination, currency, and mode on launch",
                    checked = settings.preserveLastCalculation,
                    onCheckedChange = {
                        HapticHelper.performHaptic(view, haptics)
                        onPreserveLastCalculationChange(it)
                    }
                )

                SettingsSwitchRow(
                    title = "Confirm History Delete",
                    subtitle = "Ask before deleting individual recent roundings",
                    checked = settings.confirmHistoryDelete,
                    onCheckedChange = {
                        HapticHelper.performHaptic(view, haptics)
                        onConfirmHistoryDeleteChange(it)
                    }
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticHelper.performHaptic(view, haptics)
                            updateDialogs(clearHistory = true, resetSettings = false)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear History")
                    }

                    OutlinedButton(
                        onClick = {
                            HapticHelper.performHaptic(view, haptics)
                            updateDialogs(clearHistory = false, resetSettings = true)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Settings")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
