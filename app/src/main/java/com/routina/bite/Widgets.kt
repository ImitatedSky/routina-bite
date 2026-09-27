package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

/**
 * 1×1 上那個數字要用多大的字。
 *
 * 固定 20sp 時「1832」這種四位數會超過一格的寬度、被裁成「183…」——1×1 只剩一個數字，
 * 那個數字再被裁掉就什麼都不剩了。RemoteViews 上的 autoSizeTextType 不是每個版本都吃，
 * 所以自己依長度分段（負號也算一位，所以吃超標的「-432」走四位數那段）。
 */
fun tinyTextSp(text: String): Float = when {
    text.length <= 3 -> 22f
    text.length == 4 -> 18f
    else -> 15f
}

/** 進度條最多滿格；吃超過由「剩餘」的負數表示 */
fun widgetPercent(value: Int, target: Int): Int =
    if (target <= 0) 0 else (value * 100 / target).coerceIn(0, 100)

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
