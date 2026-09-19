package com.routina.bite

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.routina.bite.data.defaultMeal
import com.routina.bite.data.todayDate
import com.routina.bite.model.DiaryEntry
import java.util.UUID
import kotlin.math.roundToInt

/**
 * 桌面捷徑的收件人：長按 App 圖示（或釘在桌面）選「喝水」「記 1 份 ⋯」時走這裡。
 *
 * 透明、讀完 extras 就做事、立刻 finish，使用者看不到任何畫面。
 * 這是 App 外的入口，任何失敗都只以 Toast 回饋，絕不崩潰。
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        run()
        finish()
    }

    private fun run() {
        when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_WATER -> addWater(intent.getIntExtra(EXTRA_ML, DEFAULT_ML))
            ACTION_LOG_FOOD -> logFood(intent.getStringExtra(EXTRA_FOOD_ID))
            else -> toast(getString(R.string.shortcut_unknown))
        }
    }

    private fun addWater(ml: Int) {
        val repository = (application as? BiteApp)?.repository ?: return
        // 桌面捷徑的語意就是「現在」，所以一律記在今天，
        // 不能用 App 裡正在看的那一天（使用者可能停在上週某一天）
        val date = todayDate()
        repository.addWater(date, ml)
        toast(getString(R.string.shortcut_water_done, ml, repository.waterOn(date), repository.targets.value.water))
    }

    private fun logFood(foodId: String?) {
        val repository = (application as? BiteApp)?.repository ?: return
        val food = foodId?.let { repository.findFood(it) }
        if (food == null) {
            toast(getString(R.string.shortcut_food_missing))
            return
        }
        // 同上：日期用今天，餐別依當下時間，與 App 裡按 ＋ 的預設一致
        repository.addEntry(
            DiaryEntry(
                id = UUID.randomUUID().toString(),
                date = todayDate(),
                meal = defaultMeal(),
                foodId = food.id,
                name = food.name,
                servings = 1.0,
                servingGrams = food.servingGrams,
                perServing = food.nutrients,
                createdAt = System.currentTimeMillis()
            )
        )
        toast(getString(R.string.shortcut_food_done, food.name, food.nutrients.kcal.roundToInt()))
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        /** 這個捷徑要做什麼：[ACTION_WATER] 或 [ACTION_LOG_FOOD] */
        const val EXTRA_ACTION = "com.routina.bite.extra.SHORTCUT_ACTION"

        /** 喝水的毫升數，沒帶就是 [DEFAULT_ML] */
        const val EXTRA_ML = "com.routina.bite.extra.ML"

        /** 要記哪一個食物，值是食物庫的 id */
        const val EXTRA_FOOD_ID = "com.routina.bite.extra.FOOD_ID"

        const val ACTION_WATER = "water"
        const val ACTION_LOG_FOOD = "log_food"

        private const val DEFAULT_ML = 250
    }
}
