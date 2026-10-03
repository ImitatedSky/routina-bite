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
import kotlin.math.abs

/**
 * 桌面小工具：今天還剩多少大卡、喝了多少水，加上「+250 水」與「記一餐」兩顆按鈕。
 *
 * 用傳統 RemoteViews 而不是 Glance：畫面上只有幾個 TextView 與兩個圓環，
 * 不值得為此再拉進一整套 Compose 相依（D55）。圓環是畫好的 Bitmap（WidgetRings.kt）。
 *
 * 三種尺寸三個版面（D77）：1×1 是一個熱量環、中間是還能吃多少；矮而寬時小環加一行字；
 * 兩格高以上是兩個環（熱量、喝水）加兩顆按鈕。
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
        WidgetSize.TINY -> buildTiny(context, tinyRingDp(options))
        WidgetSize.ROW -> buildRow(context, widgetWidthDp(options))
        WidgetSize.FULL -> buildFull(context)
    }

/** 1×1：一個熱量環，中間是還能吃多少（吃超過就是超過多少） */
private fun buildTiny(context: Context, ringDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget_tiny)
    views.setImageViewBitmap(R.id.widget_ring, kcalRing(context, today))
    val text = remainingText(today)
    views.setTextViewText(R.id.widget_remaining, text)
    views.setTextViewTextSize(R.id.widget_remaining, TypedValue.COMPLEX_UNIT_SP, ringCenterTextSp(text, ringDp))
    views.setTextViewText(
        R.id.widget_remaining_label,
        context.getString(if (isOver(today)) R.string.widget_over else R.string.widget_remaining)
    )
    views.setViewVisibility(
        R.id.widget_remaining_label,
        if (ringHasRoomForLabel(ringDp)) View.VISIBLE else View.GONE
    )
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

/** 矮而寬：高度只有一格，放按鈕會把數字擠掉，所以只顯示不動作 */
private fun buildRow(context: Context, widthDp: Int): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget_row)
    views.setImageViewBitmap(R.id.widget_ring, kcalRing(context, today))
    views.setTextViewText(R.id.widget_remaining_label, remainingLabel(context, today))
    views.setTextViewText(R.id.widget_remaining, remainingText(today))
    // 2×1 的寬度只夠熱量那一組，喝水要 4×1 才擠得下
    val showWater = widthDp >= WIDGET_WIDE_DP
    views.setViewVisibility(R.id.widget_water_group, if (showWater) View.VISIBLE else View.GONE)
    if (showWater) {
        views.setImageViewBitmap(R.id.widget_water_ring, waterRing(context, today))
        views.setTextViewText(
            R.id.widget_water_amount,
            bigThenSmall(today.water.toString(), " / ${today.targets.water}")
        )
    }
    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    return views
}

private fun buildFull(context: Context): RemoteViews {
    val today = widgetToday(context)
    val views = RemoteViews(context.packageName, R.layout.bite_widget)
    views.setImageViewBitmap(R.id.widget_ring, kcalRing(context, today))
    views.setTextViewText(R.id.widget_remaining_label, remainingLabel(context, today))
    views.setTextViewText(R.id.widget_remaining, remainingText(today))

    views.setImageViewBitmap(R.id.widget_water_ring, waterRing(context, today))
    views.setTextViewText(
        R.id.widget_water_label,
        context.getString(
            if (today.water >= today.targets.water) R.string.widget_water_done
            else R.string.widget_water_label
        )
    )
    views.setTextViewText(
        R.id.widget_water_amount,
        // 欄寬只夠一個數字；喝了幾成由旁邊的圓環表示
        bigThenSmall(today.water.toString(), " ml")
    )

    views.setOnClickPendingIntent(android.R.id.background, openTodayIntent(context))
    views.setOnClickPendingIntent(R.id.widget_add_water, addWaterIntent(context))
    views.setOnClickPendingIntent(
        R.id.widget_log_meal,
        openAppIntent(context, REQUEST_OPEN_ADD, MainActivity.EXTRA_OPEN_ADD)
    )
    return views
}

private fun isOver(today: WidgetToday): Boolean = today.kcal > today.targets.kcal

/** 還能吃多少；吃超過時顯示超過多少（正數），由旁邊的「超過了」說明，負號在小字裡不好讀 */
private fun remainingText(today: WidgetToday): String =
    abs(today.targets.kcal - today.kcal).toString()

private fun remainingLabel(context: Context, today: WidgetToday): String =
    context.getString(if (isOver(today)) R.string.widget_over else R.string.widget_left_to_eat)

/** 熱量環：吃超過就換成深一點的珊瑚色，一眼看得出「滿過頭了」 */
private fun kcalRing(context: Context, today: WidgetToday) = ringBitmap(
    context,
    widgetFraction(today.kcal, today.targets.kcal),
    context.getColor(if (isOver(today)) R.color.widget_ring_kcal_over else R.color.widget_ring_kcal)
)

private fun waterRing(context: Context, today: WidgetToday) = ringBitmap(
    context,
    widgetFraction(today.water, today.targets.water),
    context.getColor(R.color.widget_ring_water)
)

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
