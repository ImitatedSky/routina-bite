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
        WidgetSize.ROW -> buildMacroRow(context, widgetWidthDp(options))
        WidgetSize.FULL -> buildMacroFull(context)
    }

/** 1×1：組成環，中間是已經吃了多少 */
private fun buildMacroTiny(context: Context, ringDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_tiny)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    val text = today.kcal.toString()
    views.setTextViewText(R.id.macro_widget_kcal, text)
    views.setTextViewTextSize(R.id.macro_widget_kcal, TypedValue.COMPLEX_UNIT_SP, ringCenterTextSp(text, ringDp))
    views.setViewVisibility(
        R.id.macro_widget_kcal_label,
        if (ringHasRoomForLabel(ringDp)) View.VISIBLE else View.GONE
    )
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

/** 矮而寬：小組成環加已吃熱量，夠寬再排出三個營養素 */
private fun buildMacroRow(context: Context, widthDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_row)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    views.setTextViewText(R.id.macro_widget_kcal, bigThenSmall(today.kcal.toString(), " " + context.getString(R.string.widget_kcal_unit)))
    // 2×1 的寬度塞不下三個營養素，硬放會被裁到看不出是哪個，不如只留熱量。
    // 4×1 也只放得下數字，名稱靠色點對：和組成環、4×2、App 裡的當日組成是同一組顏色
    val showMacros = widthDp >= WIDGET_WIDE_DP
    views.setViewVisibility(
        R.id.macro_widget_macros,
        if (showMacros) View.VISIBLE else View.GONE
    )
    if (showMacros) {
        views.setTextViewText(
            R.id.macro_widget_protein,
            context.getString(R.string.macro_widget_row_grams, today.protein)
        )
        views.setTextViewText(
            R.id.macro_widget_fat,
            context.getString(R.string.macro_widget_row_grams, today.fat)
        )
        views.setTextViewText(
            R.id.macro_widget_carbs,
            context.getString(R.string.macro_widget_row_grams, today.carbs)
        )
    }
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

private fun buildMacroFull(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget)
    views.setImageViewBitmap(R.id.macro_widget_ring, macroRing(context, today))
    views.setTextViewText(R.id.macro_widget_kcal, today.kcal.toString())
    views.setTextViewText(
        R.id.macro_widget_target,
        context.getString(R.string.macro_widget_target, today.targets.kcal)
    )
    views.setTextViewText(R.id.macro_widget_protein, grams(context, today.protein))
    views.setTextViewText(R.id.macro_widget_fat, grams(context, today.fat))
    views.setTextViewText(R.id.macro_widget_carbs, grams(context, today.carbs))

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
