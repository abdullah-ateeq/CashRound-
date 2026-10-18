package com.example.cashround.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashround.domain.CashRoundFormatter
import com.example.cashround.model.CashRoundResult
import com.example.cashround.model.CashRoundingMode

@Composable
fun RoundingVisualCard(
    result: CashRoundResult,
    modifier: Modifier = Modifier
) {
    val currency = result.currency
    val originalFormatted = CashRoundFormatter.formatMoney(result.originalAmountMinorUnits, currency)
    val lowerFormatted = CashRoundFormatter.formatMoney(result.lowerMultipleMinorUnits, currency)
    val upperFormatted = CashRoundFormatter.formatMoney(result.upperMultipleMinorUnits, currency)
    val roundedFormatted = CashRoundFormatter.formatMoney(result.roundedAmountMinorUnits, currency)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ROUNDING VISUAL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Flow step indicator: Original -> Lower (Round Down) or Upper (Round Up)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDownSelected = !result.wasAlreadyRounded &&
                    result.roundedAmountMinorUnits == result.lowerMultipleMinorUnits
                val isUpSelected = !result.wasAlreadyRounded &&
                    result.roundedAmountMinorUnits == result.upperMultipleMinorUnits

                StepOptionPill(
                    amountText = lowerFormatted,
                    label = "Round Down",
                    isSelected = isDownSelected || result.wasAlreadyRounded,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = originalFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                StepOptionPill(
                    amountText = upperFormatted,
                    label = "Round Up",
                    isSelected = isUpSelected || result.wasAlreadyRounded,
                    modifier = Modifier.weight(1f)
                )
            }

            if (result.roundingMode == CashRoundingMode.NEAREST && !result.wasAlreadyRounded) {
                Spacer(modifier = Modifier.height(16.dp))

                NearestScaleVisual(
                    result = result,
                    lowerFormatted = lowerFormatted,
                    originalFormatted = originalFormatted,
                    upperFormatted = upperFormatted,
                    roundedFormatted = roundedFormatted
                )
            }
        }
    }
}

@Composable
private fun StepOptionPill(
    amountText: String,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun NearestScaleVisual(
    result: CashRoundResult,
    lowerFormatted: String,
    originalFormatted: String,
    upperFormatted: String,
    roundedFormatted: String
) {
    val currency = result.currency
    val distanceDownFormatted = CashRoundFormatter.formatMoney(result.distanceDownMinorUnits, currency)
    val distanceUpFormatted = CashRoundFormatter.formatMoney(result.distanceUpMinorUnits, currency)

    // Compute relative position using integer ratio for visual indicator only (not monetary truth)
    val denom = result.denominationMinorUnits.coerceAtLeast(1L)
    val rem = result.distanceDownMinorUnits.coerceIn(0L, denom)
    val fraction = rem.toFloat() / denom.toFloat()

    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = lowerFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Original: $originalFormatted",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = upperFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .padding(horizontal = 6.dp)
        ) {
            val yCenter = size.height / 2f
            val width = size.width
            val markerX = (width * fraction).coerceIn(0f, width)
            val targetX = if (result.roundedAmountMinorUnits == result.upperMultipleMinorUnits) width else 0f

            // Base line
            drawLine(
                color = trackColor,
                start = Offset(0f, yCenter),
                end = Offset(width, yCenter),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Highlighted segment from original to nearest multiple
            drawLine(
                color = activeColor,
                start = Offset(markerX, yCenter),
                end = Offset(targetX, yCenter),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // End ticks
            drawCircle(
                color = trackColor,
                radius = 5.dp.toPx(),
                center = Offset(0f, yCenter)
            )
            drawCircle(
                color = trackColor,
                radius = 5.dp.toPx(),
                center = Offset(width, yCenter)
            )

            // Selected multiple dot
            drawCircle(
                color = activeColor,
                radius = 7.dp.toPx(),
                center = Offset(targetX, yCenter)
            )

            // Original position dot
            drawCircle(
                color = markerColor,
                radius = 7.dp.toPx(),
                center = Offset(markerX, yCenter)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Distance down: $distanceDownFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Distance up: $distanceUpFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val explanation = if (result.distanceDownMinorUnits == result.distanceUpMinorUnits) {
            "Exact halfway — rounds half up to $roundedFormatted"
        } else {
            "$roundedFormatted is closer (Nearest → $roundedFormatted)"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
