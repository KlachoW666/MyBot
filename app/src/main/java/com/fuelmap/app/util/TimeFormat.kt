package com.fuelmap.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))

    fun dateTime(ts: Long): String = fmt.format(Date(ts))

    fun ago(ts: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - ts
        val min = diff / 60_000
        return when {
            min < 1 -> "только что"
            min < 60 -> "$min мин. назад"
            min < 60 * 24 -> "${min / 60} ч. назад"
            else -> "${min / (60 * 24)} дн. назад"
        }
    }
}
