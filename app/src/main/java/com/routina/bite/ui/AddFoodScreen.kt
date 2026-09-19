package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch

/**
 * 新增紀錄頁。表單直接長在頁面上：按了「新增」就可以開始打字，
 * 下面的食物清單只是幫忙把表單填好，點一個食物＝用它的數值重填上面的表單。
 */
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

    val state = rememberEntryFormState(draftOf(initialMeal))
    var query by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val frequent = frequentFoods(entries, foods)
    val recent = recentFoods(entries, foods)
    val results = searchFoods(foods, query)
    val groups = remember(foods) { groupByCategory(foods) }

    val editLabel = stringResource(R.string.action_edit)
    val quickName = stringResource(R.string.add_quick)

    // 選了照片又直接返回上一頁：那張檔案沒有紀錄會用到，留著就是孤兒檔
    DisposableEffect(Unit) {
        onDispose { state.dropNewPhotos(viewModel) }
    }

    fun fillFrom(food: Food) {
        state.dropNewPhotos(viewModel)
        state.loadFrom(draftOf(food, state.meal))
        // 表單在最上面，捲回去才看得到剛剛填好的數字
        scope.launch { listState.animateScrollToItem(0) }
    }

    fun add() {
        val draft = state.toDraft(quickName) ?: return
        state.commitPhotos(viewModel)
        viewModel.addEntry(date, draft)
        onDone()
    }

    fun clear() {
        state.dropNewPhotos(viewModel)
        state.reset()
    }

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
        },
        bottomBar = {
            // 「加入」永遠看得到，選完食物不必再捲回去找按鈕
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { clear() }) {
                        Text(stringResource(R.string.add_clear))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(enabled = state.valid, onClick = { add() }) {
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MealPicker(
                        selected = state.meal,
                        onSelect = { picked -> picked?.let { state.meal = it } }
                    )
                    EntryFormFields(state = state, viewModel = viewModel, showMeal = false)
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onNewFood) {
                        Text(stringResource(R.string.add_new_food))
                    }
                    TextButton(onClick = onOpenLibrary) {
                        Text(stringResource(R.string.nav_library))
                    }
                }
            }

            if (frequent.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.add_frequent)) }
                item { FoodChips(frequent) { fillFrom(it) } }
            }
            if (recent.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.add_recent)) }
                item { FoodChips(recent) { fillFrom(it) } }
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
                    onFoodClick = { fillFrom(it) },
                    rowMenu = { food -> listOf(editLabel to { onEditFood(food.id) }) }
                )
            } else if (results.isEmpty()) {
                item { EmptyHint(stringResource(R.string.add_no_results)) }
            } else {
                items(results, key = { it.id }) { food ->
                    FoodRow(
                        food = food,
                        onClick = { fillFrom(food) },
                        menu = listOf(editLabel to { onEditFood(food.id) })
                    )
                }
            }
        }
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
