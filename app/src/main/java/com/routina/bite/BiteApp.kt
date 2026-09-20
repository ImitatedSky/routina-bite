package com.routina.bite

import android.app.Application
import com.routina.bite.data.BiteRepository
import com.routina.bite.data.updateDynamicShortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 整個 App 只有一份 [BiteRepository]，掛在 Application 上。
 * 資料量小，不需要 DI 框架，畫面端從 ViewModel 取得。
 */
class BiteApp : Application() {

    lateinit var repository: BiteRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = BiteRepository(this)
        // 動態捷徑跟著資料走：啟動時發一次，之後紀錄或食物庫一變就重發。
        // 這樣新增／刪除紀錄的每一個入口（含桌面捷徑自己）都不必各自記得呼叫。
        scope.launch {
            combine(repository.entries, repository.foods, ::Pair).collect { (entries, foods) ->
                updateDynamicShortcuts(this@BiteApp, entries, foods)
            }
        }
        // 桌面小工具同理：紀錄、喝水或目標一變就重畫，
        // 所以在 App 裡記一筆、按小工具的 +250、匯入備份都會即時反映。
        scope.launch {
            combine(repository.entries, repository.water, repository.targets) { _, _, _ -> }
                .collect {
                    updateWidgets(this@BiteApp)
                    updateMacroWidgets(this@BiteApp)
                }
        }
    }
}
