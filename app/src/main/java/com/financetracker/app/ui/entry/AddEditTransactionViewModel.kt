package com.financetracker.app.ui.entry

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.MAX_NOTE_LENGTH
import com.financetracker.app.ui.components.addPresetToAmount
import com.financetracker.app.ui.components.digitsOnly
import com.financetracker.app.ui.components.parseAmount
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.CancellationException
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
  private val transactionId: Long?,
  @get:VisibleForTesting
  internal val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

  companion object {
    internal const val KEY_TYPE = "add_edit_type"
    internal const val KEY_AMOUNT = "add_edit_amount"
    internal const val KEY_NOTE = "add_edit_note"
    internal const val KEY_CATEGORY_ID = "add_edit_category_id"
    internal const val KEY_DATE = "add_edit_date"
    internal const val KEY_RESTORED = "add_edit_restored"
  }

  private val isRestored = savedStateHandle?.get<Boolean>(KEY_RESTORED) ?: false

  private val _uiState = MutableStateFlow(
    if (isRestored) {
      EntryUiState(
        transactionType = savedStateHandle?.get<String>(KEY_TYPE)?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE,
        amountText = savedStateHandle?.get<String>(KEY_AMOUNT) ?: "",
        note = savedStateHandle?.get<String>(KEY_NOTE) ?: "",
        selectedCategoryId = savedStateHandle?.get<Long>(KEY_CATEGORY_ID),
        dateEpochDay = savedStateHandle?.get<Long>(KEY_DATE) ?: todayEpochDay(),
        isEditing = transactionId != null
      )
    } else {
      EntryUiState()
    }
  )
  val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

  private val _saved = MutableStateFlow(false)
  val saved: StateFlow<Boolean> = _saved.asStateFlow()

  private val _deleted = MutableStateFlow(false)
  val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

  private var saving = false

  init {
    savedStateHandle?.set(KEY_RESTORED, true)
    viewModelScope.launch {
      if (transactionId != null && !isRestored) {
        val tx = transactionDao.getByIdOnce(transactionId)
        if (tx != null) {
          savedStateHandle?.set(KEY_TYPE, tx.type.name)
          savedStateHandle?.set(KEY_AMOUNT, tx.amount.toString())
          savedStateHandle?.set(KEY_NOTE, tx.note)
          savedStateHandle?.set(KEY_CATEGORY_ID, tx.categoryId)
          savedStateHandle?.set(KEY_DATE, tx.date)
          _uiState.update {
            it.copy(
              transactionType = tx.type,
              amountText = tx.amount.toString(),
              note = tx.note,
              selectedCategoryId = tx.categoryId,
              dateEpochDay = tx.date,
              isEditing = true,
              error = null
            )
          }
        }
      }
      categoryDao.getAll().collect { categories ->
        _uiState.update { state ->
          val validIds = categories.filter { it.type == state.transactionType }.map { it.id }
          val current = state.selectedCategoryId
          val resolvedCategory = if (current == null || current !in validIds) validIds.firstOrNull() else current
          savedStateHandle?.set(KEY_CATEGORY_ID, resolvedCategory)
          state.copy(
            categories = categories,
            selectedCategoryId = resolvedCategory
          )
        }
      }
    }
  }



  fun onTypeSelected(type: TransactionType) {
    savedStateHandle?.set(KEY_TYPE, type.name)
    _uiState.update { state ->
      val validIds = state.categories.filter { it.type == type }.map { it.id }
      val current = state.selectedCategoryId
      val resolvedCategory = if (current == null || current !in validIds) validIds.firstOrNull() else current
      savedStateHandle?.set(KEY_CATEGORY_ID, resolvedCategory)
      state.copy(
        transactionType = type,
        selectedCategoryId = resolvedCategory
      )
    }
  }

  fun onAmountChange(text: String) {
    val digits = digitsOnly(text)
    savedStateHandle?.set(KEY_AMOUNT, digits)
    _uiState.update { it.copy(amountText = digits, error = null) }
  }

  fun onQuickAdd(preset: Long) {
    val updated = addPresetToAmount(_uiState.value.amountText, preset)
    savedStateHandle?.set(KEY_AMOUNT, updated)
    _uiState.update { it.copy(amountText = updated, error = null) }
  }

  fun onNoteChange(note: String) {
    val capped = note.take(MAX_NOTE_LENGTH)
    savedStateHandle?.set(KEY_NOTE, capped)
    _uiState.update { it.copy(note = capped) }
  }

  fun onCategorySelected(id: Long) {
    savedStateHandle?.set(KEY_CATEGORY_ID, id)
    _uiState.update { it.copy(selectedCategoryId = id, error = null) }
  }

  fun onDateSelected(epochDay: Long) {
    savedStateHandle?.set(KEY_DATE, epochDay)
    _uiState.update { it.copy(dateEpochDay = epochDay) }
  }

  fun save() {
    val state = _uiState.value
    val amount = parseAmount(state.amountText)
    when {
      amount == null || amount <= 0 -> _uiState.update { it.copy(error = "Enter an amount greater than zero") }
      state.selectedCategoryId == null -> _uiState.update { it.copy(error = "Select a category") }
      else -> {
        if (saving) return
        saving = true
        val transaction = Transaction(
          id = transactionId ?: 0,
          amount = amount,
          type = state.transactionType,
          categoryId = state.selectedCategoryId,
          date = state.dateEpochDay,
          note = state.note.trim().take(MAX_NOTE_LENGTH)
        )
        viewModelScope.launch {
          try {
            if (state.isEditing) transactionDao.update(transaction)
            else transactionDao.insert(transaction)
            _saved.value = true
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            saving = false
            _uiState.update { it.copy(error = "Failed to save transaction") }
          }
        }
      }
    }
  }

  fun onSaveHandled() { _saved.value = false }
  fun onDeleteHandled() { _deleted.value = false }

  fun delete() {
    val id = transactionId ?: return
    viewModelScope.launch {
      try {
        transactionDao.deleteById(id)
        _deleted.value = true
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update { it.copy(error = "Failed to delete transaction") }
      }
    }
  }
}
