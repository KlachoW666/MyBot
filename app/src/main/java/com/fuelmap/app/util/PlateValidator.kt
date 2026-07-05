package com.fuelmap.app.util

/**
 * Валидация российского госномера формата «А123ВС 750» (буквы кириллица из допустимого
 * набора, 3 цифры, 2 буквы, регион 2–3 цифры). Пробел между номером и регионом необязателен.
 */
object PlateValidator {
    // Буквы, визуально совпадающие с латиницей и используемые в номерах
    private const val LETTERS = "АВЕКМНОРСТУХ"
    private val regex = Regex("^[$LETTERS]\\d{3}[$LETTERS]{2}\\s?\\d{2,3}$")

    fun isValid(raw: String): Boolean {
        val plate = raw.trim().uppercase()
        return regex.matches(plate)
    }

    fun normalize(raw: String): String = raw.trim().uppercase()
}
