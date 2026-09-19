package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.bite.R
import com.routina.bite.data.defaultMeal
import com.routina.bite.model.DiaryEntry
import com.routina.bite.model.Food
import com.routina.bite.model.Meal
import com.routina.bite.model.Nutrients

/**
 * 表單正在編輯的一筆紀錄。[basis] 是「每一份」的營養值，表單上顯示的是 basis × [servings]。
 * [servingGrams] 是唯讀的換算基準，表單不改它（要改每份幾公克請去食物編輯頁）。
 */
data class EntryDraft(
    val name: String = "",
    val servings: Double = 1.0,
    val servingGrams: Double? = null,
    val basis: Nutrients = Nutrients.EMPTY,
    val meal: Meal?,
    val note: String = "",
    val foodId: String? = null
)

/** 快速輸入：空白表單 */
fun draftOf(meal: Meal) = EntryDraft(meal = meal)

/** 從食物庫點一個食物：用它的數值把表單填好 */
fun draftOf(food: Food, meal: Meal) = EntryDraft(
    name = food.name,
    servingGrams = food.servingGrams,
    basis = food.nutrients,
    meal = meal,
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
    foodId = entry.foodId
)

/** 份數的真值來自哪裡。沒動過就用帶進來的精確份數，不要拿四捨五入過的欄位文字回推 */
private enum class AmountEdit { NONE, SERVINGS, GRAMS }

/**
 * 新增與編輯紀錄共用的表單。三個入口（快速輸入、點食物、編輯紀錄）的差別只在 [initial]。
 *
 * 連動規則：改份數或公克 → 四個營養欄以 basis × 份數 重算；
 * 直接改某個營養欄 → basis 的那一項改成 輸入值 ÷ 份數。最後動的那個就是使用者要的值。
 */
@Composable
fun EntryFormDialog(
    initial: EntryDraft,
    editing: Boolean,
    onConfirm: (EntryDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val servingGrams = initial.servingGrams?.takeIf { it > 0.0 }
    val quickName = stringResource(R.string.add_quick)

    var name by remember { mutableStateOf(initial.name) }
    var servings by remember { mutableStateOf(formatAmount(initial.servings)) }
    var grams by remember {
        mutableStateOf(servingGrams?.let { formatGrams(initial.servings * it) }.orEmpty())
    }
    var kcal by remember { mutableStateOf(kcalText(initial.basis.kcal, initial.servings)) }
    var protein by remember { mutableStateOf(gramsText(initial.basis.protein, initial.servings)) }
    var fat by remember { mutableStateOf(gramsText(initial.basis.fat, initial.servings)) }
    var carbs by remember { mutableStateOf(gramsText(initial.basis.carbs, initial.servings)) }
    var meal by remember { mutableStateOf(initial.meal) }
    var note by remember { mutableStateOf(initial.note) }
    var basis by remember { mutableStateOf(initial.basis) }
    var edited by remember { mutableStateOf(AmountEdit.NONE) }

    val amount: Double? = when (edited) {
        AmountEdit.NONE -> initial.servings
        AmountEdit.SERVINGS -> servings.toDoubleOrNull()
        // 公克是最後改的那一個，份數就用公克直接除回去算，不然 150 g 會存成 150.6 g
        AmountEdit.GRAMS -> servingGrams?.let { grams.toDoubleOrNull()?.div(it) }
    }

    fun fillNutrients(value: Double?) {
        if (value == null) return
        kcal = kcalText(basis.kcal, value)
        protein = gramsText(basis.protein, value)
        fat = gramsText(basis.fat, value)
        carbs = gramsText(basis.carbs, value)
    }

    // 直接改營養欄：把每份值改成 輸入值 ÷ 份數。份數還不合法就只改文字，等份數修好再算
    fun setBasis(input: String, apply: (Double) -> Nutrients) {
        val entered = if (input.isBlank()) 0.0 else input.toDoubleOrNull() ?: return
        val value = amount ?: return
        if (value <= 0.0) return
        basis = apply(entered / value)
    }

    val valid = amount != null && amount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editing) {
                    stringResource(R.string.entry_edit_title)
                } else {
                    stringResource(R.string.add_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    // 預設值只在存檔時補上，不先塞進欄位讓使用者刪
                    placeholder = { Text(quickName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = servings,
                        onValueChange = { input ->
                            servings = input
                            edited = AmountEdit.SERVINGS
                            val value = input.toDoubleOrNull()
                            if (servingGrams != null) {
                                grams = if (value == null) "" else formatGrams(value * servingGrams)
                            }
                            fillNutrients(value)
                        },
                        label = stringResource(R.string.add_servings),
                        modifier = Modifier.weight(1f)
                    )
                    if (servingGrams != null) {
                        NumberField(
                            value = grams,
                            onValueChange = { input ->
                                grams = input
                                edited = AmountEdit.GRAMS
                                val value = input.toDoubleOrNull()?.div(servingGrams)
                                servings = if (value == null) "" else formatAmount(value)
                                fillNutrients(value)
                            },
                            label = stringResource(R.string.add_grams),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 四個營養欄排成兩欄，表單才不會長到備註要捲兩次才看得到
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = kcal,
                            onValueChange = { input ->
                                kcal = input
                                setBasis(input) { basis.copy(kcal = it) }
                            },
                            label = stringResource(R.string.entry_field_kcal),
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = protein,
                            onValueChange = { input ->
                                protein = input
                                setBasis(input) { basis.copy(protein = it) }
                            },
                            label = stringResource(R.string.entry_field_protein),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = fat,
                            onValueChange = { input ->
                                fat = input
                                setBasis(input) { basis.copy(fat = it) }
                            },
                            label = stringResource(R.string.entry_field_fat),
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = carbs,
                            onValueChange = { input ->
                                carbs = input
                                setBasis(input) { basis.copy(carbs = it) }
                            },
                            label = stringResource(R.string.entry_field_carbs),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = stringResource(R.string.entry_form_hint),
                        style = MaterialTheme.typography.bodySmall.zh(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 本來就未分餐的紀錄才給「未分餐」這個選項；新增一律要選一餐
                MealPicker(
                    selected = meal,
                    onSelect = { meal = it },
                    allowNone = initial.meal == null
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.entry_note)) },
                    placeholder = { Text(stringResource(R.string.entry_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val value = amount ?: return@TextButton
                    onConfirm(
                        initial.copy(
                            name = name.trim().ifEmpty { quickName },
                            servings = value,
                            basis = basis,
                            meal = meal,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text(
                    text = if (editing) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(R.string.action_add)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// 每份值是 0 就讓欄位留白：空白在這個 App 裡本來就當作 0，印一排 0 反而又要先刪字
private fun kcalText(perServing: Double, servings: Double): String =
    if (perServing == 0.0) "" else formatKcal(perServing * servings).toString()

private fun gramsText(perServing: Double, servings: Double): String =
    if (perServing == 0.0) "" else formatGrams(perServing * servings)
