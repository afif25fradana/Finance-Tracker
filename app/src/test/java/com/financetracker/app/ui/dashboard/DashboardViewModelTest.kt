package com.financetracker.app.ui.dashboard

import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.CategoryRefCount
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

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
    override fun getById(id: Long): Flow<Transaction?> = MutableStateFlow(null)
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
  fun uiState_aggregatesIncomeExpenseAndBalance_acrossDefaultDispatcherBoundary() = runTest(testDispatcher) {
    val today = LocalDate.ofEpochDay(todayEpochDay())
    val thisMonthDay = today.withDayOfMonth(15).toEpochDay()
    val lastMonthDay = today.minusMonths(1).withDayOfMonth(10).toEpochDay()

    val catSalary = Category(1L, "Salary", TransactionType.INCOME, 0xFF00FF00, "payments", true)
    val catGroceries = Category(2L, "Groceries", TransactionType.EXPENSE, 0xFFFF0000, "shopping_cart", true)
    val catUtilities = Category(3L, "Utilities", TransactionType.EXPENSE, 0xFF0000FF, "bolt", false)

    val transactions = listOf(
      // This month
      Transaction(1L, 5_000_000L, TransactionType.INCOME, 1L, thisMonthDay, "Monthly Salary"),
      Transaction(2L, 1_200_000L, TransactionType.EXPENSE, 2L, thisMonthDay, "Supermarket"),
      Transaction(3L, 300_000L, TransactionType.EXPENSE, 3L, thisMonthDay, "Electricity bill"),
      // Last month
      Transaction(4L, 4_000_000L, TransactionType.INCOME, 1L, lastMonthDay, "Previous Salary"),
      Transaction(5L, 2_000_000L, TransactionType.EXPENSE, 2L, lastMonthDay, "Previous Groceries")
    )

    val txDao = FakeTransactionDao(transactions)
    val catDao = FakeCategoryDao(listOf(catSalary, catGroceries, catUtilities))

    val viewModel = DashboardViewModel(transactionDao = txDao, categoryDao = catDao)

    // Await state across the .flowOn(Dispatchers.Default) boundary
    val state = viewModel.uiState.first { it.monthLabel.isNotEmpty() }

    // Balance = (5M + 4M) - (1.2M + 0.3M + 2M) = 9M - 3.5M = 5.5M
    assertEquals(5_500_000L, state.balance)
    assertEquals(5_000_000L, state.monthIncome)
    assertEquals(1_500_000L, state.monthExpense)
    assertEquals(3_500_000L, state.monthNet)

    // Previous net = 4M - 2M = 2M. Delta = ((3.5M - 2M) / 2M) * 100 = 75.0%
    assertNotNull(state.netDeltaPercent)
    assertEquals(75.0f, state.netDeltaPercent!!, 0.001f)

    // Category breakdown for this month expenses
    assertEquals(2, state.categories.size)
    assertEquals("Groceries", state.categories[0].name)
    assertEquals(1_200_000L, state.categories[0].amount)
    assertEquals(0.8f, state.categories[0].fraction, 0.001f)

    assertEquals("Utilities", state.categories[1].name)
    assertEquals(300_000L, state.categories[1].amount)
    assertEquals(0.2f, state.categories[1].fraction, 0.001f)

    // Cashflow and Trend have 5 months
    assertEquals(5, state.cashflow.size)
    assertEquals(5, state.trend.size)
    val currentCashflow = state.cashflow.last()
    assertEquals(5_000_000L, currentCashflow.income)
    assertEquals(1_500_000L, currentCashflow.expense)
    assertEquals(1_500_000L, state.trend.last().amount)

    // Recent transactions (capped at 5, mapped to Category name)
    assertEquals(5, state.recent.size)
    assertEquals("Monthly Salary", state.recent[0].note)
    assertEquals("Salary", state.recent[0].categoryName)
  }

  @Test
  fun uiState_handlesEmptyTransactionsCorrectly() = runTest(testDispatcher) {
    val txDao = FakeTransactionDao(emptyList())
    val catDao = FakeCategoryDao(emptyList())

    val viewModel = DashboardViewModel(transactionDao = txDao, categoryDao = catDao)

    val state = viewModel.uiState.first { it.monthLabel.isNotEmpty() }

    assertEquals(0L, state.balance)
    assertEquals(0L, state.monthIncome)
    assertEquals(0L, state.monthExpense)
    assertEquals(0L, state.monthNet)
    assertNull(state.netDeltaPercent)
    assertTrue(state.categories.isEmpty())
    assertTrue(state.recent.isEmpty())
    assertEquals(5, state.cashflow.size)
    assertEquals(5, state.trend.size)
    assertTrue(state.cashflow.all { it.income == 0L && it.expense == 0L })
    assertTrue(state.trend.all { it.amount == 0L })
  }
}
