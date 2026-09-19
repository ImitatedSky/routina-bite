package com.routina.bite.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.routina.bite.BiteApp
import com.routina.bite.data.BackupArchive
import com.routina.bite.data.BackupCodec
import com.routina.bite.data.entriesOn
import com.routina.bite.data.importPhotoFile
import com.routina.bite.data.photosDir
import com.routina.bite.data.shiftDate
import com.routina.bite.data.todayDate
import com.routina.bite.model.DayNote
import com.routina.bite.model.DiaryEntry
import com.routina.bite.model.Food
import com.routina.bite.model.Targets
import com.routina.bite.model.WaterDay
import com.routina.bite.model.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.UUID

/** 匯入的結果：成功時回目前的總筆數，失敗時回拒收原因 */
sealed interface ImportOutcome {
    data class Ok(val foods: Int, val entries: Int, val weights: Int, val notes: Int) : ImportOutcome
    data class Failed(val reason: BackupCodec.Reject) : ImportOutcome
}

class BiteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BiteApp).repository

    val foods: StateFlow<List<Food>> = repository.foods
    val entries: StateFlow<List<DiaryEntry>> = repository.entries
    val dayNotes: StateFlow<List<DayNote>> = repository.dayNotes
    val weights: StateFlow<List<WeightEntry>> = repository.weights
    val targets: StateFlow<Targets> = repository.targets
    val water: StateFlow<List<WaterDay>> = repository.water

    /** 目前被展開的分類（未分類是空字串）。食物庫頁與新增紀錄頁共用同一份 */
    val expandedCategories: StateFlow<Set<String>> = repository.expandedCategories

    // 目前看的是哪一天。歷史頁選日期後也是改這個值，所以 today 這條路由不需要參數
    private val _selectedDate = MutableStateFlow(todayDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun shiftSelectedDate(days: Long) {
        _selectedDate.value = shiftDate(_selectedDate.value, days)
    }

    fun backToToday() {
        _selectedDate.value = todayDate()
    }

    // ---------- 喝水 ----------

    /** 加減這一天的水量，[deltaMl] 可為負；總量不會低於 0 */
    fun addWater(date: String, deltaMl: Int) = repository.addWater(date, deltaMl)

    // ---------- 食物庫 ----------

    fun findFood(id: String): Food? = repository.findFood(id)

    fun saveFood(food: Food) = repository.upsertFood(food)

    fun deleteFood(id: String) = repository.deleteFood(id)

    fun toggleFavorite(food: Food) = repository.upsertFood(food.copy(favorite = !food.favorite))

    fun renameCategory(from: String, to: String) = repository.renameCategory(from, to)

    fun setCategoryExpanded(category: String, expanded: Boolean) =
        repository.setCategoryExpanded(category, expanded)

    fun newFoodId(): String = UUID.randomUUID().toString()

    // ---------- 紀錄 ----------

    fun findEntry(id: String): DiaryEntry? = repository.findEntry(id)

    /** 新增一筆。[draft] 是表單填出來的內容，foodId 為 null 就是快速輸入 */
    fun addEntry(date: String, draft: EntryDraft) {
        repository.addEntry(
            DiaryEntry(
                id = UUID.randomUUID().toString(),
                date = date,
                meal = draft.meal,
                foodId = draft.foodId,
                name = draft.name,
                servings = draft.servings,
                servingGrams = draft.servingGrams,
                perServing = draft.basis,
                note = draft.note,
                photo = draft.photo,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /** 改一筆。id、日期與建立時間留著，其餘一律以表單為準（含 foodId，常吃／最近才不會漏算） */
    fun updateEntry(entry: DiaryEntry, draft: EntryDraft) {
        repository.updateEntry(
            entry.copy(
                meal = draft.meal,
                foodId = draft.foodId,
                name = draft.name,
                servings = draft.servings,
                servingGrams = draft.servingGrams,
                perServing = draft.basis,
                note = draft.note,
                photo = draft.photo
            )
        )
    }

    /** 紀錄沒了，它的照片檔也該沒了 */
    fun deleteEntry(id: String) {
        findEntry(id)?.photo?.let { repository.deletePhoto(it) }
        repository.deleteEntry(id)
    }

    // ---------- 照片 ----------

    /** 把選到的圖縮小存進 App 私有目錄，回傳檔名；讀不到或壓不出來就回 null */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        importPhotoFile(getApplication(), uri)
    }

    fun deletePhoto(name: String) = repository.deletePhoto(name)

    /**
     * 把前一天的每一筆複製到 [date]，id 與時間都重新給。
     * 回傳複製的筆數，0 表示昨天沒有紀錄。
     */
    fun copyYesterday(date: String): Int {
        val source = entriesOn(entries.value, shiftDate(date, -1))
        if (source.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val copies = source.mapIndexed { index, entry ->
            entry.copy(
                id = UUID.randomUUID().toString(),
                date = date,
                // 照片不跟著複製：兩筆共用同一個檔，刪掉一筆就會把另一筆的圖也刪掉
                photo = "",
                // 加 index 讓複製出來的順序與昨天一致
                createdAt = now + index
            )
        }
        repository.addEntries(copies)
        return copies.size
    }

    fun setDayNote(date: String, note: String) = repository.setDayNote(date, note)

    fun dayNote(date: String): String =
        dayNotes.value.firstOrNull { it.date == date }?.note.orEmpty()

    // ---------- 體重 ----------

    fun saveWeight(entry: WeightEntry) = repository.upsertWeight(entry)

    fun deleteWeight(id: String) = repository.deleteWeight(id)

    fun newWeightId(): String = UUID.randomUUID().toString()

    // ---------- 目標 ----------

    fun setTargets(targets: Targets) = repository.setTargets(targets)

    // ---------- 備份 ----------

    /** [asZip] 由設定頁在按下匯出時決定（有任何一筆帶照片就包成 zip），檔名與 MIME 也是那時挑的 */
    fun exportBackup(uri: Uri, asZip: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = BackupCodec.encode(repository.buildBackup())
            val photos = if (asZip) repository.storedPhotos() else emptyList()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { out ->
                        if (asZip) {
                            BackupArchive.writeZip(out, content, photos)
                        } else {
                            out.write(content.toByteArray())
                        }
                    } ?: error("no output stream")
                }.isSuccess
            }
            onDone(ok)
        }
    }

    fun importBackup(uri: Uri, onDone: (ImportOutcome) -> Unit) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readBackup(uri) }
            if (text == null) {
                onDone(ImportOutcome.Failed(BackupCodec.Reject.UNREADABLE))
                return@launch
            }
            when (val parsed = BackupCodec.parse(text)) {
                is BackupCodec.Parsed.Failed -> onDone(ImportOutcome.Failed(parsed.reason))
                is BackupCodec.Parsed.Ok -> {
                    repository.applyBackup(parsed.backup)
                    onDone(
                        ImportOutcome.Ok(
                            foods = foods.value.size,
                            entries = entries.value.size,
                            weights = weights.value.size,
                            notes = dayNotes.value.size
                        )
                    )
                }
            }
        }
    }

    /**
     * 同一個入口要吃得下純 JSON 與帶照片的 zip。
     * 用內容的前兩個 byte 判斷（zip 一定是 PK），不看副檔名——選擇器給的是 URI，副檔名不可靠。
     */
    private fun readBackup(uri: Uri): String? = runCatching {
        val context = getApplication<Application>()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            input.mark(2)
            val zip = input.read() == 'P'.code && input.read() == 'K'.code
            input.reset()
            if (zip) {
                BackupArchive.readZip(input, photosDir(context))
            } else {
                input.bufferedReader().readText()
            }
        }
    }.getOrNull()
}
