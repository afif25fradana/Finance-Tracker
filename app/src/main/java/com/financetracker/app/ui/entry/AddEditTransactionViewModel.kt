package com.financetracker.app.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.centsToInputText
import com.financetracker.app.ui.components.parseAmountToCents
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryUiState(
  val transactionType: TransactionType = TransactionType.EXPENSE,
  val amountText: String = "",
  val note: String = "",
  val selectedCategoryId: Long? = null,
  val categories: List<Category> = emptyList(),
  val dateEpochDay: Long = todayEpochDay(),
  val isEditing: Boolean = false,
  val error: String? = null
)

class AddEditTransactionViewModel(
  private val transactionDao: TransactionDao,
  categoryDao: CategoryDao,
  private val transactionId: Long?
) : ViewModel() {

  private val _uiState = MutableStateFlow(EntryUiState())
  val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

  private val _saved = MutableStateFlow(false)
  val saved: StateFlow<Boolean> = _saved.asStateFlow()

  init {
    if (transactionId != null) loadForEdit()
    viewModelScope.launch {
      categoryDao.getAll().collect { categories ->
        _uiState.update { state ->
          val validIds = categories.filter { it.type == state.transactionType }.map { it.id }
          val current = state.selectedCategoryId
          state.copy(
            categories = categories,
            selectedCategoryId = if (current == null || current !in validIds) validIds.firstOrNull() else current
          )
        }
      }
    }
  }

  private fun loadForEdit() {
    viewModelScope.launch {
      transactionDao.getById(transactionId!!).collect { tx ->
        if (tx != null) {
          _uiState.update {
            it.copy(
              transactionType = tx.type,
              amountText = centsToInputText(tx.amount),
              note = tx.note,
              selectedCategoryId = tx.categoryId,
              dateEpochDay = tx.date,
              isEditing = true,
              error = null
            )
          }
        }
      }
    }
  }

  fun onTypeSelected(type: TransactionType) {
    _uiState.update { state ->
      val validIds = state.categories.filter { it.type == type }.map { it.id }
      val current = state.selectedCategoryId
      state.copy(
        transactionType = type,
        selectedCategoryId = if (current == null || current !in validIds) validIds.firstOrNull() else current
      )
    }
  }

  fun onAmountChange(text: String) {
    if (text.matches(Regex("""\d*\.?\d{0,2}"""))) {
      _uiState.update { it.copy(amountText = text, error = null) }
    }
  }

  fun onNoteChange(note: String) {
    _uiState.update { it.copy(note = note) }
  }

  fun onCategorySelected(id: Long) {
    _uiState.update { it.copy(selectedCategoryId = id, error = null) }
  }

  fun onDateSelected(epochDay: Long) {
    _uiState.update { it.copy(dateEpochDay = epochDay) }
  }

  fun save() {
    val state = _uiState.value
    val cents = parseAmountToCents(state.amountText)
    when {
      cents == null || cents <= 0 -> _uiState.update { it.copy(error = "Enter an amount greater than zero") }
      state.selectedCategoryId == null -> _uiState.update { it.copy(error = "Select a category") }
      else -> {
        val transaction = Transaction(
          id = transactionId ?: 0,
          amount = cents,
          type = state.transactionType,
          categoryId = state.selectedCategoryId,
          date = state.dateEpochDay,
          note = state.note.trim()
        )
        viewModelScope.launch {
          if (state.isEditing) transactionDao.update(transaction)
          else transactionDao.insert(transaction)
          _saved.value = true
        }
      }
    }
  }
}
