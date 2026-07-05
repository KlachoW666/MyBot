package com.fuelmap.app.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuelmap.app.domain.model.Premium

/** Список привилегий Premium (для paywall). */
private val PREMIUM_PERKS = listOf(
    "Фото с АЗС от других водителей",
    "Гео-уведомления о нужном топливе",
    "Уведомления в Telegram",
    "Маршрут «Поехали» до заправки"
)

/**
 * Paywall Premium-подписки. Показывает привилегии и цену со скидкой
 * (599 ₽/мес зачёркнуто → 199 ₽/мес). Кнопка оформления — демо-активация.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywall(
    onSubscribe: () -> Unit,
    onDismiss: () -> Unit,
    highlight: String? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Russia Oil Premium",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (highlight != null) {
                Text(
                    highlight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PREMIUM_PERKS.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(perk, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "${Premium.PRICE_OLD} ₽/мес",
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${Premium.PRICE_NOW} ₽",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "/мес",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    Text(
                        "цена со скидкой",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                Text("Оформить за ${Premium.PRICE_NOW} ₽/мес")
            }
            Text(
                "Демо-активация на ${Premium.TRIAL_DAYS} дней без реальной оплаты.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_TELEGRAM_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Наш Telegram-канал @$SUPPORT_TELEGRAM")
            }
        }
    }
}
