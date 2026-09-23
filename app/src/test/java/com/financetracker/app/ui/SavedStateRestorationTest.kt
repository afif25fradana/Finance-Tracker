package com.financetracker.app.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.CategoryRefCount
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.backup.BackupRestoreViewModel
import com.financetracker.app.ui.entry.AddEditTransactionViewModel
import com.financetracker.app.ui.export.ExportFormat
import com.financetracker.app.ui.export.ExportViewModel
import com.financetracker.app.ui.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SavedStateRestorationTest {

  private val testDispatcher = StandardTestDispatcher()

  private class DummyCategoryDao(private val categories: List<Category> = emptyList()) : CategoryDao() {
    override fun getAll(): Flow<List<Category>> = MutableStateFlow(categories)
    override suspend fun getByIdOnce(id: Long): Category? = categories.find { it.id == id }
    override suspend fun insert(category: Category): Long = 1L
    override suspend fun update(category: Category) {}
    override suspend fun delete(category: Category) {}
    override fun transactionCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override fun recurringCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override suspend fun reassignTransactions(oldId: Long, newId: Long) {}
    override suspend fun reassignRecurring(oldId: Long, newId: Long) {}
    override suspend fun deleteById(id: Long) {}
  }

  private class DummyTransactionDao : TransactionDao {
    override fun getAll(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    override fun getById(id: Long): Flow<Transaction?> = MutableStateFlow(null)
    override suspend fun getByIdOnce(id: Long): Transaction? = null
    override suspend fun getBetweenOnce(fromEpochDay: Long, toEpochDay: Long): List<TransactionExport> = emptyList()
    override suspend fun insert(transaction: Transaction): Long = 1L
    override suspend fun update(transaction: Transaction) {}
    override suspend fun deleteById(id: Long) {}
  }

  private class DummyBackupDao : BackupDao() {
    override suspend fun getAllCategoriesOnce(): List<Category> = emptyList()
    override suspend fun getAllTransactionsOnce(): List<Transaction> = emptyList()
    override suspend fun getAllRecurringOnce(): List<RecurringItem> = emptyList()
    override suspend fun clearTransactions() {}
    override suspend fun clearRecurring() {}
    override suspend fun clearCategories() {}
    override suspend fun insertCategories(items: List<Category>) {}
    override suspend fun insertRecurring(items: List<RecurringItem>) {}
    override suspend fun insertTransactions(items: List<Transaction>) {}
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun addEditTransactionViewModel_restoresStateFromSavedStateHandle() = runTest(testDispatcher) {
    val category = Category(id = 42L, name = "Salary", type = TransactionType.INCOME, color = 0xFF00FF00, icon = "briefcase")
    val savedState = SavedStateHandle(
      mapOf(
        "add_edit_type" to "INCOME",
        "add_edit_amount" to "750000",
        "add_edit_note" to "Restored invoice payment",
        "add_edit_category_id" to 42L,
        "add_edit_date" to 20500L,
        "add_edit_restored" to true
      )
    )

    val viewModel = AddEditTransactionViewModel(
      transactionDao = DummyTransactionDao(),
      categoryDao = DummyCategoryDao(listOf(category)),
      transactionId = 99L,
      savedStateHandle = savedState
    )
    advanceUntilIdle()

    assertEquals(TransactionType.INCOME, viewModel.uiState.value.transactionType)
    assertEquals("750000", viewModel.uiState.value.amountText)
    assertEquals("Restored invoice payment", viewModel.uiState.value.note)
    assertEquals(42L, viewModel.uiState.value.selectedCategoryId)
    assertEquals(20500L, viewModel.uiState.value.dateEpochDay)
  }

  @Test
  fun historyViewModel_restoresStateFromSavedStateHandle() = runTest(testDispatcher) {
    val savedState = SavedStateHandle(
      mapOf(
        "history_search" to "groceries",
        "history_type_filter" to "EXPENSE"
      )
    )

    val viewModel = HistoryViewModel(
      transactionDao = DummyTransactionDao(),
      categoryDao = DummyCategoryDao(),
      savedStateHandle = savedState
    )
    advanceUntilIdle()

    assertEquals("groceries", viewModel.uiState.value.searchQuery)
    assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.typeFilter)
  }

  @Test
  fun exportViewModel_restoresStateFromSavedStateHandle() {
    val savedState = SavedStateHandle(
      mapOf(
        "export_from" to 19500L,
        "export_to" to 19530L,
        "export_format" to "JSON"
      )
    )

    // Using custom context subclass avoiding Android framework instantiation
    val context = object : android.content.ContextWrapper(null) {
      override fun getApplicationContext(): Context = this
      override fun getContentResolver(): android.content.ContentResolver? = null
    }

    val viewModel = ExportViewModel(
      context = context,
      transactionDao = DummyTransactionDao(),
      savedStateHandle = savedState
    )

    assertEquals(19500L, viewModel.uiState.value.fromEpochDay)
    assertEquals(19530L, viewModel.uiState.value.toEpochDay)
    assertEquals(ExportFormat.JSON, viewModel.uiState.value.format)
  }

  @Test
  fun backupRestoreViewModel_restoresStateFromSavedStateHandle() {
    val savedState = SavedStateHandle(
      mapOf(
        "backup_message" to "Restored: 42 transactions imported successfully.",
        "backup_is_error" to false
      )
    )

    val context = object : android.content.ContextWrapper(null) {
      override fun getApplicationContext(): Context = this
      override fun getContentResolver(): android.content.ContentResolver? = null
    }

    val viewModel = BackupRestoreViewModel(
      context = context,
      backupDao = DummyBackupDao(),
      savedStateHandle = savedState
    )

    assertEquals("Restored: 42 transactions imported successfully.", viewModel.uiState.value.message)
    assertEquals(false, viewModel.uiState.value.isError)
  }
}
