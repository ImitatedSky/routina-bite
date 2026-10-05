package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlin.math.max

/**
 * 桌面小工具之二：今天吃了多少熱量，以及蛋白質／脂肪／碳水各幾公克。
 *
 * 與另外兩個小工具的分工：這個回答「今天吃進去的東西長什麼樣」（狀態），
 * 「今日」回答「還能吃多少、水喝了沒」，「喝水」則是純動作。
 *
 * 一樣是三種尺寸三個版面（D77）。每個尺寸都有一個組成環：三大營養素換算成熱量後各佔幾成，
 * 和 App 裡「當日組成」那條算法一樣；1×1 環的中間是已吃熱量。
 */
class BiteMacroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                id,
                buildMacroWidget(context, appWidgetManager.getAppWidgetOptions(id))
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildMacroWidget(context, newOptions))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            updateMacroWidgets(context)
        }
    }
}

/** 重畫桌面上所有「今日營養」小工具，逐一處理每個 id（各自可能是不同尺寸） */
fun updateMacroWidgets(context: Context) {
    try {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, BiteMacroWidgetProvider::class.java)
        )
        for (id in ids) {
            manager.updateAppWidget(id, buildMacroWidget(context, manager.getAppWidgetOptions(id)))
        }
    } catch (t: Throwable) {
        // 小工具畫不出來不影響 App 本身的功能
    }
}

private fun buildMacroWidget(context: Context, options: Bundle?): RemoteViews =
    when (widgetSizeOf(options)) {
        WidgetSize.TINY -> buildMacroTiny(context, tinyRingDp(options))
        WidgetSize.ROW -> buildMacroRow(context, options)
        WidgetSize.FULL -> buildMacroFull(context, options)
    }

/** 1×1：組成環，中間是已經吃了多少 */
private fun buildMacroTiny(context: Context, ringDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_tiny)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    val text = today.kcal.toString()
    val numberSp = ringCenterTextSp(text, ringDp)
    views.setTextViewText(R.id.macro_widget_kcal, text)
    views.setTextViewTextSize(R.id.macro_widget_kcal, TypedValue.COMPLEX_UNIT_SP, numberSp)
    views.setTextViewTextSize(R.id.macro_widget_kcal_label, TypedValue.COMPLEX_UNIT_SP, ringLabelSp(numberSp))
    views.setViewVisibility(
        R.id.macro_widget_kcal_label,
        if (ringHasRoomForLabel(ringDp)) View.VISIBLE else View.GONE
    )
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

/**
 * 矮而寬：小組成環加已吃熱量，右邊放三個營養素。放多少看實際剩下的寬度，由多到少試：
 * 1. 「620 大卡」＋「蛋白 30」連名稱
 * 2. 「620 大卡」＋色點與數字（顏色和組成環同一組）
 * 3. 「620」＋色點與數字（上面已經寫了「已吃」，單位拿掉也看得懂）
 * 4. 只有熱量一組（版面會把它置中）
 */
private fun buildMacroRow(context: Context, options: Bundle?): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_row)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    val number = today.kcal.toString()
    val unit = " " + context.getString(R.string.widget_kcal_unit)

    val room = contentWidthDp(options, ROW_PADDING_DP)
    val eatenLabel = textWidthDp(context, context.getString(R.string.macro_widget_eaten), 11f)
    val kcalWithUnit = ROW_RING_DP + 10 + max(
        eatenLabel,
        textWidthDp(context, number, 20f) + textWidthDp(context, unit, 20f * 0.55f)
    )
    val kcalBare = ROW_RING_DP + 10 + max(eatenLabel, textWidthDp(context, number, 20f))
    val names = listOf(R.string.macro_short_protein, R.string.macro_short_fat, R.string.macro_short_carbs)
        .map { context.getString(it) }
    val grams = listOf(today.protein, today.fat, today.carbs)
        .map { context.getString(R.string.macro_widget_row_grams, it) }
    val labeled = names.zip(grams) { name, value -> "$name $value" }
    var showUnit = true
    val macros = when {
        kcalWithUnit + macrosWidth(context, labeled) <= room -> labeled
        kcalWithUnit + macrosWidth(context, grams) <= room -> grams
        kcalBare + macrosWidth(context, grams) <= room -> grams.also { showUnit = false }
        else -> null
    }
    views.setTextViewText(
        R.id.macro_widget_kcal,
        if (showUnit) bigThenSmall(number, unit) else number
    )

    val visibility = if (macros != null) View.VISIBLE else View.GONE
    views.setViewVisibility(R.id.macro_widget_macros, visibility)
    views.setViewVisibility(R.id.macro_widget_spacer, visibility)
    if (macros != null) {
        views.setTextViewText(R.id.macro_widget_protein, macros[0])
        views.setTextViewText(R.id.macro_widget_fat, macros[1])
        views.setTextViewText(R.id.macro_widget_carbs, macros[2])
    }
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

/** 三個「色點 7 + 間距 3 + 字」，組與組之間 8，前面再空一段和熱量那組隔開 */
private fun macrosWidth(context: Context, texts: List<String>): Float =
    ROW_GROUP_GAP_DP + texts.sumOf { 7.0 + 3 + textWidthDp(context, it, 12f) }.toFloat() + 2 * 8

private fun buildMacroFull(context: Context, options: Bundle?): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    views.setTextViewText(R.id.macro_widget_kcal, today.kcal.toString())
    views.setTextViewText(
        R.id.macro_widget_target,
        context.getString(R.string.macro_widget_target, today.targets.kcal)
    )
    val protein = grams(context, today.protein)
    val fat = grams(context, today.fat)
    val carbs = grams(context, today.carbs)
    views.setTextViewText(R.id.macro_widget_protein, protein)
    views.setTextViewText(R.id.macro_widget_fat, fat)
    views.setTextViewText(R.id.macro_widget_carbs, carbs)

    // 右邊三列可用的寬度：扣掉環 68 與間距 16；放不下「蛋白質」這些名稱就藏起來，只留色點對顏色
    val room = contentWidthDp(options, 20) - 68 - 16
    val widest = listOf(protein, fat, carbs).maxOf { textWidthDp(context, it, 15f) }
    val needed = 8 + 8 + textWidthDp(context, context.getString(R.string.macro_protein), 13f) + 6 + widest
    val labelVisibility = if (needed <= room) View.VISIBLE else View.GONE
    views.setViewVisibility(R.id.macro_widget_protein_label, labelVisibility)
    views.setViewVisibility(R.id.macro_widget_fat_label, labelVisibility)
    views.setViewVisibility(R.id.macro_widget_carbs_label, labelVisibility)

    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

/** 蛋白質與碳水 4 kcal/g、脂肪 9 kcal/g，與 App 裡的當日組成同一個算法 */
private fun macroRing(context: Context, today: WidgetToday) = donutBitmap(
    context,
    listOf(
        today.protein * 4 to context.getColor(R.color.widget_ring_protein),
        today.fat * 9 to context.getColor(R.color.widget_ring_fat),
        today.carbs * 4 to context.getColor(R.color.widget_ring_carbs)
    ),
    context.getColor(R.color.widget_ring_track)
)

private fun grams(context: Context, value: Double): String =
    context.getString(R.string.macro_widget_grams, value)

private fun openTodayIntent(context: Context): PendingIntent =
    openAppIntent(context, REQUEST_OPEN_TODAY_FROM_MACRO, MainActivity.EXTRA_OPEN_TODAY)

// requestCode 三個小工具不能互撞，這塊用 11
private const val REQUEST_OPEN_TODAY_FROM_MACRO = 11
