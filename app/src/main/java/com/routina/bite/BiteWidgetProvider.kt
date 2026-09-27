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
 * 桌面小工具：今天還剩多少大卡、喝了多少水，加上「+250 水」與「記一餐」兩顆按鈕。
 *
 * 用傳統 RemoteViews 而不是 Glance：畫面上只有幾個 TextView 與兩條 ProgressBar，
 * 不值得為此再拉進一整套 Compose 相依（D55）。
 *
 * 三種尺寸三個版面（D77）：1×1 只放剩餘熱量的數字，矮而寬時一行字加進度條，
 * 兩格高以上是完整版面。
 */
class BiteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 系統第一次放上小工具、重開機、updatePeriodMillis 到期時走這裡。
        // 此時 App 行程可能是剛被叫醒的，但 BiteApp.onCreate 已經跑完、
        // repository 是同步載入的，所以直接讀就有今天的數字。
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                id,
                buildWidget(context, appWidgetManager.getAppWidgetOptions(id))
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // 使用者拉了大小，換成對應尺寸的版面
        appWidgetManager.updateAppWidget(appWidgetId, buildWidget(context, newOptions))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 過了午夜或改了時區要換成新一天的數字，系統不會為此發 APPWIDGET_UPDATE
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            updateWidgets(context)
        }
    }
}

/**
 * 重畫桌面上所有 Bite 小工具，桌面上沒放就什麼都不做。
 *
 * 逐一處理每個 id：同一個小工具可以被放好幾份、各自是不同尺寸。
 * 會讀資料並做 IPC，呼叫端要在背景執行緒。
 */
fun updateWidgets(context: Context) {
    try {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, BiteWidgetProvider::class.java))
        for (id in ids) {
            manager.updateAppWidget(id, buildWidget(context, manager.getAppWidgetOptions(id)))
        }
    } catch (t: Throwable) {
        // 小工具畫不出來不影響 App 本身的功能
    }
}

private fun buildWidget(context: Context, options: Bundle?): RemoteViews =
    when (widgetSizeOf(options)) {
        WidgetSize.TINY -> buildTiny(context)
        WidgetSize.ROW -> buildRow(context, widgetWidthDp(options))
        WidgetSize.FULL -> buildFull(context)
    }

/** 1×1：40dp 見方放不下標籤或按鈕，只留最常被瞄一眼的那個數字 */
private fun buildTiny(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget_tiny)
    val text = remaining(today)
    views.setTextViewText(R.id.widget_remaining, text)
    views.setTextViewTextSize(R.id.widget_remaining, TypedValue.COMPLEX_UNIT_SP, tinyTextSp(text))
    views.setOnClickPendingIntent(R.id.widget_root, openTodayIntent(context))
    return views
}

/** 矮而寬：高度只有一格，放按鈕會把數字擠掉，所以只顯示不動作 */
private fun buildRow(context: Context, widthDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget_row)
    views.setTextViewText(R.id.widget_remaining, remaining(today))
    views.setProgressBar(
        R.id.widget_kcal_bar,
        100,
        widgetPercent(today.kcal, today.targets.kcal),
        false
    )
    // 2×1 的寬度只夠熱量那一欄，喝水要 4×1 才擠得下
    val showWater = widthDp >= WIDGET_WIDE_DP
    views.setViewVisibility(R.id.widget_water_amount, if (showWater) View.VISIBLE else View.GONE)
    if (showWater) {
        views.setTextViewText(
            R.id.widget_water_amount,
            context.getString(R.string.widget_water_amount, today.water, today.targets.water)
        )
    }
    views.setOnClickPendingIntent(R.id.widget_root, openTodayIntent(context))
    return views
}

private fun buildFull(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget)
    views.setTextViewText(R.id.widget_remaining, remaining(today))
    views.setTextViewText(
        R.id.widget_water_amount,
        context.getString(R.string.widget_water_amount, today.water, today.targets.water)
    )
    views.setProgressBar(
        R.id.widget_kcal_bar,
        100,
        widgetPercent(today.kcal, today.targets.kcal),
        false
    )
    views.setProgressBar(
        R.id.widget_water_bar,
        100,
        widgetPercent(today.water, today.targets.water),
        false
    )

    views.setOnClickPendingIntent(R.id.widget_root, openTodayIntent(context))
    views.setOnClickPendingIntent(R.id.widget_add_water, addWaterIntent(context))
    views.setOnClickPendingIntent(
        R.id.widget_log_meal,
        openAppIntent(context, REQUEST_OPEN_ADD, MainActivity.EXTRA_OPEN_ADD)
    )
    return views
}

/** 剩餘超標時是負的，照實顯示，不夾到 0 */
private fun remaining(today: WidgetToday): String =
    (today.targets.kcal - today.kcal).toString()

private fun openTodayIntent(context: Context): PendingIntent =
    openAppIntent(context, REQUEST_OPEN_TODAY, MainActivity.EXTRA_OPEN_TODAY)

/** 喝水重用桌面捷徑的跳板：不開畫面、加完水 toast 就結束 */
private fun addWaterIntent(context: Context): PendingIntent {
    val intent = Intent(context, ShortcutActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_WATER)
        .putExtra(ShortcutActivity.EXTRA_ML, WATER_ML)
    return PendingIntent.getActivity(context, REQUEST_ADD_WATER, intent, WIDGET_PENDING_FLAGS)
}

private const val WATER_ML = 250

// requestCode 三個小工具不能互撞，這塊用 1～3
private const val REQUEST_OPEN_TODAY = 1
private const val REQUEST_ADD_WATER = 2
private const val REQUEST_OPEN_ADD = 3
