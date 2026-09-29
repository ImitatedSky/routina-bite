package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routina.bite.R

/**
 * 「一份東西的營養標示長什麼樣」——紀錄表單與食物編輯頁共用的欄位。
 *
 * 兩邊填的是同一張標示，差別只在數字代表什麼：紀錄表單顯示這一筆的總量、
 * 食物編輯頁顯示每一份。所以欄位順序、標籤、摺疊行為都放在這裡共用，
 * 由呼叫端各自傳自己的 [hint] 說明那個差別。
 *
 * 之前兩邊各寫一份，結果是紀錄表單只有四個營養欄、食物編輯頁有九個，
 * 連「三大營養素自動算熱量」都實作了兩次。
 */

/** 三大營養素換算熱量：蛋白質與碳水 4 kcal/g、脂肪 9 kcal/g */
fun kcalFromMacros(protein: Double, fat: Double, carbs: Double): Double =
    protein * 4 + fat * 9 + carbs * 4

/** 一個營養欄現在的文字與改動的入口 */
data class NutrientField(val value: String, val onChange: (String) -> Unit)

/**
 * 九個營養欄。常用的四個一直在，其餘五個收在「更多營養標示」底下——
 * 每天記一餐要填的就是那四個，九個全攤開會讓表單長到備註要捲兩次才看得到。
 *
 * 表單一開起來就有值的話預設展開：抄過鈉含量的食物，打開編輯頁不該看不到它。
 * 之後不再自動開合——使用者正在清空某一欄時把整區收起來，比少看到一欄更煩。
 */
@Composable
fun NutrientFields(
    kcal: NutrientField,
    protein: NutrientField,
    fat: NutrientField,
    carbs: NutrientField,
    satFat: NutrientField,
    transFat: NutrientField,
    sugar: NutrientField,
    sodium: NutrientField,
    cholesterol: NutrientField,
    hint: String,
    modifier: Modifier = Modifier,
    kcalError: Boolean = false,
    kcalErrorText: String? = null
) {
    val extras = listOf(satFat, transFat, sugar, sodium, cholesterol)
    var expanded by remember { mutableStateOf(extras.any { it.value.isNotBlank() }) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = kcal.value,
                onValueChange = kcal.onChange,
                label = stringResource(R.string.nutrient_kcal),
                isError = kcalError,
                errorText = kcalErrorText,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = protein.value,
                onValueChange = protein.onChange,
                label = stringResource(R.string.nutrient_protein),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = fat.value,
                onValueChange = fat.onChange,
                label = stringResource(R.string.nutrient_fat),
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = carbs.value,
                onValueChange = carbs.onChange,
                label = stringResource(R.string.nutrient_carbs),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall.zh(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
            Text(
                text = stringResource(
                    if (expanded) R.string.nutrient_less else R.string.nutrient_more
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = satFat.value,
                    onValueChange = satFat.onChange,
                    label = stringResource(R.string.nutrient_sat_fat),
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = transFat.value,
                    onValueChange = transFat.onChange,
                    label = stringResource(R.string.nutrient_trans_fat),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = sugar.value,
                    onValueChange = sugar.onChange,
                    label = stringResource(R.string.nutrient_sugar),
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = sodium.value,
                    onValueChange = sodium.onChange,
                    label = stringResource(R.string.nutrient_sodium),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = cholesterol.value,
                    onValueChange = cholesterol.onChange,
                    label = stringResource(R.string.nutrient_cholesterol),
                    modifier = Modifier.weight(1f)
                )
                // 只有一欄也要佔滿另外半邊，不然膽固醇會變成一個孤零零的全寬欄位
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * 分類：自己打字，或直接點既有的分類。
 * 給 chip 是因為分類沒有獨立的實體，就是食物身上那個字串——打錯一個字就會多出一組。
 */
@Composable
fun CategoryField(
    value: String,
    onValueChange: (String) -> Unit,
    categories: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.field_category)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { name ->
                    FilterChip(
                        selected = name == value,
                        onClick = { onValueChange(if (name == value) "" else name) },
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

/** 常用：開了就排在食物庫前面 */
@Composable
fun FavoriteSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.field_favorite),
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
