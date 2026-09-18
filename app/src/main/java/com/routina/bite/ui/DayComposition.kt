package com.routina.bite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routina.bite.R
import com.routina.bite.model.DiaryEntry
import com.routina.bite.model.Meal
import com.routina.bite.ui.theme.MacroColors
import kotlin.math.roundToInt

/** 一段堆疊條：顏色、名稱、熱量 */
private data class Slice(val color: Color, val label: String, val kcal: Double)

/**
 * 當日組成：這一天的熱量從哪三種營養素來、又分在哪幾餐。
 *
 * 與「趨勢」頁不同，這裡只看選定的那一天，回答的是「今天吃得均不均衡、有沒有集中在某一餐」。
 * 沒有紀錄的日子整張卡不顯示。
 */
@Composable
fun DayCompositionCard(entries: List<DiaryEntry>) {
    if (entries.isEmpty()) return

    val macroSlices = macroSlices(entries)
    val mealSlices = mealSlices(entries)

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 組間 16dp、組內 8dp：間距本身就要說得出哪些東西是一組的
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.composition_title),
                style = MaterialTheme.typography.titleSmall.zh(),
                color = MaterialTheme.colorScheme.onSurface
            )

            CompositionBlock(
                title = stringResource(R.string.composition_macro_title),
                slices = macroSlices,
                emptyText = stringResource(R.string.composition_no_macros)
            )

            // 只有一餐有東西時這條說不出任何事（匯入的舊紀錄就全是「未分餐」），直接不顯示
            if (mealSlices.count { it.kcal > 0.0 } >= 2) {
                CompositionBlock(
                    title = stringResource(R.string.composition_meal_title),
                    slices = mealSlices,
                    emptyText = null
                )
            }
        }
    }
}

@Composable
private fun CompositionBlock(title: String, slices: List<Slice>, emptyText: String?) {
    val total = slices.sumOf { it.kcal }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.zh(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (total <= 0.0) {
            if (emptyText != null) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall.zh(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }
        StackedBar(slices, total)
        slices.filter { it.kcal > 0.0 }.forEach { slice ->
            LegendRow(slice, total)
        }
    }
}

@Composable
private fun StackedBar(slices: List<Slice>, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        slices.filter { it.kcal > 0.0 }.forEach { slice ->
            Box(
                modifier = Modifier
                    .weight((slice.kcal / total).toFloat())
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(slice.color)
            )
        }
    }
}

@Composable
private fun LegendRow(slice: Slice, total: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(slice.color)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium.zh(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(
                R.string.composition_value,
                formatKcal(slice.kcal),
                percentOf(slice.kcal, total)
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 三大營養素換算成熱量：蛋白質與碳水 4 kcal/g、脂肪 9 kcal/g。
 *
 * 換算出來的總和通常與標示的熱量有出入（標示值本來就不是這樣算的），
 * 所以百分比一律以這三者的合計為分母，標題也寫明是換算來的。
 */
@Composable
private fun macroSlices(entries: List<DiaryEntry>): List<Slice> {
    val total = entries.fold(com.routina.bite.model.Nutrients()) { sum, entry -> sum + entry.total }
    return listOf(
        Slice(MacroColors.Protein, stringResource(R.string.macro_protein), total.protein * 4),
        Slice(MacroColors.Fat, stringResource(R.string.macro_fat), total.fat * 9),
        Slice(MacroColors.Carbs, stringResource(R.string.macro_carbs), total.carbs * 4)
    )
}

/**
 * 各餐的熱量。用主色的深淺排一條漸層而不是四個不同色相——
 * 餐別本來就有先後順序，而且這裡不需要再多四個顏色來記。
 */
@Composable
private fun mealSlices(entries: List<DiaryEntry>): List<Slice> {
    val primary = MaterialTheme.colorScheme.primary
    val order = listOf(Meal.BREAKFAST, Meal.LUNCH, Meal.DINNER, Meal.SNACK, null)
    val alphas = listOf(0.85f, 0.65f, 0.48f, 0.32f, 0.18f)
    return order.mapIndexed { index, meal ->
        val kcal = entries.filter { it.meal == meal }.sumOf { it.total.kcal }
        Slice(primary.copy(alpha = alphas[index]), mealLabel(meal), kcal)
    }
}

private fun percentOf(value: Double, total: Double): Int =
    if (total <= 0.0) 0 else (value / total * 100).roundToInt()
