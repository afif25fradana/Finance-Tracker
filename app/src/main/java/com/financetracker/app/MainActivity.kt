package com.financetracker.app

import androidx.compose.runtime.DisposableEffect
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.compose.foundation.layout.Box
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financetracker.app.ui.backup.BackupRestoreRoute
import com.financetracker.app.ui.categories.CategoriesRoute
import com.financetracker.app.ui.dashboard.DashboardScreen
import com.financetracker.app.ui.entry.AddEditTransactionRoute
import com.financetracker.app.ui.export.ExportRoute
import com.financetracker.app.ui.history.HistoryScreen
import com.financetracker.app.ui.recurring.RecurringRoute
import com.financetracker.app.ui.theme.FinanceTrackerTheme
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermPanelAlt


@Serializable object DashboardRoute
@Serializable object HistoryRoute
@Serializable object AddTransactionRoute
@Serializable data class EditTransactionRoute(val transactionId: Long)
@Serializable object CategoriesRoute
@Serializable object RecurringRoute
@Serializable object ExportRoute
@Serializable object BackupRestoreRoute

private enum class TopTab(
  val route: Any,
  val routeClass: KClass<out Any>,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  DASHBOARD(
    route = DashboardRoute,
    routeClass = DashboardRoute::class,
    label = "Dashboard",
    selectedIcon = Icons.Filled.Dashboard,
    unselectedIcon = Icons.Outlined.Dashboard,
    testTag = "nav_dashboard"
  ),
  ADD(
    route = AddTransactionRoute,
    routeClass = AddTransactionRoute::class,
    label = "Add",
    selectedIcon = Icons.Filled.AddCircle,
    unselectedIcon = Icons.Outlined.AddCircleOutline,
    testTag = "nav_add"
  ),
  HISTORY(
    route = HistoryRoute,
    routeClass = HistoryRoute::class,
    label = "History",
    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_history"
  ),
  CATEGORIES(
    route = CategoriesRoute,
    routeClass = CategoriesRoute::class,
    label = "Categories",
    selectedIcon = Icons.Filled.PieChart,
    unselectedIcon = Icons.Outlined.PieChart,
    testTag = "nav_categories"
  )
}

private fun NavHostController.navigateToTab(route: Any) {
  if (currentDestination?.hasRoute(AddTransactionRoute::class) == true && route !is AddTransactionRoute) {
    popBackStack()
  }
  navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

class MainActivity : ComponentActivity() {
  @VisibleForTesting
  internal var navController: NavHostController? = null
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FinanceTrackerTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val navController = rememberNavController()
          DisposableEffect(navController) { this@MainActivity.navController = navController; onDispose { this@MainActivity.navController = null } }
          val backStackEntry by navController.currentBackStackEntryAsState()
          val currentRoute = backStackEntry?.destination?.route
          val showBottomBar = TopTab.entries.any { tab -> backStackEntry?.destination?.hasRoute(tab.routeClass) == true }

          Scaffold(
            containerColor = TermBg,
            bottomBar = {
              if (showBottomBar) {
                NavigationBar(
                  containerColor = TermPanel,
                  tonalElevation = 0.dp,
                  modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                  TopTab.entries.forEach { tab ->
                    val selected = backStackEntry?.destination?.hasRoute(tab.routeClass) == true
                    NavigationBarItem(
                      selected = selected,
                      onClick = { navController.navigateToTab(tab.route) },
                      icon = {
                        Icon(
                          imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                          contentDescription = null,
                          modifier = Modifier.size(20.dp)
                        )
                      },
                      label = {
                        Text(
                          text = tab.label,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                      },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TermPanelAlt,
                        selectedIconColor = SignalPositive,
                        selectedTextColor = SignalPositive,
                        unselectedIconColor = TermMuted,
                        unselectedTextColor = TermMuted
                      ),
                      modifier = Modifier.testTag(tab.testTag)
                    )
                  }
                }
              }
            }
          ) { innerPadding ->
            NavHost(
              navController = navController,
              startDestination = DashboardRoute,
              modifier = Modifier.fillMaxSize()
            ) {
              composable<DashboardRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  DashboardScreen(
                    onAddTransaction = { navController.navigateToTab(AddTransactionRoute) },
                    onHistory = { navController.navigateToTab(HistoryRoute) },
                    onEditTransaction = { id -> navController.navigate(EditTransactionRoute(id)) },
                    onManageReminders = { navController.navigate(RecurringRoute) },
                    onExport = { navController.navigate(ExportRoute) },
                    onBackupRestore = { navController.navigate(BackupRestoreRoute) }
                  )
                }
              }
              composable<HistoryRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  HistoryScreen(
                    onEditTransaction = { id -> navController.navigate(EditTransactionRoute(id)) }
                  )
                }
              }
              composable<AddTransactionRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  AddEditTransactionRoute(
                    transactionId = null,
                    onBack = { navController.popBackStack() }
                  )
                }
              }
              composable<EditTransactionRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditTransactionRoute>()
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  AddEditTransactionRoute(
                    transactionId = route.transactionId,
                    onBack = { navController.popBackStack() }
                  )
                }
              }
              composable<CategoriesRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  CategoriesRoute()
                }
              }
              composable<RecurringRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  RecurringRoute(onBack = { navController.popBackStack() })
                }
              }
              composable<ExportRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  ExportRoute(onBack = { navController.popBackStack() })
                }
              }
              composable<BackupRestoreRoute> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                  BackupRestoreRoute(onBack = { navController.popBackStack() })
                }
              }
            }
          }
        }
      }
    }
  }
}
