package com.fuelmap.app.domain.model

import com.fuelmap.app.data.local.UserEntity

/**
 * Параметры Premium-подписки Russia Oil.
 *
 * Premium открывает: просмотр фото с чужих отметок, гео-/Telegram-уведомления и построение
 * маршрута «Поехали». Оплата на устройстве не проводится (нет бэкенда/биллинга) — активация
 * демонстрационная на [TRIAL_DAYS] дней; администратор может выдать Premium вручную.
 */
object Premium {
    /** Цена со скидкой, ₽/мес. */
    const val PRICE_NOW = 199
    /** Цена без скидки (зачёркнутая), ₽/мес. */
    const val PRICE_OLD = 599
    /** На сколько дней активируется подписка. */
    const val TRIAL_DAYS = 30

    const val DAY_MS = 24L * 60 * 60 * 1000
}

/** Активна ли Premium-подписка. Админ/супер-админ имеют Premium неявно. */
fun UserEntity.hasPremium(now: Long = System.currentTimeMillis()): Boolean =
    role.isAdmin || (premiumUntil?.let { it > now } == true)
