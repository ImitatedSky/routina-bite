package com.routina.bite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routina.bite.data.defaultMeal
import com.routina.bite.data.todayDate
import com.routina.bite.model.Meal
import com.routina.bite.ui.AddFoodScreen
import com.routina.bite.ui.BiteViewModel
import com.routina.bite.ui.ChartTab
import com.routina.bite.ui.ChartsScreen
import com.routina.bite.ui.FoodEditScreen
import com.routina.bite.ui.FoodLibraryScreen
import com.routina.bite.ui.HistoryScreen
import com.routina.bite.ui.SettingsScreen
import com.routina.bite.ui.TodayScreen
import com.routina.bite.ui.WeightScreen
import com.routina.bite.ui.theme.BiteTheme

class MainActivity : ComponentActivity() {

    // 被能力跳板帶到前景時 +1，畫面看到值變了就回到今日頁
    private val openTodayRequests = mutableIntStateOf(0)

    // 桌面捷徑「記一餐」按下時 +1，畫面看到值變了就開新增紀錄頁
    private val openAddRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 冷啟動也要讀一次：桌面捷徑常常是在 App 沒開著的時候按的，
        // 那一次只會進 onCreate，不會進 onNewIntent
        readRequests(intent)
        setContent {
            BiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiteNavHost(openTodayRequests.intValue, openAddRequests.intValue)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readRequests(intent)
    }

    /** 旗標讀完就從 intent 上拿掉，不然轉螢幕重建時會照著同一個 intent 再跳一次 */
    private fun readRequests(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            intent.removeExtra(EXTRA_OPEN_TODAY)
            openTodayRequests.intValue += 1
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD, false)) {
            intent.removeExtra(EXTRA_OPEN_ADD)
            openAddRequests.intValue += 1
        }
    }

    companion object {
        /** 跳板用來要求「回到今日頁」，一般啟動不會帶 */
        const val EXTRA_OPEN_TODAY = "com.routina.bite.extra.OPEN_TODAY"

        /** 桌面捷徑用來要求「開今天的新增紀錄頁」，一般啟動不會帶 */
        const val EXTRA_OPEN_ADD = "com.routina.bite.extra.OPEN_ADD"
    }
}

private object Routes {
    const val TODAY = "today"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val WEIGHTS = "weights"
    const val SETTINGS = "settings"
    const val FOOD_NEW = "food/new"

    // charts?tab=kcal|weight|macros，省略時落在熱量分頁
    const val CHARTS = "charts?tab={tab}"

    // add/{date}/{meal}：meal 帶 Meal 的名稱，例如 add/2025-09-18/LUNCH
    const val ADD = "add/{date}/{meal}"
    const val FOOD_EDIT = "food/{foodId}"

    const val ARG_DATE = "date"
    const val ARG_MEAL = "meal"
    const val ARG_FOOD_ID = "foodId"
    const val ARG_TAB = "tab"

    fun add(date: String, meal: Meal) = "add/$date/${meal.name}"
    fun food(foodId: String) = "food/$foodId"
    fun charts(tab: ChartTab) = "charts?tab=${tab.name.lowercase()}"
}

@Composable
private fun BiteNavHost(openTodayRequests: Int, openAddRequests: Int) {
    val navController = rememberNavController()
    val viewModel: BiteViewModel = viewModel()

    // 被別的家族成員呼叫 open_today 時，把畫面帶回今日頁
    LaunchedEffect(openTodayRequests) {
        if (openTodayRequests > 0) {
            viewModel.backToToday()
            navController.popBackStack(Routes.TODAY, inclusive = false)
        }
    }

    // 桌面捷徑「記一餐」：先回到今日頁再疊新增紀錄頁，返回鍵才會停在今日頁。
    // 日期用今天、餐別依當下時間 —— 捷徑的語意就是「現在」
    LaunchedEffect(openAddRequests) {
        if (openAddRequests > 0) {
            viewModel.backToToday()
            navController.popBackStack(Routes.TODAY, inclusive = false)
            navController.navigate(Routes.add(todayDate(), defaultMeal()))
        }
    }

    NavHost(navController = navController, startDestination = Routes.TODAY) {

        composable(Routes.TODAY) {
            TodayScreen(
                viewModel = viewModel,
                onAdd = { date, meal -> navController.navigate(Routes.add(date, meal)) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenWeights = { navController.navigate(Routes.WEIGHTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.ADD,
            arguments = listOf(
                navArgument(Routes.ARG_DATE) { type = NavType.StringType },
                navArgument(Routes.ARG_MEAL) { type = NavType.StringType }
            )
        ) { entry ->
            val date = entry.arguments?.getString(Routes.ARG_DATE).orEmpty()
            val meal = entry.arguments?.getString(Routes.ARG_MEAL)
                ?.let { name -> Meal.entries.firstOrNull { it.name == name } }
                ?: defaultMeal()
            AddFoodScreen(
                viewModel = viewModel,
                date = date,
                initialMeal = meal,
                onDone = { navController.popBackStack() },
                onEditFood = { foodId -> navController.navigate(Routes.food(foodId)) },
                onNewFood = { navController.navigate(Routes.FOOD_NEW) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) }
            )
        }

        composable(Routes.LIBRARY) {
            FoodLibraryScreen(
                viewModel = viewModel,
                onEditFood = { foodId -> navController.navigate(Routes.food(foodId)) },
                onNewFood = { navController.navigate(Routes.FOOD_NEW) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FOOD_NEW) {
            FoodEditScreen(
                viewModel = viewModel,
                foodId = null,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FOOD_EDIT,
            arguments = listOf(navArgument(Routes.ARG_FOOD_ID) { type = NavType.StringType })
        ) { entry ->
            FoodEditScreen(
                viewModel = viewModel,
                foodId = entry.arguments?.getString(Routes.ARG_FOOD_ID),
                onDone = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onOpenDate = { date ->
                    viewModel.selectDate(date)
                    navController.popBackStack()
                },
                onOpenCharts = { navController.navigate(Routes.charts(ChartTab.KCAL)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.WEIGHTS) {
            WeightScreen(
                viewModel = viewModel,
                onOpenCharts = { navController.navigate(Routes.charts(ChartTab.WEIGHT)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHARTS,
            arguments = listOf(
                navArgument(Routes.ARG_TAB) {
                    type = NavType.StringType
                    defaultValue = ChartTab.KCAL.name.lowercase()
                }
            )
        ) { entry ->
            val tab = entry.arguments?.getString(Routes.ARG_TAB)
                ?.let { name -> ChartTab.entries.firstOrNull { it.name.lowercase() == name } }
                ?: ChartTab.KCAL
            ChartsScreen(
                viewModel = viewModel,
                initialTab = tab,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
