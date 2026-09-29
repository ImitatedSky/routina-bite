package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routina.bite.R
import com.routina.bite.data.allCategories
import com.routina.bite.model.Food
import com.routina.bite.model.Nutrients

/**
 * 食物庫的編輯頁。
 *
 * 欄位與紀錄表單共用 [NutrientFields]／[CategoryField]／[FavoriteSwitch]——兩邊填的是同一張
 * 營養標示，差別只在這裡的數字是「每一份」，紀錄表單的是「這一筆的總量」，
 * 所以只有提示文字不一樣。這頁不必乘份數，換算熱量就是直接加總。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    viewModel: BiteViewModel,
    foodId: String?,
    onDone: () -> Unit
) {
    val existing = remember(foodId) { foodId?.let { viewModel.findFood(it) } }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var servingGrams by remember {
        mutableStateOf(existing?.servingGrams?.let { formatGrams(it) }.orEmpty())
    }
    var kcal by remember { mutableStateOf(existing?.nutrients?.kcal?.let { formatKcalExact(it) }.orEmpty()) }
    var protein by remember { mutableStateOf(existing.gramsOf { it.protein }) }
    var fat by remember { mutableStateOf(existing.gramsOf { it.fat }) }
    var satFat by remember { mutableStateOf(existing.gramsOf { it.satFat }) }
    var transFat by remember { mutableStateOf(existing.gramsOf { it.transFat }) }
    var carbs by remember { mutableStateOf(existing.gramsOf { it.carbs }) }
    var sugar by remember { mutableStateOf(existing.gramsOf { it.sugar }) }
    var sodium by remember { mutableStateOf(existing.gramsOf { it.sodium }) }
    var cholesterol by remember { mutableStateOf(existing.gramsOf { it.cholesterol }) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var favorite by remember { mutableStateOf(existing?.favorite ?: false) }

    // 熱量是使用者自己填的（或既有食物本來就有的標示值），別被三大營養素的換算蓋掉。
    // 與紀錄表單同一條規則，見 add-macro-widget 的 D64/D65。
    var kcalIsManual by remember { mutableStateOf((existing?.nutrients?.kcal ?: 0.0) > 0.0) }

    fun recalcKcal() {
        if (kcalIsManual) return
        val sum = kcalFromMacros(
            protein.toDoubleOrNull() ?: 0.0,
            fat.toDoubleOrNull() ?: 0.0,
            carbs.toDoubleOrNull() ?: 0.0
        )
        kcal = if (sum <= 0.0) "" else formatKcalExact(sum)
    }

    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val categories = allCategories(foods)

    var showErrors by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val nameError = name.isBlank()
    val kcalError = kcal.toDoubleOrNull() == null
    val deleteLabel = stringResource(R.string.action_delete)

    fun save() {
        showErrors = true
        if (nameError || kcalError) return
        viewModel.saveFood(
            Food(
                id = existing?.id ?: viewModel.newFoodId(),
                name = name.trim(),
                servingGrams = servingGrams.toDoubleOrNull()?.takeIf { it > 0.0 },
                nutrients = Nutrients(
                    kcal = kcal.toDoubleOrNull() ?: 0.0,
                    protein = protein.toDoubleOrNull() ?: 0.0,
                    fat = fat.toDoubleOrNull() ?: 0.0,
                    satFat = satFat.toDoubleOrNull() ?: 0.0,
                    transFat = transFat.toDoubleOrNull() ?: 0.0,
                    carbs = carbs.toDoubleOrNull() ?: 0.0,
                    sugar = sugar.toDoubleOrNull() ?: 0.0,
                    sodium = sodium.toDoubleOrNull() ?: 0.0,
                    cholesterol = cholesterol.toDoubleOrNull() ?: 0.0
                ),
                note = note.trim(),
                category = category.trim(),
                favorite = favorite,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existing == null) R.string.food_new_title else R.string.food_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { save() }) { Text(stringResource(R.string.action_save)) }
                    if (existing != null) {
                        OverflowMenu(listOf(deleteLabel to { deleting = true }))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                isError = showErrors && nameError,
                supportingText = if (showErrors && nameError) {
                    { Text(stringResource(R.string.error_name_required), color = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(
                value = servingGrams,
                onValueChange = { servingGrams = it },
                label = stringResource(R.string.field_serving_grams),
                modifier = Modifier.fillMaxWidth()
            )

            NutrientFields(
                kcal = NutrientField(kcal) {
                    kcal = it
                    // 清空熱量欄＝交還自動換算，與紀錄表單一致
                    kcalIsManual = it.isNotBlank()
                    if (!kcalIsManual) recalcKcal()
                },
                protein = NutrientField(protein) { protein = it; recalcKcal() },
                fat = NutrientField(fat) { fat = it; recalcKcal() },
                carbs = NutrientField(carbs) { carbs = it; recalcKcal() },
                satFat = NutrientField(satFat) { satFat = it },
                transFat = NutrientField(transFat) { transFat = it },
                sugar = NutrientField(sugar) { sugar = it },
                sodium = NutrientField(sodium) { sodium = it },
                cholesterol = NutrientField(cholesterol) { cholesterol = it },
                hint = stringResource(R.string.food_blank_hint),
                kcalError = showErrors && kcalError,
                kcalErrorText = stringResource(R.string.error_kcal_required)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.field_note)) },
                modifier = Modifier.fillMaxWidth()
            )
            CategoryField(
                value = category,
                onValueChange = { category = it },
                categories = categories
            )
            FavoriteSwitch(checked = favorite, onCheckedChange = { favorite = it })
        }
    }

    if (deleting && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.food_delete_title),
            message = stringResource(R.string.food_delete_message),
            onConfirm = {
                viewModel.deleteFood(existing.id)
                onDone()
            },
            onDismiss = { deleting = false }
        )
    }
}

/** 空白欄位就是 0，所以 0 顯示成空字串，使用者不必先刪掉一堆 0 */
private fun Food?.gramsOf(pick: (Nutrients) -> Double): String {
    val value = this?.nutrients?.let(pick) ?: return ""
    return if (value == 0.0) "" else formatGrams(value)
}
