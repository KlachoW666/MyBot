package com.fuelmap.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Построение маршрута до АЗС во внешнем приложении (Яндекс.Навигатор/Карты/любой geo). */
object NavigationLauncher {
    fun route(context: Context, lat: Double, lng: Double, label: String? = null) {
        val name = Uri.encode(label ?: "АЗС")
        val candidates = listOf(
            "yandexnavi://build_route_on_map?lat_to=$lat&lon_to=$lng",
            "yandexmaps://maps.yandex.ru/?rtext=~$lat,$lng&rtt=auto",
            "geo:$lat,$lng?q=$lat,$lng($name)",
            "https://yandex.ru/maps/?rtext=~$lat,$lng&rtt=auto"
        )
        for (uri in candidates) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (_: ActivityNotFoundException) {
                // пробуем следующий вариант
            }
        }
    }
}
