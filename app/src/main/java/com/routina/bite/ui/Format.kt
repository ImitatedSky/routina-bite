package com.routina.bite.ui

import com.routina.bite.data.parseDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 顯示用的格式。
 *
 * 熱量有兩種寫法：合計用 [formatKcal] 取整數（一整天加起來的小數點是雜訊），
 * 單筆食物或單筆紀錄用 [formatKcalExact] 照實顯示——營養標示上就有 337.89 這種值，
 * 四捨五入會在使用者只是打開編輯頁看一眼的時候把它改掉。
 */

fun formatKcal(value: Double): Int = value.roundToInt()

/** 單筆的熱量：整數不帶小數點，有小數就留到兩位（去掉尾端的 0） */
fun formatKcalExact(value: Double): String =
    if (abs(value - value.roundToInt()) < 0.005) value.roundToInt().toString()
    else String.format(Locale.TAIWAN, "%.2f", value).trimEnd('0').trimEnd('.')

fun formatGrams(value: Double): String = String.format(Locale.TAIWAN, "%.1f", value)

/** 份數：整數就不顯示小數點，1.5 份仍然顯示 1.5 */
fun formatAmount(value: Double): String =
    if (abs(value - value.roundToInt()) < 0.001) value.roundToInt().toString()
    else String.format(Locale.TAIWAN, "%.2f", value).trimEnd('0').trimEnd('.')

private val dayLabel = DateTimeFormatter.ofPattern("M月d日（E）", Locale.TAIWAN)
private val dayLabelWithYear = DateTimeFormatter.ofPattern("yyyy年M月d日（E）", Locale.TAIWAN)

/** 不是今年的日期才帶年份，歷史列表裡跨年的日子才分得出來 */
fun formatDayLabel(date: String): String {
    val parsed = parseDate(date)
    val pattern = if (parsed.year == LocalDate.now().year) dayLabel else dayLabelWithYear
    return parsed.format(pattern)
}
