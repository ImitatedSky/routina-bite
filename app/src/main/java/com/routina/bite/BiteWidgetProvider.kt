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
 * 桌面小工具：今天還剩多少大卡、喝了多少水，加上「+250 水」與「記一餐」兩顆按鈕。
 *
 * 用傳統 RemoteViews 而不是 Glance：畫面上只有幾個 TextView 與兩條 ProgressBar，
 * 不值得為此再拉進一整套 Compose 相依（D55）。
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
        appWidgetManager.updateAppWidget(appWidgetIds, buildWidget(context))
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
 * 會讀資料並做 IPC，呼叫端要在背景執行緒。
 */
fun updateWidgets(context: Context) {
    try {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, BiteWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildWidget(context))
    } catch (t: Throwable) {
        // 小工具畫不出來不影響 App 本身的功能
    }
}

private fun buildWidget(context: Context): RemoteViews {
    val repository = (context.applicationContext as? BiteApp)?.repository
    val targets = repository?.targets?.value ?: Targets()
    // 小工具看的一律是今天，不是 App 裡正在看的那一天（與桌面捷徑同一條規則）
    val date = todayDate()
    val kcal = repository?.let { totalOf(entriesOn(it.entries.value, date)).kcal }?.roundToInt() ?: 0
    val water = repository?.waterOn(date) ?: 0

    val views = RemoteViews(context.packageName, R.layout.bite_widget)
    // 剩餘超標時是負的，照實顯示，不夾到 0
    views.setTextViewText(R.id.widget_remaining, (targets.kcal - kcal).toString())
    views.setTextViewText(
        R.id.widget_water_amount,
        context.getString(R.string.widget_water_amount, water, targets.water)
    )
    views.setProgressBar(R.id.widget_kcal_bar, 100, percent(kcal, targets.kcal), false)
    views.setProgressBar(R.id.widget_water_bar, 100, percent(water, targets.water), false)

    views.setOnClickPendingIntent(
        R.id.widget_root,
        mainActivityIntent(context, REQUEST_OPEN_TODAY, MainActivity.EXTRA_OPEN_TODAY)
    )
    views.setOnClickPendingIntent(R.id.widget_add_water, addWaterIntent(context))
    views.setOnClickPendingIntent(
        R.id.widget_log_meal,
        mainActivityIntent(context, REQUEST_OPEN_ADD, MainActivity.EXTRA_OPEN_ADD)
    )
    return views
}

/** 進度條最多滿格；吃超過由「剩餘」的負數表示 */
private fun percent(value: Int, target: Int): Int =
    if (target <= 0) 0 else (value * 100 / target).coerceIn(0, 100)

private fun mainActivityIntent(context: Context, requestCode: Int, extra: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(extra, true)
    return PendingIntent.getActivity(context, requestCode, intent, PENDING_INTENT_FLAGS)
}

/** 喝水重用桌面捷徑的跳板：不開畫面、加完水 toast 就結束 */
private fun addWaterIntent(context: Context): PendingIntent {
    val intent = Intent(context, ShortcutActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_WATER)
        .putExtra(ShortcutActivity.EXTRA_ML, WATER_ML)
    return PendingIntent.getActivity(context, REQUEST_ADD_WATER, intent, PENDING_INTENT_FLAGS)
}

private const val WATER_ML = 250

// PendingIntent 只比對 requestCode 與 Intent 的「過濾」部分（不含 extras），
// 三個要各用一個 requestCode，否則後面的會蓋掉前面的
private const val REQUEST_OPEN_TODAY = 1
private const val REQUEST_ADD_WATER = 2
private const val REQUEST_OPEN_ADD = 3

private const val PENDING_INTENT_FLAGS =
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
