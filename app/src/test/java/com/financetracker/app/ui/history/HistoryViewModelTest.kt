package com.financetracker.app.ui.history

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private class FakeCategoryDao(categories: List<Category> = emptyList()) : CategoryDao() {
    val flow = MutableStateFlow(categories)
    override fun getAll(): Flow<List<Category>> = flow
    override suspend fun getByIdOnce(id: Long): Category? = flow.value.find { it.id == id }
    override suspend fun insert(category: Category): Long = 1L
    override suspend fun update(category: Category) {}
    override suspend fun delete(category: Category) {}
    override fun transactionCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override fun recurringCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override suspend fun reassignTransactions(oldId: Long, newId: Long) {}
    override suspend fun reassignRecurring(oldId: Long, newId: Long) {}
    override suspend fun deleteById(id: Long) {}
  }

  private class FakeTransactionDao(transactions: List<Transaction> = emptyList()) : TransactionDao {
    val flow = MutableStateFlow(transactions)
    override fun getAll(): Flow<List<Transaction>> = flow
    override suspend fun getByIdOnce(id: Long): Transaction? = flow.value.find { it.id == id }
    override suspend fun getBetweenOnce(fromEpochDay: Long, toEpochDay: Long): List<TransactionExport> = emptyList()
    override suspend fun insert(transaction: Transaction): Long = 1L
    override suspend fun update(transaction: Transaction) {}
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
  fun uiState_filtersBySearchQuery_matchingNoteOrCategory() = runTest(testDispatcher) {
    val catGroceries = Category(1L, "Groceries", TransactionType.EXPENSE, 0xFFFF0000, "cart", true)
    val catSalary = Category(2L, "Salary", TransactionType.INCOME, 0xFF00FF00, "payments", true)

    val tx1 = Transaction(1L, 50_000L, TransactionType.EXPENSE, 1L, LocalDate.of(2026, 9, 10).toEpochDay(), "Weekly fruit")
    val tx2 = Transaction(2L, 100_000L, TransactionType.EXPENSE, 1L, LocalDate.of(2026, 9, 12).toEpochDay(), "Snacks & Drinks")
    val tx3 = Transaction(3L, 5_000_000L, TransactionType.INCOME, 2L, LocalDate.of(2026, 9, 1).toEpochDay(), "Paycheck")

    val txDao = FakeTransactionDao(listOf(tx1, tx2, tx3))
    val catDao = FakeCategoryDao(listOf(catGroceries, catSalary))
    val savedState = SavedStateHandle()

    val viewModel = HistoryViewModel(txDao, catDao, savedState)

    val initial = viewModel.uiState.first { it.totalCount == 3 }
    assertEquals(3, initial.months.flatMap { it.rows }.size)

    viewModel.onSearchQueryChange("fruit")
    advanceUntilIdle()
    val fruitMatch = viewModel.uiState.first { it.searchQuery == "fruit" }
    val fruitRows = fruitMatch.months.flatMap { it.rows }
    assertEquals(1, fruitRows.size)
    assertEquals(1L, fruitRows.first().id)

    viewModel.onSearchQueryChange("sal")
    advanceUntilIdle()
    val catMatch = viewModel.uiState.first { it.searchQuery == "sal" }
    val catRows = catMatch.months.flatMap { it.rows }
    assertEquals(1, catRows.size)
    assertEquals(3L, catRows.first().id)

    viewModel.onSearchQueryChange("nonexistent")
    advanceUntilIdle()
    val noMatch = viewModel.uiState.first { it.searchQuery == "nonexistent" }
    assertTrue(noMatch.months.isEmpty())
  }

  @Test
  fun uiState_filtersByTransactionType() = runTest(testDispatcher) {
    val cat = Category(1L, "General", TransactionType.EXPENSE, 0xFF888888, "tag", true)

    val txIncome = Transaction(1L, 1_000_000L, TransactionType.INCOME, 1L, LocalDate.of(2026, 9, 5).toEpochDay(), "Income 1")
    val txExpense = Transaction(2L, 200_000L, TransactionType.EXPENSE, 1L, LocalDate.of(2026, 9, 6).toEpochDay(), "Expense 1")

    val txDao = FakeTransactionDao(listOf(txIncome, txExpense))
    val catDao = FakeCategoryDao(listOf(cat))
    val viewModel = HistoryViewModel(txDao, catDao)

    viewModel.onTypeFilterChange(TransactionType.INCOME)
    advanceUntilIdle()
    val incomeState = viewModel.uiState.first { it.typeFilter == TransactionType.INCOME }
    val incomeRows = incomeState.months.flatMap { it.rows }
    assertEquals(1, incomeRows.size)
    assertEquals(TransactionType.INCOME, incomeRows.first().type)

    viewModel.onTypeFilterChange(TransactionType.EXPENSE)
    advanceUntilIdle()
    val expenseState = viewModel.uiState.first { it.typeFilter == TransactionType.EXPENSE }
    val expenseRows = expenseState.months.flatMap { it.rows }
    assertEquals(1, expenseRows.size)
    assertEquals(TransactionType.EXPENSE, expenseRows.first().type)

    viewModel.onTypeFilterChange(null)
    advanceUntilIdle()
    val allState = viewModel.uiState.first { it.typeFilter == null }
    assertEquals(2, allState.months.flatMap { it.rows }.size)
  }

  @Test
  fun uiState_groupsTransactionsByMonthDescending() = runTest(testDispatcher) {
    val cat = Category(1L, "General", TransactionType.EXPENSE, 0xFF888888, "tag", true)

    val sepTx = Transaction(1L, 100_000L, TransactionType.EXPENSE, 1L, LocalDate.of(2026, 9, 15).toEpochDay(), "Sep Tx")
    val augTx = Transaction(2L, 200_000L, TransactionType.INCOME, 1L, LocalDate.of(2026, 8, 20).toEpochDay(), "Aug Tx")

    val txDao = FakeTransactionDao(listOf(sepTx, augTx))
    val catDao = FakeCategoryDao(listOf(cat))
    val viewModel = HistoryViewModel(txDao, catDao)

    val state = viewModel.uiState.first { it.totalCount == 2 }
    assertEquals(2, state.months.size)

    val firstMonth = state.months[0]
    assertEquals(0L, firstMonth.income)
    assertEquals(100_000L, firstMonth.expense)
    assertEquals(1, firstMonth.rows.size)

    val secondMonth = state.months[1]
    assertEquals(200_000L, secondMonth.income)
    assertEquals(0L, secondMonth.expense)
    assertEquals(1, secondMonth.rows.size)
  }

  @Test
  fun savedStateHandle_persistsSearchAndFilterUpdates() = runTest(testDispatcher) {
    val savedState = SavedStateHandle()
    val viewModel = HistoryViewModel(FakeTransactionDao(), FakeCategoryDao(), savedState)

    viewModel.onSearchQueryChange("test query")
    assertEquals("test query", savedState.get<String>("history_search"))

    viewModel.onTypeFilterChange(TransactionType.INCOME)
    assertEquals("INCOME", savedState.get<String>("history_type_filter"))
  }

  @Test
  fun onSearchQueryChange_pastedOversizedQuery_capsAt50Chars() = runTest(testDispatcher) {
    val savedState = SavedStateHandle()
    val viewModel = HistoryViewModel(FakeTransactionDao(), FakeCategoryDao(), savedState)
    val pastedText = "Search query pasted from clipboard that is way too long for a transaction search".take(120)
    viewModel.onSearchQueryChange(pastedText)

    assertEquals(50, savedState.get<String>("history_search")?.length)
    assertEquals(pastedText.take(50), savedState.get<String>("history_search"))

    val state = viewModel.uiState.first { it.searchQuery.isNotEmpty() }
    assertEquals(com.financetracker.app.ui.components.MAX_SEARCH_QUERY_LENGTH, state.searchQuery.length)
    assertEquals(50, state.searchQuery.length)
    assertEquals(pastedText.take(50), state.searchQuery)
  }
}
