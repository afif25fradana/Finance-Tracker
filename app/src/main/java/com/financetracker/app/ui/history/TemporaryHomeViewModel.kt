package com.financetracker.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeRow(
  val id: Long,
  val note: String,
  val dateEpochDay: Long,
  val amountCents: Long,
  val type: TransactionType,
  val categoryName: String,
  val categoryColor: Long
)

class TemporaryHomeViewModel(
  transactionDao: TransactionDao,
  categoryDao: CategoryDao
) : ViewModel() {

  private val _rows = MutableStateFlow<List<HomeRow>>(emptyList())
  val rows: StateFlow<List<HomeRow>> = _rows.asStateFlow()

  init {
    viewModelScope.launch {
      combine(transactionDao.getAll(), categoryDao.getAll()) { transactions, categories ->
        val byId = categories.associateBy { it.id }
        transactions.map { tx: Transaction ->
          HomeRow(
            id = tx.id,
            note = tx.note,
            dateEpochDay = tx.date,
            amountCents = tx.amount,
            type = tx.type,
            categoryName = byId[tx.categoryId]?.name ?: "Deleted",
            categoryColor = byId[tx.categoryId]?.color ?: 0xFF8A8A8A
          )
        }
      }.collect { _rows.value = it }
    }
  }
}
