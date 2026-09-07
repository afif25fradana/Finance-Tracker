package com.financetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.financetracker.app.ui.dashboard.DashboardScreen
import com.financetracker.app.ui.entry.AddEditTransactionRoute
import com.financetracker.app.ui.history.HistoryScreen
import com.financetracker.app.ui.theme.FinanceTrackerTheme

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
          NavHost(
            navController = navController,
            startDestination = "dashboard"
          ) {
            composable("dashboard") {
              DashboardScreen(
                onAddTransaction = { navController.navigate("add_transaction") },
                onHistory = { navController.navigate("history") },
                onEditTransaction = { id -> navController.navigate("add_transaction/$id") }
              )
            }
            composable("history") {
              HistoryScreen(
                onAddTransaction = { navController.navigate("add_transaction") },
                onEditTransaction = { id -> navController.navigate("add_transaction/$id") }
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
            composable("add_transaction") {
              AddEditTransactionRoute(
                transactionId = null,
                onBack = { navController.popBackStack() }
              )
            }
          }
        }
      }
    }
  }
}
