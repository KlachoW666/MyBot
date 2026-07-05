package com.fuelmap.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.fuelmap.app.domain.model.MarkFreshness
import kotlin.math.ceil

/**
 * Программно рисует круглые маркеры АЗС нужного цвета (без необходимости в PNG-ресурсах).
 * Цвет определяется свежестью отметки.
 */
object MarkerIcons {

    fun colorFor(freshness: MarkFreshness): Int = when (freshness) {
        MarkFreshness.FRESH -> Color.rgb(0x2E, 0xCC, 0x40)   // зелёный
        MarkFreshness.AGING -> Color.rgb(0xFF, 0xC1, 0x07)   // жёлтый
        MarkFreshness.STALE -> Color.rgb(0x9E, 0x9E, 0x9E)   // серый
        MarkFreshness.NO_FUEL -> Color.rgb(0xE5, 0x39, 0x35) // красный
    }

    /** Иконка кластера: синий круг с числом АЗС. */
    fun clusterBitmap(count: Int, sizePx: Int = 96): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = sizePx / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x21, 0x74, 0xE8)
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.08f
        }
        canvas.drawCircle(r, r, r - sizePx * 0.08f, fill)
        canvas.drawCircle(r, r, r - sizePx * 0.08f, border)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.42f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val label = if (count > 99) "99+" else count.toString()
        canvas.drawText(label, r, r - (text.ascent() + text.descent()) / 2f, text)
        return bmp
    }

    /** Синяя точка «вы здесь». */
    fun userBitmap(sizePx: Int = 64): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = sizePx / 2f
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0x21, 0x96, 0xF3)
            style = Paint.Style.FILL
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x21, 0x96, 0xF3)
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.08f
        }
        canvas.drawCircle(r, r, r, halo)
        canvas.drawCircle(r, r, sizePx * 0.22f, dot)
        canvas.drawCircle(r, r, sizePx * 0.22f, border)
        return bmp
    }

    /**
     * Плашка-карточка над АЗС (видна при приближении): строки с топливом и ценой,
     * цветная полоса по свежести и «хвостик» снизу. Якорь — низ по центру.
     */
    fun labelBitmap(lines: List<String>, accent: Int): Bitmap {
        val textSize = 34f
        val padH = 22f
        val padV = 16f
        val lineGap = 10f
        val stripeW = 12f
        val pointer = 18f
        val radius = 22f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x20, 0x24, 0x28)
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lineH = textSize + lineGap
        val textBlockH = lines.size * lineH - lineGap
        val maxTextW = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f

        val cardW = 8f + stripeW + padH + maxTextW + padH
        val cardH = padV + textBlockH + padV
        val w = ceil(cardW + 4f).toInt()
        val h = ceil(cardH + pointer + 4f).toInt()

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val cx = cardW / 2f
        // хвостик
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val pointerPath = Path().apply {
            moveTo(cx - 12f, cardH - 1f)
            lineTo(cx + 12f, cardH - 1f)
            lineTo(cx, cardH + pointer)
            close()
        }
        canvas.drawPath(pointerPath, pointerPaint)

        val rect = RectF(2f, 2f, cardW - 2f, cardH - 2f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(
            RectF(rect.left + 8f, rect.top + 10f, rect.left + 8f + stripeW, rect.bottom - 10f),
            6f, 6f, stripePaint
        )

        val textX = rect.left + 8f + stripeW + padH
        var y = rect.top + padV + textSize - 6f
        for (line in lines) {
            canvas.drawText(line, textX, y, textPaint)
            y += lineH
        }
        return bmp
    }

    /** Маркер АЗС в стиле Яндекса: скруглённый квадрат («сквиркл») с белой обводкой и каплей. */
    fun bitmap(freshness: MarkFreshness, sizePx: Int = 84): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pad = sizePx * 0.10f
        val corner = sizePx * 0.30f
        val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFor(freshness)
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.09f
        }
        canvas.drawRoundRect(rect, corner, corner, fill)
        canvas.drawRoundRect(rect, corner, corner, border)

        // белая «капля» топлива в центре
        val cx = sizePx / 2f
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cx, sizePx * 0.15f, inner)
        return bmp
    }
}
