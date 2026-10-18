package com.example.cashround.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cashround.domain.CashRoundFormatter
import com.example.cashround.domain.CurrencyRepository
import com.example.cashround.model.CashRoundResult
import com.example.cashround.model.CashRoundStatus
import com.example.cashround.model.CashRoundUiState
import com.example.cashround.model.CashRoundingMode
import com.example.cashround.util.HapticHelper
import com.example.cashround.util.ShareAndClipboardHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: CashRoundUiState,
    onAmountChange: (String) -> Unit,
    onQuickAmountPreset: (String) -> Unit,
    onDenominationPreset: (String) -> Unit,
    onSelectCustomDenomination: () -> Unit,
    onCustomDenominationChange: (String) -> Unit,
    onRoundingModeChange: (CashRoundingMode) -> Unit,
    onCurrencySelected: (com.example.cashround.model.CurrencyOption) -> Unit,
    onCustomCurrencyConfigured: (String, String, String, Int) -> Unit,
    onDismissCurrencyNotice: () -> Unit,
    onRecalculate: () -> Unit,
    onSaveResult: () -> Boolean,
    onReset: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onCustomDialogVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticsEnabled = uiState.settings.hapticFeedback
    val animationsEnabled = uiState.settings.animationsEnabled

    var showCustomCurrencyDialog by remember { mutableStateOf(false) }

    fun setDialogOpen(open: Boolean) {
        showCustomCurrencyDialog = open
        onCustomDialogVisibilityChanged(open)
    }

    if (showCustomCurrencyDialog) {
        CustomCurrencyDialog(
            initialCurrency = uiState.settings.customCurrency,
            onDismiss = { setDialogOpen(false) },
            onConfirm = { code, symbol, name, decimals ->
                setDialogOpen(false)
                HapticHelper.performHaptic(view, hapticsEnabled)
                onCustomCurrencyConfigured(code, symbol, name, decimals)
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
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CashRound",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Round cash amounts to practical denominations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Currency change notice if active
        AnimatedVisibility(visible = uiState.currencyChangeNotice != null) {
            uiState.currencyChangeNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissCurrencyNotice) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notice",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 1. CASH AMOUNT & CURRENCY CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CASH AMOUNT",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    CurrencyDropdownSelector(
                        selectedCurrency = uiState.currency,
                        onCurrencySelected = { option ->
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            onCurrencySelected(option)
                        },
                        onSelectOtherCustom = {
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            setDialogOpen(true)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val amountAccessibility = uiState.result?.let {
                    CashRoundFormatter.formatAccessibleAmount(
                        label = "Cash amount",
                        minorUnits = it.originalAmountMinorUnits,
                        currency = uiState.currency
                    )
                } ?: "Cash amount, ${uiState.amountInput}"

                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = onAmountChange,
                    prefix = {
                        Text(
                            text = uiState.currency.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    placeholder = { Text(CurrencyRepository.getDefaultAmountInput(uiState.currency)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (uiState.currency.decimalPlaces == 0) {
                            KeyboardType.Number
                        } else {
                            KeyboardType.Decimal
                        },
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = amountAccessibility
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Amounts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val quickAmounts = CurrencyRepository.getQuickAmountPresetInputs(uiState.currency)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickAmounts.forEach { preset ->
                        AssistChip(
                            onClick = {
                                HapticHelper.performHaptic(view, hapticsEnabled)
                                onQuickAmountPreset(preset)
                            },
                            label = {
                                Text(
                                    text = "${uiState.currency.symbol}$preset",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val testAmounts = CurrencyRepository.getQuickTestAmountInputs(uiState.currency)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Examples:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    testAmounts.forEach { testVal ->
                        AssistChip(
                            onClick = {
                                HapticHelper.performHaptic(view, hapticsEnabled)
                                onQuickAmountPreset(testVal)
                            },
                            label = {
                                Text(
                                    text = "${uiState.currency.symbol}$testVal",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }
        }

        // 2. DENOMINATION CARD ("ROUND TO")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val denomAccessibility = uiState.result?.let {
                CashRoundFormatter.formatAccessibleAmount(
                    label = "Rounding denomination",
                    minorUnits = it.denominationMinorUnits,
                    currency = uiState.currency
                )
            } ?: "Rounding denomination, ${uiState.denominationInput}"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics {
                        contentDescription = denomAccessibility
                    }
            ) {
                Text(
                    text = "ROUND TO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presets = CurrencyRepository.getDenominationPresetInputs(uiState.currency)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = !uiState.isCustomDenominationSelected &&
                            uiState.denominationInput == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                HapticHelper.performHaptic(view, hapticsEnabled)
                                onDenominationPreset(preset)
                            },
                            label = {
                                Text(
                                    text = "${uiState.currency.symbol}$preset",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }

                    FilterChip(
                        selected = uiState.isCustomDenominationSelected,
                        onClick = {
                            HapticHelper.performHaptic(view, hapticsEnabled)
                            onSelectCustomDenomination()
                        },
                        label = {
                            Text(
                                text = "CUSTOM",
                                fontWeight = if (uiState.isCustomDenominationSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                AnimatedVisibility(visible = uiState.isCustomDenominationSelected) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.denominationInput,
                            onValueChange = onCustomDenominationChange,
                            label = { Text("Custom denomination") },
                            prefix = {
                                Text(
                                    text = uiState.currency.symbol,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (uiState.currency.decimalPlaces == 0) {
                                    KeyboardType.Number
                                } else {
                                    KeyboardType.Decimal
                                },
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. ROUNDING METHOD SELECTOR
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ROUNDING METHOD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CashRoundingMode.entries.forEachIndexed { index, mode ->
                        val modeA11y = when (mode) {
                            CashRoundingMode.NEAREST -> "Round to nearest."
                            CashRoundingMode.UP -> "Round up."
                            CashRoundingMode.DOWN -> "Round down."
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CashRoundingMode.entries.size
                            ),
                            selected = uiState.roundingMode == mode,
                            onClick = {
                                HapticHelper.performHaptic(view, hapticsEnabled)
                                onRoundingModeChange(mode)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = modeA11y
                            }
                        ) {
                            Text(
                                text = mode.shortName,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. ERROR OR RESULT CARD
        if (uiState.errorMessage != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (uiState.result != null) {
            ResultProminentCard(
                result = uiState.result,
                animationsEnabled = animationsEnabled,
                onSave = {
                    HapticHelper.performHaptic(view, hapticsEnabled)
                    val saved = onSaveResult()
                    if (saved) {
                        onShowSnackbar("Saved to recent roundings")
                    }
                },
                onCopy = {
                    HapticHelper.performHaptic(view, hapticsEnabled)
                    val copyText = CashRoundFormatter.formatCopyText(uiState.result)
                    ShareAndClipboardHelper.copyToClipboard(context, copyText)
                    onShowSnackbar("Copied result to clipboard")
                },
                onShare = {
                    HapticHelper.performHaptic(view, hapticsEnabled)
                    val shareText = CashRoundFormatter.formatShareText(uiState.result)
                    ShareAndClipboardHelper.shareText(context, shareText)
                },
                onReset = {
                    HapticHelper.performHaptic(view, hapticsEnabled)
                    onReset()
                    onShowSnackbar("Reset to default values")
                }
            )

            RoundingVisualCard(result = uiState.result)
        }

        // Explicit ROUND CASH button as requested ("Also provide a visible ROUND CASH button if useful")
        Button(
            onClick = {
                HapticHelper.performHaptic(view, hapticsEnabled)
                onRecalculate()
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "ROUND CASH",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdownSelector(
    selectedCurrency: com.example.cashround.model.CurrencyOption,
    onCurrencySelected: (com.example.cashround.model.CurrencyOption) -> Unit,
    onSelectOtherCustom: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "${selectedCurrency.code} (${selectedCurrency.symbol.trim()})",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .width(155.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CurrencyRepository.supportedCurrencies.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text("${option.code} — ${option.name} (${option.symbol.trim()})")
                    },
                    onClick = {
                        expanded = false
                        onCurrencySelected(option)
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text("Other — Custom Currency…")
                },
                onClick = {
                    expanded = false
                    onSelectOtherCustom()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultProminentCard(
    result: CashRoundResult,
    animationsEnabled: Boolean,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit
) {
    val currency = result.currency
    val roundedFormatted = CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, currency)
    val originalFormatted = CashRoundFormatter.formatMoney(result.originalAmountMinorUnits, currency)
    val denominationFormatted = CashRoundFormatter.formatDenomination(result.denominationMinorUnits, currency)
    val adjustmentFormatted = CashRoundFormatter.formatAdjustment(result.adjustmentMinorUnits, currency)

    val roundedA11y = CashRoundFormatter.formatAccessibleAmount(
        label = "Rounded amount",
        minorUnits = result.roundedAmountMinorUnits,
        currency = currency
    )
    val adjustmentA11y = CashRoundFormatter.formatAccessibleAdjustment(
        adjustmentMinorUnits = result.adjustmentMinorUnits,
        currency = currency
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ROUNDED CASH AMOUNT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 1. Rounded Amount (most prominent, animated subtly 250ms when enabled)
            if (animationsEnabled) {
                AnimatedContent(
                    targetState = roundedFormatted,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith
                            fadeOut(animationSpec = tween(200))
                    },
                    label = "RoundedAmountAnimation"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.semantics {
                            contentDescription = roundedA11y
                        }
                    )
                }
            } else {
                Text(
                    text = roundedFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.semantics {
                        contentDescription = roundedA11y
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Adjustment & Status Badge
            val statusLabel = when (result.status) {
                CashRoundStatus.ALREADY_ROUNDED -> "ALREADY ROUNDED · $adjustmentFormatted"
                CashRoundStatus.ROUNDED_UP -> "Adjustment: $adjustmentFormatted (Rounded Up)"
                CashRoundStatus.ROUNDED_DOWN -> "Adjustment: $adjustmentFormatted (Rounded Down)"
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.semantics {
                    contentDescription = adjustmentA11y
                }
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // 3. Original Amount, 4. Denomination, 5. Rounding Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricColumn(
                    label = "Original",
                    value = originalFormatted,
                    alignment = Alignment.Start
                )
                SummaryMetricColumn(
                    label = "Denomination",
                    value = denominationFormatted,
                    alignment = Alignment.CenterHorizontally
                )
                SummaryMetricColumn(
                    label = "Method",
                    value = result.roundingMode.displayName,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: SAVE, COPY, SHARE, RESET
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE")
                }

                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY")
                }

                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARE")
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET")
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    value: String,
    alignment: Alignment.Horizontal
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
