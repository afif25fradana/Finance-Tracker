package com.financetracker.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.TransactionRow
import com.financetracker.app.ui.components.epochDayToMonthLabel
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMM")

data class CashflowPoint(
  val label: String,
  val income: Long,
  val expense: Long
)

data class CategorySlice(
  val name: String,
  val color: Long,
  val iconKey: String,
  val amount: Long,
  val fraction: Float
)

data class DashboardUiState(
  val monthLabel: String = "",
  val balance: Long = 0,
  val monthIncome: Long = 0,
  val monthExpense: Long = 0,
  val monthNet: Long = 0,
  val netDeltaPercent: Float? = null,
  val cashflow: List<CashflowPoint> = emptyList(),
  val categories: List<CategorySlice> = emptyList(),
  val recent: List<TransactionRow> = emptyList()
)

class DashboardViewModel(
  transactionDao: TransactionDao,
  categoryDao: CategoryDao
) : ViewModel() {

  val uiState: StateFlow<DashboardUiState> =
    combine(transactionDao.getAll(), categoryDao.getAll()) { transactions, categories ->
      val byId = categories.associateBy { it.id }
      val today = LocalDate.ofEpochDay(todayEpochDay())
      val thisMonth = YearMonth.from(today)
      val months = (4 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
      val lastMonth = thisMonth.minusMonths(1)

      val balance =
        transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }

      fun sumIn(month: YearMonth, type: TransactionType): Long =
        transactions
          .filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
          .filter { it.type == type }
          .sumOf { it.amount }

      val monthIncome = sumIn(thisMonth, TransactionType.INCOME)
      val monthExpense = sumIn(thisMonth, TransactionType.EXPENSE)
      val prevNet = sumIn(lastMonth, TransactionType.INCOME) - sumIn(lastMonth, TransactionType.EXPENSE)
      val monthNet = monthIncome - monthExpense

      val delta = if (prevNet != 0L) {
        ((monthNet - prevNet).toFloat() / abs(prevNet).toFloat()) * 100f
      } else null

      val cashflow = months.map { m ->
        CashflowPoint(
          label = m.atDay(1).format(MONTH_LABEL),
          income = sumIn(m, TransactionType.INCOME),
          expense = sumIn(m, TransactionType.EXPENSE)
        )
      }

      val monthExpenses = transactions
        .filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == thisMonth }
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .mapValues { (id, list) -> Pair(byId[id], list.sumOf { it.amount }) }
        .values
        .filter { it.first != null && it.second > 0 }
        .sortedByDescending { it.second }

      val totalExpense = monthExpenses.sumOf { it.second }.coerceAtLeast(1)
      val categories = monthExpenses.map { (cat, amount) ->
        CategorySlice(
          name = cat!!.name,
          color = cat.color,
          iconKey = cat.icon,
          amount = amount,
          fraction = amount.toFloat() / totalExpense
        )
      }

      val recent = transactions.take(5).map { tx ->
        val cat = byId[tx.categoryId]
        TransactionRow(
          id = tx.id,
          note = tx.note,
          dateEpochDay = tx.date,
          amount = tx.amount,
          type = tx.type,
          categoryName = cat?.name ?: "Deleted",
          categoryColor = cat?.color ?: 0xFF8A8A8A,
          categoryIcon = cat?.icon ?: ""
        )
      }

      DashboardUiState(
        monthLabel = epochDayToMonthLabel(today.toEpochDay()),
        balance = balance,
        monthIncome = monthIncome,
        monthExpense = monthExpense,
        monthNet = monthNet,
        netDeltaPercent = delta,
        cashflow = cashflow,
        categories = categories,
        recent = recent
      )
    }.flowOn(Dispatchers.Default)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
