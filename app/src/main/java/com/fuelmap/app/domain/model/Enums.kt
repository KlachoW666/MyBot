package com.fuelmap.app.domain.model

/** Роль пользователя. */
enum class Role {
    USER,
    ADMIN,
    SUPER_ADMIN;

    val isAdmin: Boolean get() = this == ADMIN || this == SUPER_ADMIN
}

/** Тип топлива. */
enum class FuelType(val title: String) {
    AI92("АИ-92"),
    AI95("АИ-95"),
    AI98("АИ-98"),
    AI100("АИ-100"),
    GAS("Газ"),
    DIESEL("Дизель");

    companion object {
        fun fromName(name: String): FuelType? = entries.firstOrNull { it.name == name }
    }
}

/** Индикатор очереди на АЗС. */
enum class Queue(val title: String) {
    NONE("Нет очереди"),
    SMALL("Небольшая"),
    LARGE("Большая")
}

/** Тип подтверждения достоверности отметки. */
enum class ConfirmationType {
    CONFIRM, // топливо действительно есть
    EMPTY    // уже закончилось
}

/** Статус модерации АЗС, добавленной пользователем. */
enum class StationStatus {
    APPROVED, // видна всем
    PENDING   // ждёт подтверждения модератора
}

/**
 * Свежесть / статус отметки для цветовой индикации маркера на карте.
 * 🟢 свежая (<2ч), 🟡 устаревает (2–6ч), ⚪ нет данных / >6ч, 🔴 топлива нет.
 */
enum class MarkFreshness {
    FRESH,      // зелёный
    AGING,      // жёлтый
    STALE,      // серый
    NO_FUEL;    // красный

    companion object {
        const val FRESH_THRESHOLD_MS = 2 * 60 * 60 * 1000L   // 2 часа
        const val AGING_THRESHOLD_MS = 6 * 60 * 60 * 1000L   // 6 часов

        /**
         * @param createdAt время отметки (мс), null — данных нет
         * @param hasAvailableFuel есть ли хотя бы один доступный тип топлива
         */
        fun of(createdAt: Long?, hasAvailableFuel: Boolean, now: Long = System.currentTimeMillis()): MarkFreshness {
            if (createdAt == null) return STALE
            val age = now - createdAt
            if (!hasAvailableFuel) return NO_FUEL
            return when {
                age <= FRESH_THRESHOLD_MS -> FRESH
                age <= AGING_THRESHOLD_MS -> AGING
                else -> STALE
            }
        }
    }
}
