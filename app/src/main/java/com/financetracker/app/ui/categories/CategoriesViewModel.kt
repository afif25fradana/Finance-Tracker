package com.financetracker.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(
  val category: Category,
  val transactionCount: Int,
  val recurringCount: Int
) {
  val totalReferences: Int get() = transactionCount + recurringCount
  val isDefault: Boolean get() = category.isDefault
}

data class CategoriesUiState(
  val expense: List<CategoryRow> = emptyList(),
  val income: List<CategoryRow> = emptyList()
)

class CategoriesViewModel(
  private val categoryDao: CategoryDao
) : ViewModel() {

  val uiState: StateFlow<CategoriesUiState> =
    combine(
      categoryDao.getAll(),
      categoryDao.transactionCounts(),
      categoryDao.recurringCounts()
    ) { categories, txCounts, recurringCounts ->
      val txByCategory = txCounts.associate { it.categoryId to it.refCount }
      val recurringByCategory = recurringCounts.associate { it.categoryId to it.refCount }
      val rows = categories.map { cat ->
        CategoryRow(
          category = cat,
          transactionCount = txByCategory[cat.id] ?: 0,
          recurringCount = recurringByCategory[cat.id] ?: 0
        )
      }
      CategoriesUiState(
        expense = rows.filter { it.category.type == TransactionType.EXPENSE },
        income = rows.filter { it.category.type == TransactionType.INCOME }
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

  fun add(name: String, type: TransactionType, color: Long) {
    viewModelScope.launch {
      categoryDao.insert(
        Category(
          name = name,
          type = type,
          color = color,
          icon = "category",
          isDefault = false
        )
      )
    }
  }

  fun update(category: Category) {
    viewModelScope.launch { categoryDao.update(category) }
  }

  fun delete(category: Category, reassignTo: Long?) {
    viewModelScope.launch {
      if (reassignTo == null) {
        categoryDao.delete(category)
      } else {
        categoryDao.reassignAndDelete(category.id, reassignTo)
      }
    }
  }
}
