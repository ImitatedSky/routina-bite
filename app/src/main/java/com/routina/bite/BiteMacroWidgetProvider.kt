package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

/**
 * 桌面小工具之二：今天吃了多少熱量，以及蛋白質／脂肪／碳水各幾公克。
 *
 * 與另外兩個小工具的分工：這個回答「今天吃進去的東西長什麼樣」（狀態），
 * 「今日」回答「還能吃多少、水喝了沒」，「喝水」則是純動作。
 *
 * 一樣是三種尺寸三個版面（D77）：1×1 只放已吃熱量的數字。
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
        WidgetSize.TINY -> buildMacroTiny(context)
        WidgetSize.ROW -> buildMacroRow(context, widgetWidthDp(options))
        WidgetSize.FULL -> buildMacroFull(context)
    }

/** 1×1：這個小工具的主角是「已經吃了多少」，只留那個數字 */
private fun buildMacroTiny(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_tiny)
    views.setTextViewText(R.id.macro_widget_kcal, today.kcal.toString())
    views.setOnClickPendingIntent(R.id.macro_widget_root, openTodayIntent(context))
    return views
}

/** 矮而寬：一行熱量加三個營養素的數字，沒有進度條（高度只夠一行字） */
private fun buildMacroRow(context: Context, widthDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget_row)
    views.setTextViewText(
        R.id.macro_widget_kcal,
        context.getString(R.string.macro_widget_row_kcal, today.kcal)
    )
    // 2×1 的寬度塞不下三個營養素，硬放會被裁到看不出是哪個，不如只留熱量
    val showMacros = widthDp >= WIDGET_WIDE_DP
    views.setViewVisibility(
        R.id.macro_widget_macros,
        if (showMacros) View.VISIBLE else View.GONE
    )
    if (showMacros) {
        views.setTextViewText(
            R.id.macro_widget_protein,
            context.getString(R.string.macro_widget_row_protein, today.protein)
        )
        views.setTextViewText(
            R.id.macro_widget_fat,
            context.getString(R.string.macro_widget_row_fat, today.fat)
        )
        views.setTextViewText(
            R.id.macro_widget_carbs,
            context.getString(R.string.macro_widget_row_carbs, today.carbs)
        )
    }
    views.setOnClickPendingIntent(R.id.macro_widget_root, openTodayIntent(context))
    return views
}

private fun buildMacroFull(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget)
    views.setTextViewText(
        R.id.macro_widget_kcal,
        context.getString(R.string.macro_widget_kcal, today.kcal, today.targets.kcal)
    )
    views.setProgressBar(
        R.id.macro_widget_kcal_bar,
        100,
        widgetPercent(today.kcal, today.targets.kcal),
        false
    )
    views.setTextViewText(R.id.macro_widget_protein, grams(context, today.protein))
    views.setTextViewText(R.id.macro_widget_fat, grams(context, today.fat))
    views.setTextViewText(R.id.macro_widget_carbs, grams(context, today.carbs))

    views.setOnClickPendingIntent(R.id.macro_widget_root, openTodayIntent(context))
    return views
}

private fun grams(context: Context, value: Double): String =
    context.getString(R.string.macro_widget_grams, value)

private fun openTodayIntent(context: Context): PendingIntent =
    openAppIntent(context, REQUEST_OPEN_TODAY_FROM_MACRO, MainActivity.EXTRA_OPEN_TODAY)

// requestCode 三個小工具不能互撞，這塊用 11
private const val REQUEST_OPEN_TODAY_FROM_MACRO = 11
