package com.routina.bite.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.bite.R
import com.routina.bite.model.DiaryEntry
import com.routina.bite.model.Food
import com.routina.bite.model.Meal
import com.routina.bite.model.Nutrients
import kotlinx.coroutines.launch

/**
 * 表單正在編輯的一筆紀錄。[basis] 是「每一份」的營養值，表單上顯示的是 basis × [servings]。
 */
data class EntryDraft(
    val name: String = "",
    val servings: Double = 1.0,
    val servingGrams: Double? = null,
    val basis: Nutrients = Nutrients.EMPTY,
    val meal: Meal?,
    val note: String = "",
    val photo: String = "",
    val foodId: String? = null
)

/** 空白表單 */
fun draftOf(meal: Meal?) = EntryDraft(meal = meal)

/**
 * 從食物庫點一個食物：用它的數值把表單填好。
 *
 * 備註也一起帶過來——食物庫裡的備註寫的就是「這東西是什麼」（麵包種類、醬料、份量說明），
 * 記下來的那一筆沒有理由把它丟掉，要改再改就好。
 */
fun draftOf(food: Food, meal: Meal?) = EntryDraft(
    name = food.name,
    servingGrams = food.servingGrams,
    basis = food.nutrients,
    meal = meal,
    note = food.note,
    foodId = food.id
)

/** 編輯既有紀錄 */
fun draftOf(entry: DiaryEntry) = EntryDraft(
    name = entry.name,
    servings = entry.servings,
    servingGrams = entry.servingGrams,
    basis = entry.perServing,
    meal = entry.meal,
    note = entry.note,
    photo = entry.photo,
    foodId = entry.foodId
)

/** 份數的真值來自哪裡。沒動過就用帶進來的精確份數，不要拿四捨五入過的欄位文字回推 */
private enum class AmountEdit { NONE, SERVINGS, GRAMS }

/** 表單裡的照片縮圖大小 */
private val PHOTO_SIZE = 64.dp

/**
 * 表單的狀態。新增紀錄頁的內嵌表單與編輯紀錄的對話框共用這一份，
 * 差別只在誰來畫、按鈕放哪裡。
 *
 * 連動規則：改份數或公克 → 九個營養欄以 basis × 份數 重算；
 * 直接改某個營養欄 → basis 的那一項改成 輸入值 ÷ 份數。最後動的那個就是使用者要的值。
 */
@Stable
class EntryFormState(initial: EntryDraft) {

    /** 這張表單現在以哪一筆 draft 為底。點食物重填時會換成那個食物的 draft */
    var source by mutableStateOf(initial)
        private set

    var name by mutableStateOf("")
    var servings by mutableStateOf("")
        private set
    var grams by mutableStateOf("")
        private set

    /** 每份幾公克。只有「同時加進食物庫」打開時才給改，那時才有東西要記這個基準 */
    var servingGramsText by mutableStateOf("")
        private set

    var kcal by mutableStateOf("")
        private set

    /** 熱量是使用者自己填的（或食物庫帶來的標示值），別被三大營養素的換算蓋掉 */
    private var kcalIsManual = initial.basis.kcal > 0.0
    var protein by mutableStateOf("")
        private set
    var fat by mutableStateOf("")
        private set
    var carbs by mutableStateOf("")
        private set
    var satFat by mutableStateOf("")
        private set
    var transFat by mutableStateOf("")
        private set
    var sugar by mutableStateOf("")
        private set
    var sodium by mutableStateOf("")
        private set
    var cholesterol by mutableStateOf("")
        private set

    var meal by mutableStateOf(initial.meal)
    var note by mutableStateOf("")
    var photo by mutableStateOf("")

    /** 打開就順手把這一筆存成食物庫的一筆，下次點一下就填好 */
    var saveToLibrary by mutableStateOf(false)
    var category by mutableStateOf("")
    var favorite by mutableStateOf(false)

    /** 每一份的營養值；欄位顯示的是 basis × 份數 */
    private var basis by mutableStateOf(initial.basis)
    private var edited by mutableStateOf(AmountEdit.NONE)

    private val added = mutableStateListOf<String>()

    init {
        loadFrom(initial)
    }

    /** 這次表單新寫進 filesDir、還沒歸屬給任何紀錄的照片檔 */
    val newPhotos: List<String> get() = added

    /** 有每份公克基準才給公克欄 */
    val servingGrams: Double? get() = source.servingGrams?.takeIf { it > 0.0 }

    /** 本來就未分餐的紀錄才給「未分餐」這個選項；新增一律要選一餐 */
    val allowNoMeal: Boolean get() = source.meal == null

    /** 這一筆還不是食物庫裡的東西，才有「加進食物庫」可言 */
    val canSaveToLibrary: Boolean get() = source.foodId == null

    val amount: Double?
        get() = when (edited) {
            AmountEdit.NONE -> source.servings
            AmountEdit.SERVINGS -> servings.toDoubleOrNull()
            // 公克是最後改的那一個，份數就用公克直接除回去算，不然 150 g 會存成 150.6 g
            AmountEdit.GRAMS -> servingGrams?.let { grams.toDoubleOrNull()?.div(it) }
        }

    /** 存進食物庫就得有名字——庫裡多一筆叫「快速輸入」的東西沒有任何用處 */
    val valid: Boolean
        get() = (amount ?: 0.0) > 0.0 && !(saveToLibrary && name.isBlank())

    fun updateServings(input: String) {
        servings = input
        edited = AmountEdit.SERVINGS
        val value = input.toDoubleOrNull()
        val perServing = servingGrams
        if (perServing != null) {
            grams = if (value == null) "" else formatGrams(value * perServing)
        }
        fillNutrients(value)
    }

    fun updateGrams(input: String) {
        val perServing = servingGrams ?: return
        grams = input
        edited = AmountEdit.GRAMS
        val value = input.toDoubleOrNull()?.div(perServing)
        servings = if (value == null) "" else formatAmount(value)
        fillNutrients(value)
    }

    /**
     * 每份幾公克。改它不動份數，只讓公克欄跟著重算——
     * 使用者說的是「一份是 100 克」，不是「我吃的份數變了」。
     */
    fun updateServingGrams(input: String) {
        // 先把現在的份數定下來：source 一改，用公克回推的 amount 就跟著變了
        val value = amount
        servingGramsText = input
        source = source.copy(servingGrams = input.toDoubleOrNull()?.takeIf { it > 0.0 })
        edited = AmountEdit.SERVINGS
        if (value != null) {
            servings = formatAmount(value)
            grams = servingGrams?.let { formatGrams(value * it) }.orEmpty()
        }
    }

    /**
     * 熱量欄。使用者一動它就記成「自己填的」，之後改三大營養素不再覆蓋它；
     * 清空熱量欄就交還給自動換算（這是唯一回到自動的方式，也是最直覺的：欄位空了就讓它自己算）。
     */
    fun updateKcal(input: String) {
        kcal = input
        kcalIsManual = input.isNotBlank()
        setBasis(input) { basis.copy(kcal = it) }
        if (!kcalIsManual) recalcKcalFromMacros()
    }

    fun updateProtein(input: String) {
        protein = input
        setBasis(input) { basis.copy(protein = it) }
        recalcKcalFromMacros()
    }

    fun updateFat(input: String) {
        fat = input
        setBasis(input) { basis.copy(fat = it) }
        recalcKcalFromMacros()
    }

    fun updateCarbs(input: String) {
        carbs = input
        setBasis(input) { basis.copy(carbs = it) }
        recalcKcalFromMacros()
    }

    // 以下五項不參與熱量換算（標示上的飽和／反式脂肪已經含在脂肪裡，糖含在碳水裡）
    fun updateSatFat(input: String) {
        satFat = input
        setBasis(input) { basis.copy(satFat = it) }
    }

    fun updateTransFat(input: String) {
        transFat = input
        setBasis(input) { basis.copy(transFat = it) }
    }

    fun updateSugar(input: String) {
        sugar = input
        setBasis(input) { basis.copy(sugar = it) }
    }

    fun updateSodium(input: String) {
        sodium = input
        setBasis(input) { basis.copy(sodium = it) }
    }

    fun updateCholesterol(input: String) {
        cholesterol = input
        setBasis(input) { basis.copy(cholesterol = it) }
    }

    /**
     * 三大營養素換算熱量。只在使用者沒有自己填熱量時才算——食物庫帶進來的熱量是標示值，
     * 標示值本來就不會等於這個換算（[DayCompositionCard] 的註解講過同一件事），不該被蓋掉。
     */
    private fun recalcKcalFromMacros() {
        if (kcalIsManual) return
        val perServing = kcalFromMacros(basis.protein, basis.fat, basis.carbs)
        basis = basis.copy(kcal = perServing)
        val value = amount ?: return
        kcal = kcalText(perServing, value)
    }

    /** 選到照片。檔案在回呼當下就寫進去了，先記帳，存檔或放棄時才決定留不留 */
    fun addPhoto(file: String) {
        added += file
        photo = file
    }

    fun forgetNewPhotos() = added.clear()

    /** 點一個食物、或開一張新表單：用這筆 draft 重填每一個欄位 */
    fun loadFrom(draft: EntryDraft) {
        source = draft
        name = draft.name
        servings = formatAmount(draft.servings)
        val perServing = draft.servingGrams?.takeIf { it > 0.0 }
        servingGramsText = perServing?.let { formatGrams(it) }.orEmpty()
        grams = perServing?.let { formatGrams(draft.servings * it) }.orEmpty()
        kcal = kcalText(draft.basis.kcal, draft.servings)
        protein = gramsText(draft.basis.protein, draft.servings)
        fat = gramsText(draft.basis.fat, draft.servings)
        carbs = gramsText(draft.basis.carbs, draft.servings)
        satFat = gramsText(draft.basis.satFat, draft.servings)
        transFat = gramsText(draft.basis.transFat, draft.servings)
        sugar = gramsText(draft.basis.sugar, draft.servings)
        sodium = gramsText(draft.basis.sodium, draft.servings)
        cholesterol = gramsText(draft.basis.cholesterol, draft.servings)
        meal = draft.meal
        note = draft.note
        photo = draft.photo
        basis = draft.basis
        edited = AmountEdit.NONE
        // 食物庫帶進來的熱量是標示值，算是「已經指定」；空白表單才交給自動換算
        kcalIsManual = draft.basis.kcal > 0.0
        // 換了一筆就重問一次要不要進食物庫（點到的食物本來就在庫裡）
        saveToLibrary = false
        category = ""
        favorite = false
    }

    /** 清空：回到空白表單，只留目前選的餐別 */
    fun reset() = loadFrom(draftOf(meal))

    /** 表單內容做成一筆 draft。份數不合法時回 null（按鈕本來就停用，這裡只是不讓它漏過去） */
    fun toDraft(quickName: String): EntryDraft? {
        val value = amount ?: return null
        return source.copy(
            name = name.trim().ifEmpty { quickName },
            servings = value,
            basis = basis,
            meal = meal,
            note = note.trim(),
            photo = photo
        )
    }

    /**
     * 表單內容做成食物庫的一筆。
     *
     * 存的是 [basis]（每一份）而不是欄位上顯示的總量——食物庫記的本來就是「一份有多少」，
     * 這也是為什麼記了 2 份之後存進庫裡，下次點它會正確地帶出一份的量。
     */
    fun toFood(id: String, createdAt: Long) = Food(
        id = id,
        name = name.trim(),
        servingGrams = servingGrams,
        nutrients = basis,
        note = note.trim(),
        category = category.trim(),
        favorite = favorite,
        createdAt = createdAt
    )

    /** 份數或公克改了：九個營養欄以 basis × 份數 重算 */
    private fun fillNutrients(value: Double?) {
        if (value == null) return
        kcal = kcalText(basis.kcal, value)
        protein = gramsText(basis.protein, value)
        fat = gramsText(basis.fat, value)
        carbs = gramsText(basis.carbs, value)
        satFat = gramsText(basis.satFat, value)
        transFat = gramsText(basis.transFat, value)
        sugar = gramsText(basis.sugar, value)
        sodium = gramsText(basis.sodium, value)
        cholesterol = gramsText(basis.cholesterol, value)
    }

    // 直接改營養欄：把每份值改成 輸入值 ÷ 份數。份數還不合法就只改文字，等份數修好再算
    private fun setBasis(input: String, apply: (Double) -> Nutrients) {
        val entered = if (input.isBlank()) 0.0 else input.toDoubleOrNull() ?: return
        val value = amount ?: return
        if (value <= 0.0) return
        basis = apply(entered / value)
    }
}

@Composable
fun rememberEntryFormState(initial: EntryDraft): EntryFormState = remember { EntryFormState(initial) }

/** 存檔成功：留下的那張照片歸紀錄所有，換掉的與這次沒用上的都刪掉 */
fun EntryFormState.commitPhotos(viewModel: BiteViewModel) {
    newPhotos.forEach { if (it != photo) viewModel.deletePhoto(it) }
    if (source.photo.isNotEmpty() && source.photo != photo) viewModel.deletePhoto(source.photo)
    forgetNewPhotos()
}

/**
 * 放棄這次的輸入（取消對話框、清空表單、離開新增頁）：這次新寫的檔全刪，
 * 既有紀錄原本的照片留著。
 */
fun EntryFormState.dropNewPhotos(viewModel: BiteViewModel) {
    newPhotos.forEach { viewModel.deletePhoto(it) }
    forgetNewPhotos()
}

/**
 * 表單的欄位本體。編輯對話框與新增紀錄頁畫的是同一份。
 *
 * 營養欄與分類、常用都用 [NutrientFields]／[CategoryField]／[FavoriteSwitch]，
 * 和食物編輯頁是同一份元件——兩邊填的就是同一張營養標示。
 *
 * [showMeal] 在新增紀錄頁是 false：那裡的餐別晶片在頁面最上面，不由表單畫。
 * [showSaveToLibrary] 只有新增紀錄頁給 true：改一筆舊紀錄不該順便產生新食物。
 *
 * 照片要 [viewModel]：選到圖就得當場複製進 App 目錄（URI 的授權只在回呼那一趟有效）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryFormFields(
    state: EntryFormState,
    viewModel: BiteViewModel,
    modifier: Modifier = Modifier,
    showMeal: Boolean = true,
    showSaveToLibrary: Boolean = false,
    categories: List<String> = emptyList()
) {
    val quickName = stringResource(R.string.add_quick)
    var viewingPhoto by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = viewModel.importPhoto(uri)
            photoFailed = file == null
            if (file != null) state.addPhoto(file)
        }
    }

    fun pickPhoto() {
        photoFailed = false
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val photoState = rememberPhoto(state.photo, with(LocalDensity.current) { PHOTO_SIZE.roundToPx() })
    // 檔案不在（例如匯入了只有 JSON 的備份）就當作沒有照片，不顯示破圖
    val hasPhoto = state.photo.isNotEmpty() && photoState !is PhotoState.Missing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it },
            label = { Text(stringResource(R.string.field_name)) },
            // 預設值只在存檔時補上，不先塞進欄位讓使用者刪
            placeholder = { Text(quickName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = state.servings,
                onValueChange = { state.updateServings(it) },
                label = stringResource(R.string.add_servings),
                modifier = Modifier.weight(1f)
            )
            if (state.servingGrams != null) {
                NumberField(
                    value = state.grams,
                    onValueChange = { state.updateGrams(it) },
                    label = stringResource(R.string.add_grams),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        NutrientFields(
            kcal = NutrientField(state.kcal) { state.updateKcal(it) },
            protein = NutrientField(state.protein) { state.updateProtein(it) },
            fat = NutrientField(state.fat) { state.updateFat(it) },
            carbs = NutrientField(state.carbs) { state.updateCarbs(it) },
            satFat = NutrientField(state.satFat) { state.updateSatFat(it) },
            transFat = NutrientField(state.transFat) { state.updateTransFat(it) },
            sugar = NutrientField(state.sugar) { state.updateSugar(it) },
            sodium = NutrientField(state.sodium) { state.updateSodium(it) },
            cholesterol = NutrientField(state.cholesterol) { state.updateCholesterol(it) },
            hint = stringResource(R.string.entry_form_hint)
        )

        if (showMeal) {
            MealPicker(
                selected = state.meal,
                onSelect = { state.meal = it },
                allowNone = state.allowNoMeal
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPhoto) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhotoThumb(
                        state = photoState,
                        size = PHOTO_SIZE,
                        corner = 8.dp,
                        modifier = Modifier.combinedClickable(
                            onClick = { pickPhoto() },
                            onLongClick = { viewingPhoto = true }
                        )
                    )
                    Text(
                        text = stringResource(R.string.photo_hint),
                        style = MaterialTheme.typography.bodySmall.zh(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { state.photo = "" }) {
                        Icon(Icons.Default.Close, stringResource(R.string.photo_remove))
                    }
                }
            } else {
                OutlinedButton(onClick = { pickPhoto() }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Text(
                        text = stringResource(R.string.photo_add),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (photoFailed) {
                Text(
                    text = stringResource(R.string.photo_failed),
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        OutlinedTextField(
            value = state.note,
            onValueChange = { state.note = it },
            label = { Text(stringResource(R.string.entry_note)) },
            placeholder = { Text(stringResource(R.string.entry_note_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showSaveToLibrary && state.canSaveToLibrary) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.saveToLibrary,
                        onCheckedChange = { state.saveToLibrary = it }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(stringResource(R.string.add_save_to_library))
                        Text(
                            text = stringResource(R.string.add_save_to_library_hint),
                            style = MaterialTheme.typography.bodySmall.zh(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 每份公克、分類、常用只有食物庫用得到，沒要存就不要擺在那裡佔位子
                if (state.saveToLibrary) {
                    NumberField(
                        value = state.servingGramsText,
                        onValueChange = { state.updateServingGrams(it) },
                        label = stringResource(R.string.field_serving_grams),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CategoryField(
                        value = state.category,
                        onValueChange = { state.category = it },
                        categories = categories
                    )
                    FavoriteSwitch(
                        checked = state.favorite,
                        onCheckedChange = { state.favorite = it }
                    )
                }
            }
        }
    }

    if (viewingPhoto) {
        PhotoViewerDialog(name = state.photo, onDismiss = { viewingPhoto = false })
    }
}

/**
 * 編輯既有紀錄的對話框。新增走的是新增紀錄頁上的內嵌表單，
 * 這裡只剩「改一筆」這個情境——對話框正好合適。
 */
@Composable
fun EntryFormDialog(
    viewModel: BiteViewModel,
    initial: EntryDraft,
    onConfirm: (EntryDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberEntryFormState(initial)
    val quickName = stringResource(R.string.add_quick)

    fun cancel() {
        state.dropNewPhotos(viewModel)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { cancel() },
        title = { Text(stringResource(R.string.entry_edit_title)) },
        text = {
            EntryFormFields(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(
                enabled = state.valid,
                onClick = {
                    val draft = state.toDraft(quickName) ?: return@TextButton
                    state.commitPhotos(viewModel)
                    onConfirm(draft)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { cancel() }) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// 每份值是 0 就讓欄位留白：空白在這個 App 裡本來就當作 0，印一排 0 反而又要先刪字
private fun kcalText(perServing: Double, servings: Double): String =
    if (perServing == 0.0) "" else formatKcalExact(perServing * servings)

private fun gramsText(perServing: Double, servings: Double): String =
    if (perServing == 0.0) "" else formatGrams(perServing * servings)
