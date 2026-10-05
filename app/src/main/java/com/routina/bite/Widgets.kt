package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
    /** 一個圓環：1×1，或窄到排不下兩欄的（1×2 直的、2×2） */
    TINY,

    /** 矮而寬（2×1、3×1、4×1）：一行，放不下按鈕 */
    ROW,

    /** 夠高也夠寬：完整版面 */
    FULL
}

/** 桌面一格約 70dp、兩格 110dp；不到 110dp 就放不下完整版面 */
private const val COMPACT_DP = 110

/** 完整版面要並排兩欄（兩個環、或環加三列營養素），窄於這個寬度會互相擠壓 */
private const val FULL_MIN_WIDTH_DP = 200

/**
 * 從桌面回報的 options 讀出尺寸分桶。
 *
 * 沒有 options 或值是 0（舊桌面、剛綁上還沒回報）時當完整版面：
 * 那是小工具新放上去的預設尺寸。
 *
 * 高但窄的（一格寬拉長、或 2×2）用一個大圓環：完整版面在那個寬度下兩欄會互相擠掉，
 * 一個環撐滿反而最清楚。
 */
fun widgetSizeOf(options: Bundle?): WidgetSize {
    val width = widgetWidthDp(options)
    val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    if (width <= 0 || height <= 0) return WidgetSize.FULL
    if (height >= COMPACT_DP) {
        return if (width >= FULL_MIN_WIDTH_DP) WidgetSize.FULL else WidgetSize.TINY
    }
    return if (width >= COMPACT_DP) WidgetSize.ROW else WidgetSize.TINY
}

/**
 * Android 11 以下的桌面會替小工具每邊再加約 8dp 內距，12 以上沒有。
 * 只在舊版本扣，否則 Android 12 以上的手機會平白少掉 16dp、數字被縮小。
 */
private fun hostPaddingDp(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0 else 16

/**
 * 小工具裡實際能放內容的寬度（dp）：桌面回報的寬，扣掉我們自己的左右內距與舊桌面那圈。
 * 讀不到寬度時當作一般的 4 格寬。
 */
fun contentWidthDp(options: Bundle?, ownPaddingDp: Int): Float {
    val width = widgetWidthDp(options).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
    return (width - ownPaddingDp - hostPaddingDp()).toFloat()
}

private const val DEFAULT_WIDTH_DP = 300

/** 矮而寬版面的共同尺寸（兩個 *_row.xml 寫的值） */
const val ROW_PADDING_DP = 22
const val ROW_RING_DP = 36
const val ROW_GROUP_GAP_DP = 12

/** 一段字要塞進 [roomDp] 寬時該用多大的字級：放得下就用 [maxSp]，放不下就縮，最小 [minSp] */
fun fitTextSp(context: Context, text: CharSequence, roomDp: Float, maxSp: Float, minSp: Float): Float {
    val atMax = textWidthDp(context, text, maxSp)
    if (atMax <= roomDp) return maxSp
    return (maxSp * roomDp / atMax).coerceAtLeast(minSp)
}

/** 圓環中間那個數字底下的小標籤：跟著數字的字級走，大環上不要還是一行蚊子字 */
fun ringLabelSp(numberSp: Float): Float = (numberSp * 0.45f).coerceIn(9f, 14f)

/**
 * 粗估一段字在小工具上要多寬（dp），用來決定「放不放得下」。
 * 依實際截圖量過：數字與英文約 0.58 個字級寬、空白約 0.28、中文約 0.95；
 * 系統字級放大時跟著放大。不用 Paint 實際量：桌面用的字型不見得和我們一樣。
 *
 * 第一版一律抓 0.6／1.0，估得太寬，3 格寬的營養小工具明明放得下三個數字卻被藏起來。
 */
fun textWidthDp(context: Context, text: CharSequence, sp: Float): Float {
    val scale = context.resources.configuration.fontScale
    val ems = text.sumOf {
        when {
            it == ' ' -> 0.28
            it.code < 0x2E80 -> 0.58
            else -> 0.95
        }
    }
    return (ems * sp * scale).toFloat()
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
 * 圓環版面的環實際有多大（dp）。環以 fitCenter 撐滿格子，所以是格子的短邊扣掉內距；
 * 中間數字的字級要靠這個算。讀不到尺寸時用一般桌面一格的大小。
 * 扣的是我們自己的 6dp×2，舊桌面再扣它多加的那圈。
 */
fun tinyRingDp(options: Bundle?): Int {
    val width = widgetWidthDp(options)
    val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    if (width <= 0 || height <= 0) return 56
    return (minOf(width, height) - 12 - hostPaddingDp()).coerceIn(28, 160)
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
