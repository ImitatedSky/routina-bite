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
 * 桌面小工具之三：一顆 1×1 的「+250」。
 *
 * 三個小工具裡唯一點下去會改資料的，所以整塊就是一顆按鈕，不放第二個點擊區——
 * 1×1 上沒有空間讓使用者分辨「這裡會加水、那裡會開 App」。
 */
class BiteWaterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                id,
                buildWaterWidget(context, appWidgetManager.getAppWidgetOptions(id))
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildWaterWidget(context, newOptions))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            updateWaterWidgets(context)
        }
    }
}

/** 重畫桌面上所有「喝水」小工具，逐一處理每個 id（各自可能是不同尺寸） */
fun updateWaterWidgets(context: Context) {
    try {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, BiteWaterWidgetProvider::class.java)
        )
        for (id in ids) {
            manager.updateAppWidget(id, buildWaterWidget(context, manager.getAppWidgetOptions(id)))
        }
    } catch (t: Throwable) {
        // 小工具畫不出來不影響 App 本身的功能
    }
}

/**
 * 1×1 是一個水的圓環、中間「+250」；放大之後換成橫的版面，把「+250 ml」放大、
 * 再講還差多少。內容始終是同一個動作加一行進度，不分三種版面。
 */
private fun buildWaterWidget(context: Context, options: Bundle?): RemoteViews {
    val today = widgetToday(context)
    val ring = ringBitmap(
        context,
        widgetFraction(today.water, today.targets.water),
        context.getColor(R.color.widget_ring_water)
    )

    val views = if (widgetSizeOf(options) == WidgetSize.TINY) {
        val ringDp = tinyRingDp(options)
        val numberSp = ringCenterTextSp(context.getString(R.string.water_widget_add), ringDp)
        RemoteViews(context.packageName, R.layout.bite_water_widget).apply {
            setTextViewTextSize(R.id.water_widget_add, TypedValue.COMPLEX_UNIT_SP, numberSp)
            setTextViewTextSize(R.id.water_widget_total, TypedValue.COMPLEX_UNIT_SP, ringLabelSp(numberSp))
            setTextViewText(
                R.id.water_widget_total,
                context.getString(R.string.water_widget_tiny_total, today.water)
            )
            setViewVisibility(
                R.id.water_widget_total,
                if (ringHasRoomForLabel(ringDp)) View.VISIBLE else View.GONE
            )
        }
    } else {
        val left = today.targets.water - today.water
        val sentence = if (left <= 0) {
            context.getString(R.string.water_widget_done, today.water, today.targets.water)
        } else {
            context.getString(R.string.water_widget_left, today.water, today.targets.water, left)
        }
        // 放得下整句「今天 500 / 2000 · 還差 1500」就講完整，不然只放「500 / 2000 ml」
        val room = contentWidthDp(options, 24) - 36 - 10
        val status = if (textWidthDp(context, sentence, 11f) <= room) {
            sentence
        } else {
            context.getString(R.string.water_widget_total, today.water, today.targets.water)
        }
        RemoteViews(context.packageName, R.layout.bite_water_widget_wide).apply {
            setTextViewText(R.id.water_widget_total, status)
        }
    }
    views.setImageViewBitmap(R.id.water_widget_ring, ring)
    views.setOnClickPendingIntent(android.R.id.background, addWaterIntent(context))
    return views
}

/** 重用桌面捷徑的跳板：不開畫面、加完水 toast 就結束 */
private fun addWaterIntent(context: Context): PendingIntent {
    val intent = Intent(context, ShortcutActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_WATER)
        .putExtra(ShortcutActivity.EXTRA_ML, WATER_ML)
    return PendingIntent.getActivity(context, REQUEST_ADD_WATER, intent, WIDGET_PENDING_FLAGS)
}

private const val WATER_ML = 250

// requestCode 三個小工具不能互撞，這塊從 21 起跳
private const val REQUEST_ADD_WATER = 21
