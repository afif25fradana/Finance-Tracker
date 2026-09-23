package com.financetracker.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.ui.components.CashflowChart
import com.financetracker.app.ui.components.CategoryIconTile
import com.financetracker.app.ui.components.EmptyState
import com.financetracker.app.ui.components.TransactionRowItem
import com.financetracker.app.ui.components.TrendLineChart
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermText
import java.util.Locale

@Composable
fun DashboardScreen(
  onAddTransaction: () -> Unit,
  onHistory: () -> Unit,
  onEditTransaction: (Long) -> Unit,
  onManageReminders: () -> Unit,
  onExport: () -> Unit,
  onBackupRestore: () -> Unit
) {
  val context = LocalContext.current
  val viewModel: DashboardViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        DashboardViewModel(
          transactionDao = db.transactionDao(),
          categoryDao = db.categoryDao()
        )
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var balanceVisible by rememberSaveable { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(TermBg),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Finance Tracker",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TermText
          )
          Text(
            text = state.monthLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TermMuted
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HeaderChip(text = "Reminders", onClick = onManageReminders)
          HeaderChip(text = "Export", onClick = onExport)
          HeaderChip(text = "Backup", onClick = onBackupRestore)
        }
      }
    }

    item {
      RunningBalanceCard(
        balanceVisible = balanceVisible,
        onToggleVisibility = { balanceVisible = !balanceVisible },
        state = state,
        onAddTransaction = onAddTransaction,
        onHistory = onHistory
      )
    }

    if (state.cashflow.any { it.income > 0 || it.expense > 0 }) {
      item {
        CashflowChart(data = state.cashflow)
      }
    }

    if (state.categories.isNotEmpty()) {
      item {
        CategoryBreakdownCard(categories = state.categories)
      }
    }

    if (state.trend.any { it.amount > 0 }) {
      item {
        TrendLineChart(data = state.trend)
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Transactions",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = TermText
        )
        Row(
          modifier = Modifier.clickable(onClick = onHistory),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "View all",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TermMuted,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }

    if (state.recent.isEmpty()) {
      item {
        EmptyState(
          message = "No transactions yet",
          cta = "Tap + to add one"
        )
      }
    } else {
      items(state.recent, key = { it.id }) { row ->
        TransactionRowItem(row = row, onEdit = { onEditTransaction(row.id) })
      }
    }
  }
}

@Composable
private fun HeaderChip(text: String, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = TermMuted,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun RunningBalanceCard(
  balanceVisible: Boolean,
  onToggleVisibility: () -> Unit,
  state: DashboardUiState,
  onAddTransaction: () -> Unit,
  onHistory: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Balance",
          style = MaterialTheme.typography.titleSmall,
          color = TermMuted,
          fontWeight = FontWeight.Medium
        )
        IconButton(onClick = onToggleVisibility) {
          Icon(
            imageVector = if (balanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = "Toggle balance visibility",
            tint = TermMuted,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (balanceVisible) formatRupiah(state.balance) else "Rp" + "•".repeat(6),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        color = TermText
      )

      val delta = state.netDeltaPercent
      if (balanceVisible && delta != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = String.format(Locale.US, "%+.1f%%", delta),
            style = MaterialTheme.typography.bodySmall,
            color = if (delta >= 0) SignalPositive else SignalNegative,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "net vs last month",
            style = MaterialTheme.typography.bodySmall,
            color = TermMuted
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(TermBorder)
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Metric(label = "Income", amount = state.monthIncome, isExpense = false)
        Metric(label = "Expenses", amount = state.monthExpense, isExpense = true)
        Metric(label = "Net", amount = state.monthNet, isExpense = state.monthNet < 0)
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .clickable(onClick = onAddTransaction),
          shape = RoundedCornerShape(2.dp),
          color = SignalPositive
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = TermBg, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Add Transaction",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = TermBg
            )
          }
        }
        Surface(
          modifier = Modifier
            .weight(1f)
            .clickable(onClick = onHistory),
          shape = RoundedCornerShape(2.dp),
          color = TermPanel,
          border = BorderStroke(1.dp, TermBorder)
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "History",
              style = MaterialTheme.typography.bodyMedium,
              color = TermMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = TermMuted,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Metric(label: String, amount: Long, isExpense: Boolean) {
  val sign = when {
    amount < 0 -> "-"
    isExpense -> "-"
    else -> "+"
  }
  val color = if (isExpense || amount < 0) SignalNegative else SignalPositive
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TermMuted
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "$sign${formatRupiah(kotlin.math.abs(amount))}",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Bold,
      color = color
    )
  }
}

@Composable
private fun CategoryBreakdownCard(categories: List<CategorySlice>) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Spending by Category",
          style = MaterialTheme.typography.titleSmall,
          color = TermMuted,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Total: ${formatRupiah(categories.sumOf { it.amount })}",
          style = MaterialTheme.typography.bodySmall,
          color = TermText
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        categories.forEach { slice ->
          Box(
            modifier = Modifier
              .weight(slice.fraction.coerceAtLeast(0.01f))
              .height(6.dp)
              .background(Color(slice.color))
          ) {}
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      categories.take(5).forEachIndexed { index, cat ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconTile(
              iconKey = cat.iconKey,
              colorArgb = cat.color,
              containerSize = 18.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${index + 1}. ${cat.name}",
              style = MaterialTheme.typography.bodySmall,
              color = TermText
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatRupiah(cat.amount),
              style = MaterialTheme.typography.bodySmall,
              color = TermText
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = String.format(Locale.US, "%.1f%%", cat.fraction * 100),
              style = MaterialTheme.typography.bodySmall,
              color = TermMuted
            )
          }
        }
      }
    }
  }
}



