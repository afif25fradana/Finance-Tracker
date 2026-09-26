package com.financetracker.app.ui.entry

import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.CategoryRefCount
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private class FakeCategoryDao : CategoryDao() {
    val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    override fun getAll(): Flow<List<Category>> = categoriesFlow
    override suspend fun getByIdOnce(id: Long): Category? = categoriesFlow.value.find { it.id == id }
    override suspend fun insert(category: Category): Long = 1L
    override suspend fun update(category: Category) {}
    override suspend fun delete(category: Category) {}
    override fun transactionCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override fun recurringCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override suspend fun reassignTransactions(oldId: Long, newId: Long) {}
    override suspend fun reassignRecurring(oldId: Long, newId: Long) {}
    override suspend fun deleteById(id: Long) {}
  }

  private class FakeTransactionDao : TransactionDao {
    val continuousFlow = MutableSharedFlow<Transaction?>(replay = 1)
    var currentTransaction: Transaction? = null

    override fun getAll(): Flow<List<Transaction>> = MutableStateFlow(emptyList())

    override fun getById(id: Long): Flow<Transaction?> = continuousFlow

    override suspend fun getByIdOnce(id: Long): Transaction? = currentTransaction

    override suspend fun getBetweenOnce(fromEpochDay: Long, toEpochDay: Long): List<TransactionExport> = emptyList()

    override suspend fun insert(transaction: Transaction): Long = transaction.id

    override suspend fun update(transaction: Transaction) {
      currentTransaction = transaction
      continuousFlow.tryEmit(transaction)
    }

    override suspend fun deleteById(id: Long) {}
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
  fun loadForEdit_loadsInitialTransactionState() = runTest(testDispatcher) {
    val fakeTxDao = FakeTransactionDao().apply {
      currentTransaction = Transaction(
        id = 10L,
        amount = 50000L,
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        date = 20000L,
        note = "Original Note"
      )
    }
    val fakeCatDao = FakeCategoryDao()

    val viewModel = AddEditTransactionViewModel(
      transactionDao = fakeTxDao,
      categoryDao = fakeCatDao,
      transactionId = 10L
    )
    advanceUntilIdle()

    assertEquals("50000", viewModel.uiState.value.amountText)
    assertEquals("Original Note", viewModel.uiState.value.note)
    assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
    assertEquals(true, viewModel.uiState.value.isEditing)
  }

  @Test
  fun midEdit_uncommittedUserEditsNotOverwrittenByFlowOrTableInvalidation() = runTest(testDispatcher) {
    val initialTx = Transaction(
      id = 10L,
      amount = 50000L,
      type = TransactionType.EXPENSE,
      categoryId = 1L,
      date = 20000L,
      note = "Original Note"
    )
    val fakeTxDao = FakeTransactionDao().apply {
      currentTransaction = initialTx
      continuousFlow.tryEmit(initialTx)
    }
    val fakeCatDao = FakeCategoryDao()

    val viewModel = AddEditTransactionViewModel(
      transactionDao = fakeTxDao,
      categoryDao = fakeCatDao,
      transactionId = 10L
    )
    advanceUntilIdle()

    // User is mid-edit, typing a new amount and a new note
    viewModel.onAmountChange("125000")
    viewModel.onNoteChange("Draft note in progress")
    advanceUntilIdle()

    assertEquals("125000", viewModel.uiState.value.amountText)
    assertEquals("Draft note in progress", viewModel.uiState.value.note)

    // Simulate an external table invalidation or concurrent DB write emitting to the continuous flow
    val externalTx = initialTx.copy(note = "External Background Change", amount = 75000L)
    fakeTxDao.continuousFlow.emit(externalTx)
    advanceUntilIdle()

    // With one-shot getByIdOnce, the continuous Flow is not collected, so active user edits survive
    assertEquals("125000", viewModel.uiState.value.amountText)
    assertEquals("Draft note in progress", viewModel.uiState.value.note)
  }

  @Test
  fun onNoteChange_pastedOversizedNote_capsAt100Chars() = runTest {
    val viewModel = AddEditTransactionViewModel(
      transactionDao = FakeTransactionDao(),
      categoryDao = FakeCategoryDao(),
      transactionId = null
    )
    val pastedText = "A".repeat(250) // 250 chars pasted
    viewModel.onNoteChange(pastedText)

    val note = viewModel.uiState.value.note
    assertEquals(com.financetracker.app.ui.components.MAX_NOTE_LENGTH, note.length)
    assertEquals(100, note.length)
    assertEquals("A".repeat(100), note)
  }
}
