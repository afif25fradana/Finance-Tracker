package com.financetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

private enum class TopTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  DASHBOARD(
    route = "dashboard",
    label = "Dashboard",
    selectedIcon = Icons.Filled.Dashboard,
    unselectedIcon = Icons.Outlined.Dashboard,
    testTag = "nav_dashboard"
  ),
  ADD(
    route = "add",
    label = "Add",
    selectedIcon = Icons.Filled.AddCircle,
    unselectedIcon = Icons.Outlined.AddCircleOutline,
    testTag = "nav_add"
  ),
  HISTORY(
    route = "history",
    label = "History",
    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_history"
  ),
  CATEGORIES(
    route = "categories",
    label = "Categories",
    selectedIcon = Icons.Filled.PieChart,
    unselectedIcon = Icons.Outlined.PieChart,
    testTag = "nav_categories"
  )
}

private fun NavHostController.navigateToTab(route: String) {
  if (currentDestination?.route == TopTab.ADD.route && route != TopTab.ADD.route) {
    popBackStack()
  }
  navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

class MainActivity : ComponentActivity() {
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
          val backStackEntry by navController.currentBackStackEntryAsState()
          val currentRoute = backStackEntry?.destination?.route
          val showBottomBar = TopTab.entries.any { it.route == currentRoute }

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
                    val selected = currentRoute == tab.route
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
              startDestination = TopTab.DASHBOARD.route,
              modifier = Modifier.padding(innerPadding)
            ) {
              composable(TopTab.DASHBOARD.route) {
                DashboardScreen(
                  onAddTransaction = { navController.navigateToTab(TopTab.ADD.route) },
                  onHistory = { navController.navigateToTab(TopTab.HISTORY.route) },
                  onEditTransaction = { id -> navController.navigate("add_transaction/$id") },
                  onManageReminders = { navController.navigate("recurring") },
                  onExport = { navController.navigate("export") },
                  onBackupRestore = { navController.navigate("backup_restore") }
                )
              }
              composable(TopTab.HISTORY.route) {
                HistoryScreen(
                  onEditTransaction = { id -> navController.navigate("add_transaction/$id") }
                )
              }
              composable(TopTab.ADD.route) {
                AddEditTransactionRoute(
                  transactionId = null,
                  onBack = { navController.popBackStack() }
                )
              }
              composable(
                route = "add_transaction/{transactionId}",
                arguments = listOf(
                  navArgument("transactionId") { type = NavType.LongType }
                )
              ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId")
                AddEditTransactionRoute(
                  transactionId = transactionId,
                  onBack = { navController.popBackStack() }
                )
              }
              composable(TopTab.CATEGORIES.route) {
                CategoriesRoute()
              }
              composable("recurring") {
                RecurringRoute(onBack = { navController.popBackStack() })
              }
              composable("export") {
                ExportRoute(onBack = { navController.popBackStack() })
              }
              composable("backup_restore") {
                BackupRestoreRoute(onBack = { navController.popBackStack() })
              }
            }
          }
        }
      }
    }
  }
}
