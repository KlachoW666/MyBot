package com.fuelmap.app.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration

/** Телеграм техподдержки и канала проекта. */
const val SUPPORT_TELEGRAM = "oilfor"
const val SUPPORT_TELEGRAM_URL = "https://t.me/$SUPPORT_TELEGRAM"

@Composable
fun SupportFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        text = "Техподдержка и Telegram-канал: @$SUPPORT_TELEGRAM",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_TELEGRAM_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    )
}
