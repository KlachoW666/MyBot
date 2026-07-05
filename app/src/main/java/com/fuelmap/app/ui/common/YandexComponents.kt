package com.fuelmap.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuelmap.app.ui.theme.BrandGradient

/** Крупная капсульная кнопка с фирменным зелёным градиентом (как «Подтверждаю» у Яндекса). */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(28.dp)
    val bg = if (enabled) Brush.horizontalGradient(BrandGradient)
    else Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHighest)
    )
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon, contentDescription = null,
                    tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Пилюля-опция как в «Ситуация на АЗС»: скруглённая капсула + опциональный квадрат-чекбокс. */
@Composable
fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLowest
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(container)
            .border(border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showCheckbox) Arrangement.Start else Arrangement.Center
    ) {
        if (showCheckbox) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        } else {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Большая круглая кнопка действия с подписью снизу (как «Заправиться»/«Поехали» у Яндекса). */
@Composable
fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(Brush.linearGradient(BrandGradient))
                    else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = label,
                tint = if (filled) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}
