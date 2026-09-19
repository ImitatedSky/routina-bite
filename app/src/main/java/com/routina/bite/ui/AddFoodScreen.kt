package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.routina.bite.data.frequentFoods
import com.routina.bite.data.groupByCategory
import com.routina.bite.data.recentFoods
import com.routina.bite.data.searchFoods
import com.routina.bite.model.Food
import com.routina.bite.model.Meal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    viewModel: BiteViewModel,
    date: String,
    initialMeal: Meal,
    onDone: () -> Unit,
    onEditFood: (String) -> Unit,
    onNewFood: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedCategories.collectAsStateWithLifecycle()

    var meal by remember { mutableStateOf(initialMeal) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Food?>(null) }
    var quickAdd by remember { mutableStateOf(false) }

    val frequent = frequentFoods(entries, foods)
    val recent = recentFoods(entries, foods)
    val results = searchFoods(foods, query)
    val groups = remember(foods) { groupByCategory(foods) }

    val editLabel = stringResource(R.string.action_edit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MealPicker(selected = meal, onSelect = { meal = it }) }

            if (frequent.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.add_frequent)) }
                item { FoodChips(frequent) { picked = it } }
            }
            if (recent.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.add_recent)) }
                item { FoodChips(recent) { picked = it } }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.add_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { quickAdd = true }) {
                        Text(stringResource(R.string.add_quick))
                    }
                    OutlinedButton(onClick = onNewFood) {
                        Text(stringResource(R.string.add_new_food))
                    }
                    OutlinedButton(onClick = onOpenLibrary) {
                        Text(stringResource(R.string.nav_library))
                    }
                }
            }

            item {
                SectionTitle(
                    if (query.isBlank()) {
                        stringResource(R.string.add_all_foods)
                    } else {
                        stringResource(R.string.add_results)
                    }
                )
            }

            if (foods.isEmpty()) {
                item { EmptyHint(stringResource(R.string.add_library_empty)) }
            } else if (query.isBlank()) {
                // 沒搜尋就依分類分組，摺疊狀態與食物庫頁共用
                foodGroups(
                    groups = groups,
                    expanded = expanded,
                    onToggle = { category ->
                        viewModel.setCategoryExpanded(category, category !in expanded)
                    },
                    onFoodClick = { picked = it },
                    rowMenu = { food -> listOf(editLabel to { onEditFood(food.id) }) }
                )
            } else if (results.isEmpty()) {
                item { EmptyHint(stringResource(R.string.add_no_results)) }
            } else {
                items(results, key = { it.id }) { food ->
                    FoodRow(
                        food = food,
                        onClick = { picked = food },
                        menu = listOf(editLabel to { onEditFood(food.id) })
                    )
                }
            }
        }
    }

    picked?.let { food ->
        EntryFormDialog(
            initial = draftOf(food, meal),
            editing = false,
            onConfirm = { draft ->
                viewModel.addEntry(date, draft)
                picked = null
                onDone()
            },
            onDismiss = { picked = null }
        )
    }

    if (quickAdd) {
        EntryFormDialog(
            initial = draftOf(meal),
            editing = false,
            onConfirm = { draft ->
                viewModel.addEntry(date, draft)
                quickAdd = false
                onDone()
            },
            onDismiss = { quickAdd = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun FoodChips(foods: List<Food>, onClick: (Food) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(foods, key = { it.id }) { food ->
            AssistChip(onClick = { onClick(food) }, label = { Text(food.name) })
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
