package com.financetracker.app.ui.categories

import android.database.sqlite.SQLiteConstraintException
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.CategoryRefCount
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private open class TestCategoryDao : CategoryDao() {
    val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val txCountFlow = MutableStateFlow<List<CategoryRefCount>>(emptyList())
    val recurringCountFlow = MutableStateFlow<List<CategoryRefCount>>(emptyList())

    var deletedCategories = mutableListOf<Category>()
    var throwOnDelete: Throwable? = null

    override fun getAll(): Flow<List<Category>> = categoriesFlow
    override suspend fun getByIdOnce(id: Long): Category? = categoriesFlow.value.find { it.id == id }
    override suspend fun insert(category: Category): Long = 1L
    override suspend fun update(category: Category) {}
    override suspend fun delete(category: Category) {
      throwOnDelete?.let { throw it }
      deletedCategories.add(category)
    }
    override fun transactionCounts(): Flow<List<CategoryRefCount>> = txCountFlow
    override fun recurringCounts(): Flow<List<CategoryRefCount>> = recurringCountFlow
    override suspend fun reassignTransactions(oldId: Long, newId: Long) {}
    override suspend fun reassignRecurring(oldId: Long, newId: Long) {}
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
  fun delete_whenCategoryDaoThrowsConstraintException_catchesAndSetsErrorMessageInUiState() = runTest(testDispatcher) {
    val category = Category(
      id = 10L,
      name = "Utilities",
      type = TransactionType.EXPENSE,
      color = 0xFF123456,
      icon = "bolt"
    )

    val dao = TestCategoryDao().apply {
      categoriesFlow.value = listOf(category)
      throwOnDelete = SQLiteConstraintException("FOREIGN KEY constraint failed: categories.id referenced")
    }

    val viewModel = CategoriesViewModel(categoryDao = dao)
    val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.delete(category, reassignTo = null)
    advanceUntilIdle()

    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Expected errorMessage to be set when delete fails with SQLiteConstraintException", errorMessage)
    assertTrue("Error message should mention category name", errorMessage!!.contains("Utilities"))
    assertTrue("Error message should explain references", errorMessage.contains("referenced by existing"))

    viewModel.clearError()
    advanceUntilIdle()
    assertNull("Expected errorMessage to be cleared", viewModel.uiState.value.errorMessage)

    collectJob.cancel()
  }

  @Test
  fun delete_whenSuccessful_removesCategoryAndNoError() = runTest(testDispatcher) {
    val category = Category(
      id = 10L,
      name = "Groceries",
      type = TransactionType.EXPENSE,
      color = 0xFF123456,
      icon = "cart"
    )

    val dao = TestCategoryDao().apply {
      categoriesFlow.value = listOf(category)
    }

    val viewModel = CategoriesViewModel(categoryDao = dao)
    val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.delete(category, reassignTo = null)
    advanceUntilIdle()

    assertEquals(listOf(category), dao.deletedCategories)
    assertNull(viewModel.uiState.value.errorMessage)

    collectJob.cancel()
  }

  @Test
  fun add_pastedOversizedCategoryName_capsAt36Chars() = runTest(testDispatcher) {
    var inserted: Category? = null
    val dao = object : TestCategoryDao() {
      override suspend fun insert(category: Category): Long {
        inserted = category
        return 10L
      }
    }
    val viewModel = CategoriesViewModel(categoryDao = dao)
    val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    val pastedText = "Extremely Long Category Name Pasted From Clipboard Exceeding Thirty Six Characters"
    viewModel.add(name = pastedText, type = TransactionType.EXPENSE, color = 0xFF123456)
    advanceUntilIdle()

    assertNotNull(inserted)
    assertEquals(com.financetracker.app.ui.components.MAX_CATEGORY_NAME_LENGTH, inserted?.name?.length)
    assertEquals(36, inserted?.name?.length)
    assertEquals(pastedText.take(36), inserted?.name)

    collectJob.cancel()
  }

  @Test
  fun add_whenDaoThrows_setsErrorMessage() = runTest(testDispatcher) {
    val dao = object : TestCategoryDao() {
      override suspend fun insert(category: Category): Long = throw RuntimeException("DB Disk Full")
    }
    val viewModel = CategoriesViewModel(categoryDao = dao)
    val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.add(name = "New Category", type = TransactionType.EXPENSE, color = 0xFF123456)
    advanceUntilIdle()

    assertEquals("Failed to add category 'New Category'.", viewModel.uiState.value.errorMessage)
    collectJob.cancel()
  }

  @Test
  fun update_whenDaoThrows_setsErrorMessage() = runTest(testDispatcher) {
    val category = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE, color = 1L, icon = "cart")
    val dao = object : TestCategoryDao() {
      override suspend fun update(category: Category) {
        throw RuntimeException("DB Locked")
      }
    }
    val viewModel = CategoriesViewModel(categoryDao = dao)
    val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.update(category)
    advanceUntilIdle()

    assertEquals("Failed to update category 'Food'.", viewModel.uiState.value.errorMessage)
    collectJob.cancel()
  }

  @Test
  fun sameTypeNames_evaluatesNamesPerTypeAndExcludesCurrentId() {
    val expenseCat1 = Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE, color = 1L, icon = "cart")
    val expenseCat2 = Category(id = 2L, name = "Dining", type = TransactionType.EXPENSE, color = 2L, icon = "food")
    val incomeCat1 = Category(id = 3L, name = "Salary", type = TransactionType.INCOME, color = 3L, icon = "cash")
    val incomeCat2 = Category(id = 4L, name = "Groceries", type = TransactionType.INCOME, color = 4L, icon = "cart")

    val state = CategoriesUiState(
      expense = listOf(CategoryRow(expenseCat1, 0, 0), CategoryRow(expenseCat2, 0, 0)),
      income = listOf(CategoryRow(incomeCat1, 0, 0), CategoryRow(incomeCat2, 0, 0))
    )

    val expenseNames = sameTypeNames(state, TransactionType.EXPENSE, excludeId = null)
    assertEquals(setOf("groceries", "dining"), expenseNames)

    val incomeNames = sameTypeNames(state, TransactionType.INCOME, excludeId = null)
    assertEquals(setOf("salary", "groceries"), incomeNames)

    val editingExpenseNames = sameTypeNames(state, TransactionType.EXPENSE, excludeId = 1L)
    assertEquals(setOf("dining"), editingExpenseNames)
  }
}
