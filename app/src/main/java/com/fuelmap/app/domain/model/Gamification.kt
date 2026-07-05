package com.fuelmap.app.domain.model

/** Уровни и бейджи по карме — простая on-device геймификация. */
object Gamification {

    data class Level(val level: Int, val title: String, val current: Int, val nextAt: Int) {
        /** Прогресс к следующему уровню, 0f..1f. */
        val progress: Float
            get() = if (nextAt <= 0) 1f else (current.toFloat() / nextAt).coerceIn(0f, 1f)
    }

    private val titles = listOf(
        "Новичок", "Наблюдатель", "Заправщик", "Штурман",
        "Эксперт", "Ветеран трасс", "Легенда дорог"
    )

    /** Порог кармы для достижения уровня n (1-based): 0,10,30,70,150,310,630… */
    private fun thresholdFor(level: Int): Int =
        if (level <= 1) 0 else (10 * ((1 shl (level - 1)) - 1))

    fun levelFor(karma: Int): Level {
        var lvl = 1
        while (lvl < titles.size && karma >= thresholdFor(lvl + 1)) lvl++
        val title = titles[(lvl - 1).coerceIn(0, titles.lastIndex)]
        val nextAt = if (lvl >= titles.size) thresholdFor(lvl) else thresholdFor(lvl + 1)
        return Level(lvl, title, karma, nextAt)
    }

    data class Badge(val emoji: String, val title: String, val unlocked: Boolean)

    fun badges(karma: Int, marksCount: Int): List<Badge> = listOf(
        Badge("⛽", "Первая отметка", marksCount >= 1),
        Badge("🔟", "10 отметок", marksCount >= 10),
        Badge("💯", "100 кармы", karma >= 100),
        Badge("🏅", "50 отметок", marksCount >= 50),
        Badge("🌟", "500 кармы", karma >= 500)
    )
}
