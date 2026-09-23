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

    // Trigger delete which causes SQLiteConstraintException
    viewModel.delete(category, reassignTo = null)
    advanceUntilIdle()

    // Verify exception was caught and error message surfaced in UI state
    val errorMessage = viewModel.uiState.value.errorMessage
    assertNotNull("Expected errorMessage to be set when delete fails with SQLiteConstraintException", errorMessage)
    assertTrue("Error message should mention category name", errorMessage!!.contains("Utilities"))
    assertTrue("Error message should explain references", errorMessage.contains("referenced by existing"))

    // Verify clearError resets the message
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
}
