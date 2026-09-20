package com.routina.bite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.routina.bite.data.entriesOn
import com.routina.bite.data.todayDate
import com.routina.bite.data.totalOf
import com.routina.bite.model.Targets
import kotlin.math.roundToInt

/**
 * 桌面小工具之二：今天吃了多少熱量，以及蛋白質／脂肪／碳水各幾公克。
 *
 * 與另一個小工具的分工：那個回答「還能吃多少、水喝了沒」並提供兩顆按鈕（動作），
 * 這個回答「今天吃進去的東西長什麼樣」（狀態）。兩個都放得下，各佔桌面 2 格。
 */
class BiteMacroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetManager.updateAppWidget(appWidgetIds, buildMacroWidget(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            updateMacroWidgets(context)
        }
    }
}

/** 重畫桌面上所有「今日營養」小工具。會讀資料並做 IPC，呼叫端要在背景執行緒 */
fun updateMacroWidgets(context: Context) {
    try {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, BiteMacroWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildMacroWidget(context))
    } catch (t: Throwable) {
        // 小工具畫不出來不影響 App 本身的功能
    }
}

private fun buildMacroWidget(context: Context): RemoteViews {
    val repository = (context.applicationContext as? BiteApp)?.repository
    val targets = repository?.targets?.value ?: Targets()
    // 一律是今天，不是 App 裡正在看的那一天
    val total = repository?.let { totalOf(entriesOn(it.entries.value, todayDate())) }
    val kcal = total?.kcal?.roundToInt() ?: 0

    val views = RemoteViews(context.packageName, R.layout.bite_macro_widget)
    views.setTextViewText(
        R.id.macro_widget_kcal,
        context.getString(R.string.macro_widget_kcal, kcal, targets.kcal)
    )
    views.setProgressBar(
        R.id.macro_widget_kcal_bar,
        100,
        if (targets.kcal <= 0) 0 else (kcal * 100 / targets.kcal).coerceIn(0, 100),
        false
    )
    views.setTextViewText(R.id.macro_widget_protein, grams(context, total?.protein ?: 0.0))
    views.setTextViewText(R.id.macro_widget_fat, grams(context, total?.fat ?: 0.0))
    views.setTextViewText(R.id.macro_widget_carbs, grams(context, total?.carbs ?: 0.0))

    views.setOnClickPendingIntent(R.id.macro_widget_root, openTodayIntent(context))
    return views
}

private fun grams(context: Context, value: Double): String =
    context.getString(R.string.macro_widget_grams, value)

private fun openTodayIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(MainActivity.EXTRA_OPEN_TODAY, true)
    return PendingIntent.getActivity(
        context,
        REQUEST_OPEN_TODAY_FROM_MACRO,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

// 與另一個小工具的 requestCode 不能撞（PendingIntent 只比對 requestCode 與 Intent 的過濾部分）
private const val REQUEST_OPEN_TODAY_FROM_MACRO = 11
