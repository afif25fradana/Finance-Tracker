package com.financetracker.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.epochDayToMonthLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class HistoryRow(
  val id: Long,
  val note: String,
  val dateEpochDay: Long,
  val amount: Long,
  val type: TransactionType,
  val categoryName: String,
  val categoryColor: Long,
  val categoryIcon: String
)

data class HistoryMonth(
  val label: String,
  val income: Long,
  val expense: Long,
  val rows: List<HistoryRow>
)

data class HistoryUiState(
  val searchQuery: String = "",
  val typeFilter: TransactionType? = null,
  val totalCount: Int = 0,
  val months: List<HistoryMonth> = emptyList()
)

class HistoryViewModel(
  transactionDao: TransactionDao,
  categoryDao: CategoryDao
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val typeFilter = MutableStateFlow<TransactionType?>(null)

  val uiState: StateFlow<HistoryUiState> =
    combine(
      transactionDao.getAll(),
      categoryDao.getAll(),
      searchQuery,
      typeFilter
    ) { transactions, categories, query, filter ->
      val byId = categories.associateBy { it.id }
      val q = query.trim().lowercase()

      val allRows = transactions.map { tx ->
        val category = byId[tx.categoryId]
        HistoryRow(
          id = tx.id,
          note = tx.note,
          dateEpochDay = tx.date,
          amount = tx.amount,
          type = tx.type,
          categoryName = category?.name ?: "Deleted",
          categoryColor = category?.color ?: 0xFF8A8A8A,
          categoryIcon = category?.icon ?: ""
        )
      }

      val filtered = allRows.filter { row ->
        val matchesType = filter == null || row.type == filter
        val matchesQuery = q.isEmpty() ||
            row.note.lowercase().contains(q) ||
            row.categoryName.lowercase().contains(q)
        matchesType && matchesQuery
      }

      val months = filtered
        .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        .entries
        .sortedByDescending { it.key }
        .map { (yearMonth, rows) ->
          HistoryMonth(
            label = epochDayToMonthLabel(yearMonth.atDay(1).toEpochDay()),
            income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            rows = rows
          )
        }

      HistoryUiState(
        searchQuery = query,
        typeFilter = filter,
        totalCount = transactions.size,
        months = months
      )
    }.flowOn(Dispatchers.Default)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

  fun onSearchQueryChange(value: String) {
    searchQuery.value = value
  }

  fun onTypeFilterChange(value: TransactionType?) {
    typeFilter.value = value
  }
}
