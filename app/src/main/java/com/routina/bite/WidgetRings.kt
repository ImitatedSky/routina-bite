package com.routina.bite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * 小工具上的圓環，畫成 Bitmap 再交給 RemoteViews。
 *
 * 為什麼不用 ProgressBar 配 ring shape：XML 的 ring 沒有圓頭，進度的兩端是切平的，
 * 而「圓頭圓環」正是這次要的那個柔和感。RemoteViews 只收得下 Bitmap，所以自己畫。
 *
 * 顏色刻意不分淺色／深色：Bitmap 在畫的那一刻就定了，桌面之後切深色模式不會重畫它。
 * 所以底環用「同一個顏色的淡版」（半透明），在奶油底和深色底上都讀得出來。
 */

/** 環的粗細佔直徑的比例。太細顯得單薄，太粗中間放不下數字 */
private const val STROKE_RATIO = 0.13f

/** 底環的透明度：淡到不搶戲，但看得出「還沒滿的那一段」 */
private const val TRACK_ALPHA = 0.22f

/** 畫的解析度。ImageView 會依實際格子縮放，這個大小在 1×1 放大時也不會糊 */
private const val RING_DP = 72f

/** 一個進度環：從 12 點鐘方向順時針畫 [fraction]（0～1，超過 1 視為滿圈） */
fun ringBitmap(context: Context, fraction: Float, color: Int): Bitmap {
    val canvas = RingCanvas(context)
    canvas.drawTrack(color)
    val sweep = fraction.coerceIn(0f, 1f) * 360f
    if (sweep > 0f) canvas.drawArc(-90f, sweep, color)
    return canvas.bitmap
}

/**
 * 三大營養素的組成環：每一段的長度是那一項換算成熱量後的佔比，段與段之間留一點空隙。
 * 全部是 0（今天還沒吃）就只畫底環。
 */
fun donutBitmap(context: Context, parts: List<Pair<Double, Int>>, trackColor: Int): Bitmap {
    val canvas = RingCanvas(context)
    canvas.drawTrack(trackColor)
    val total = parts.sumOf { it.first }
    if (total <= 0.0) return canvas.bitmap

    val shown = parts.filter { it.first > 0.0 }
    // 只有一項就是整圈，不需要空隙
    val gap = if (shown.size > 1) canvas.capGapDegrees() else 0f
    var start = -90f
    for ((value, color) in shown) {
        val share = (value / total).toFloat() * 360f
        // 太小的一段畫成一個圓點，不要因為減掉空隙變成負的
        canvas.drawArc(start + gap / 2, (share - gap).coerceAtLeast(0.1f), color)
        start += share
    }
    return canvas.bitmap
}

private class RingCanvas(context: Context) {
    private val size = (RING_DP * context.resources.displayMetrics.density).roundToInt()
    private val stroke = size * STROKE_RATIO
    val bitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)

    // 筆畫是沿著中線畫的，所以往內縮半個筆寬，圓頭才不會被裁掉
    private val bounds = RectF(stroke / 2, stroke / 2, size - stroke / 2, size - stroke / 2)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    fun drawTrack(color: Int) {
        paint.color = Color.argb(
            (TRACK_ALPHA * 255).roundToInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        canvas.drawArc(bounds, 0f, 360f, false, paint)
    }

    fun drawArc(start: Float, sweep: Float, color: Int) {
        paint.color = color
        canvas.drawArc(bounds, start, sweep, false, paint)
    }

    /** 圓頭會各往外凸半個筆寬，兩段之間的空隙要大過這個，才看得出是分開的兩段 */
    fun capGapDegrees(): Float {
        val radius = bounds.width() / 2
        val capDegrees = Math.toDegrees((stroke / 2 / radius).toDouble()).toFloat()
        return capDegrees * 2 + 6f
    }
}

/**
 * 圓環中間那個數字的字級：依環的大小與位數算，數字要整個放進內圈。
 * 取代原本 1×1 依位數分三段的作法——現在數字在環裡，能用的寬度是內圈直徑，不是整格。
 */
fun ringCenterTextSp(text: String, ringDp: Int): Float {
    val inner = ringDp * (1 - STROKE_RATIO * 2)
    // 數字的平均字寬約 0.58 個字級；留四分之一給左右邊距，數字才不會貼著環
    val fit = inner * 0.75f / (0.58f * text.length.coerceAtLeast(1))
    return fit.coerceIn(9f, 40f)
}
