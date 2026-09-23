package com.financetracker.app.ui.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.RecurringItemDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringRow(
  val item: RecurringItem,
  val category: Category?
)

data class RecurringUiState(
  val rows: List<RecurringRow> = emptyList(),
  val categories: List<Category> = emptyList()
)

class RecurringViewModel(
  context: Context,
  private val recurringItemDao: RecurringItemDao,
  categoryDao: CategoryDao,
  private val scheduleReminder: (Context, RecurringItem) -> Unit = { ctx, item ->
    ReminderScheduler.schedule(ctx, item)
  },
  private val cancelReminder: (Context, Long) -> Unit = { ctx, id ->
    ReminderScheduler.cancel(ctx, id)
  }
) : ViewModel() {

  private val appContext = context.applicationContext

  val uiState: StateFlow<RecurringUiState> =
    combine(
      recurringItemDao.getAll(),
      categoryDao.getAll()
    ) { items, categories ->
      val categoriesById = categories.associateBy { it.id }
      RecurringUiState(
        rows = items.map { item -> RecurringRow(item, categoriesById[item.categoryId]) },
        categories = categories
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringUiState())

  fun add(amount: Long, categoryId: Long, frequency: RecurringFrequency, nextDueDate: Long) {
    viewModelScope.launch {
      val id = recurringItemDao.insert(
        RecurringItem(
          categoryId = categoryId,
          amount = amount,
          frequency = frequency,
          nextDueDate = nextDueDate
        )
      )
      scheduleReminder(appContext, RecurringItem(id, categoryId, amount, frequency, nextDueDate))
    }
  }

  fun update(item: RecurringItem, amount: Long, categoryId: Long, frequency: RecurringFrequency, nextDueDate: Long) {
    val updated = item.copy(
      amount = amount,
      categoryId = categoryId,
      frequency = frequency,
      nextDueDate = nextDueDate
    )
    viewModelScope.launch {
      recurringItemDao.update(updated)
      scheduleReminder(appContext, updated)
    }
  }

  fun delete(item: RecurringItem) {
    viewModelScope.launch {
      recurringItemDao.delete(item)
      cancelReminder(appContext, item.id)
    }
  }
}
