package com.fuelmap.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.fuelmap.app.domain.model.MarkFreshness

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

    fun bitmap(freshness: MarkFreshness, sizePx: Int = 72): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = sizePx / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFor(freshness)
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.10f
        }
        canvas.drawCircle(r, r, r - sizePx * 0.10f, fill)
        canvas.drawCircle(r, r, r - sizePx * 0.10f, border)
        // маленькая «капля» топлива в центре
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(r, r, sizePx * 0.16f, inner)
        return bmp
    }
}
