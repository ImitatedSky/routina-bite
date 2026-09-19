package com.routina.bite.data

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.routina.bite.MainActivity
import com.routina.bite.R
import com.routina.bite.ShortcutActivity
import com.routina.bite.model.DiaryEntry
import com.routina.bite.model.Food

/**
 * 桌面捷徑。靜態的兩個（記一餐、喝水）在 res/xml/shortcuts.xml，
 * 這裡只管動態的部分：最近吃過的前兩個食物，點下去直接記 1 份。
 */

/** 動態捷徑只放兩個：長按選單裡靜態的已經佔兩格，再多就要捲 */
private const val RECENT_LIMIT = 2

private const val FOOD_ID_PREFIX = "food_"

/** 與 res/xml/shortcuts.xml 裡的 shortcutId 對應，釘選時要用同一個 id 才會釘到同一個捷徑 */
private const val ID_WATER = "water_250"
private const val ID_LOG_MEAL = "log_meal"

private const val WATER_ML = 250

/**
 * 依最近吃的食物重發動態捷徑。食物被刪掉時重算自然就沒有它，對應的捷徑跟著消失。
 *
 * 會做 IPC，呼叫端要在背景執行緒。
 */
fun updateDynamicShortcuts(context: Context, entries: List<DiaryEntry>, foods: List<Food>) {
    val recent = recentFoods(entries, foods, RECENT_LIMIT)
    try {
        // 內容沒變就不要再發一次：系統對 setDynamicShortcuts 有呼叫次數限制
        if (alreadyPublished(context, recent)) return
        ShortcutManagerCompat.setDynamicShortcuts(
            context,
            recent.mapIndexed { index, food -> foodShortcut(context, food, index) }
        )
    } catch (t: Throwable) {
        // 捷徑發不出去不影響 App 本身的功能
    }
}

private fun alreadyPublished(context: Context, recent: List<Food>): Boolean {
    val current = ShortcutManagerCompat.getDynamicShortcuts(context)
    if (current.size != recent.size) return false
    return current.zip(recent).all { (shortcut, food) ->
        shortcut.id == FOOD_ID_PREFIX + food.id && shortcut.shortLabel == food.name
    }
}

private fun foodShortcut(context: Context, food: Food, rank: Int): ShortcutInfoCompat =
    ShortcutInfoCompat.Builder(context, FOOD_ID_PREFIX + food.id)
        // 名稱太長由系統自己截斷
        .setShortLabel(food.name)
        .setLongLabel(context.getString(R.string.shortcut_food_long, food.name))
        .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_food))
        .setRank(rank)
        .setIntent(
            shortcutIntent(context)
                .putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_LOG_FOOD)
                .putExtra(ShortcutActivity.EXTRA_FOOD_ID, food.id)
        )
        .build()

/** 設定頁「加到桌面」用的喝水捷徑；id 與靜態的同一個 */
fun waterPinShortcut(context: Context): ShortcutInfoCompat =
    ShortcutInfoCompat.Builder(context, ID_WATER)
        .setShortLabel(context.getString(R.string.shortcut_water_short))
        .setLongLabel(context.getString(R.string.shortcut_water_long))
        .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_water))
        .setIntent(
            shortcutIntent(context)
                .putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_WATER)
                .putExtra(ShortcutActivity.EXTRA_ML, WATER_ML)
        )
        .build()

/** 設定頁「加到桌面」用的記一餐捷徑；id 與靜態的同一個 */
fun logMealPinShortcut(context: Context): ShortcutInfoCompat =
    ShortcutInfoCompat.Builder(context, ID_LOG_MEAL)
        .setShortLabel(context.getString(R.string.shortcut_log_meal_short))
        .setLongLabel(context.getString(R.string.shortcut_log_meal_long))
        .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_meal))
        .setIntent(
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(MainActivity.EXTRA_OPEN_ADD, true)
        )
        .build()

/** 捷徑的 Intent 一定要有 action，不然系統不收 */
private fun shortcutIntent(context: Context): Intent =
    Intent(context, ShortcutActivity::class.java).setAction(Intent.ACTION_VIEW)
