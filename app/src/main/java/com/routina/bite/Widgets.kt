package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import com.routina.bite.data.entriesOn
import com.routina.bite.data.todayDate
import com.routina.bite.data.totalOf
import com.routina.bite.model.Targets
import kotlin.math.roundToInt

/**
 * 三個桌面小工具共用的東西：尺寸分桶、今天的數字、開 App 的 PendingIntent。
 *
 * 每個小工具自己負責版面與互動，這裡只放三份都一樣的部分。
 */

/** 依桌面給的尺寸決定用哪一個版面 */
enum class WidgetSize {
    /** 約 1×1：只放得下一個數字 */
    TINY,

    /** 矮而寬（2×1、4×1）：一行字，放不下按鈕 */
    ROW,

    /** 2 格以上高：完整版面 */
    FULL
}

/** 桌面一格約 70dp、兩格 110dp；不到 110dp 就放不下完整版面 */
private const val COMPACT_DP = 110

/** 一行之內還擠得下第二組數字的寬度 */
const val WIDGET_WIDE_DP = 250

/**
 * 從桌面回報的 options 讀出尺寸分桶。
 *
 * 沒有 options 或值是 0（舊桌面、剛綁上還沒回報）時當完整版面：
 * 那是小工具新放上去的預設尺寸。
 */
fun widgetSizeOf(options: Bundle?): WidgetSize {
    val width = widgetWidthDp(options)
    val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    if (width <= 0 || height <= 0) return WidgetSize.FULL
    if (height >= COMPACT_DP) return WidgetSize.FULL
    return if (width >= COMPACT_DP) WidgetSize.ROW else WidgetSize.TINY
}

fun widgetWidthDp(options: Bundle?): Int =
    options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0

/** 小工具要顯示的今天數字。小工具看的一律是今天，不是 App 裡正在看的那一天 */
class WidgetToday(
    val kcal: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val water: Int,
    val targets: Targets
)

fun widgetToday(context: Context): WidgetToday {
    val repository = (context.applicationContext as? BiteApp)?.repository
    val date = todayDate()
    val total = repository?.let { totalOf(entriesOn(it.entries.value, date)) }
    return WidgetToday(
        kcal = total?.kcal?.roundToInt() ?: 0,
        protein = total?.protein ?: 0.0,
        fat = total?.fat ?: 0.0,
        carbs = total?.carbs ?: 0.0,
        water = repository?.waterOn(date) ?: 0,
        targets = repository?.targets?.value ?: Targets()
    )
}

/** 圓環要畫幾成滿：超過目標就是滿圈，吃超過由文字「超過了」講 */
fun widgetFraction(value: Int, target: Int): Float =
    if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)

/**
 * 1×1 的圓環實際有多大（dp）。環以 fitCenter 撐滿格子，所以是格子的短邊扣掉內距；
 * 中間數字的字級要靠這個算。讀不到尺寸時用一般桌面一格的大小。
 *
 * 扣的是最壞情況：我們自己的 6dp，加上 Android 11 以下桌面每邊再塞的 8dp。
 * 算小了只是字小一號；算大了數字會壓到環上。
 */
fun tinyRingDp(options: Bundle?): Int {
    val width = widgetWidthDp(options)
    val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    if (width <= 0 || height <= 0) return 48
    return (minOf(width, height) - 28).coerceIn(28, 120)
}

/** 環太小時，數字底下那行小標籤擠不進內圈，乾脆不放 */
fun ringHasRoomForLabel(ringDp: Int): Boolean = ringDp >= 44

/** 「500 / 2000 ml」這種：前面的數字大、後面的目標與單位縮小，一眼先看到重點 */
fun bigThenSmall(big: String, small: String): CharSequence {
    val text = SpannableString(big + small)
    text.setSpan(RelativeSizeSpan(0.55f), big.length, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return text
}

/**
 * 開 App 的 PendingIntent。
 *
 * PendingIntent 只比對 requestCode 與 Intent 的「過濾」部分（不含 extras），
 * 所以每個入口都要給自己的 requestCode，否則後面的會蓋掉前面的。
 */
fun openAppIntent(context: Context, requestCode: Int, extra: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(extra, true)
    return PendingIntent.getActivity(context, requestCode, intent, WIDGET_PENDING_FLAGS)
}

const val WIDGET_PENDING_FLAGS =
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
