package com.financetracker.app.data.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringItemDaoTest {

  private lateinit var db: AppDatabase
  private lateinit var categoryDao: CategoryDao
  private lateinit var recurringItemDao: RecurringItemDao
  private var defaultCategoryId: Long = 0

  @Before
  fun setUp() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    db = AppDatabase.createInMemory(context, withDefaultSeed = false)
    categoryDao = db.categoryDao()
    recurringItemDao = db.recurringItemDao()
    defaultCategoryId = categoryDao.insert(
      Category(name = "Bills", type = TransactionType.EXPENSE, color = 0xFF123456, icon = "receipt", isDefault = true)
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun insertAndGetByIdOnce_returnsItem() = runTest {
    val item = RecurringItem(
      categoryId = defaultCategoryId,
      amount = 150000,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = 18000
    )
    val id = recurringItemDao.insert(item)

    val loaded = recurringItemDao.getByIdOnce(id)
    assertNotNull(loaded)
    assertEquals(id, loaded!!.id)
    assertEquals(defaultCategoryId, loaded.categoryId)
    assertEquals(150000L, loaded.amount)
    assertEquals(RecurringFrequency.MONTHLY, loaded.frequency)
    assertEquals(18000L, loaded.nextDueDate)
  }

  @Test
  fun getAllAndGetAllOnce_ordersByNextDueDateAsc() = runTest {
    val item1 = recurringItemDao.insert(RecurringItem(categoryId = defaultCategoryId, amount = 10, frequency = RecurringFrequency.DAILY, nextDueDate = 300))
    val item2 = recurringItemDao.insert(RecurringItem(categoryId = defaultCategoryId, amount = 20, frequency = RecurringFrequency.WEEKLY, nextDueDate = 100))
    val item3 = recurringItemDao.insert(RecurringItem(categoryId = defaultCategoryId, amount = 30, frequency = RecurringFrequency.YEARLY, nextDueDate = 200))

    val fromFlow = recurringItemDao.getAll().first()
    assertEquals(listOf(item2, item3, item1), fromFlow.map { it.id })
    assertEquals(listOf(100L, 200L, 300L), fromFlow.map { it.nextDueDate })

    val fromOnce = recurringItemDao.getAllOnce()
    assertEquals(fromFlow, fromOnce)
  }

  @Test
  fun insertWithInvalidCategoryId_throwsForeignKeyConstraint() = runTest {
    val nonExistentCatId = 999999L

    assertThrows(SQLiteConstraintException::class.java) {
      kotlinx.coroutines.runBlocking {
        recurringItemDao.insert(
          RecurringItem(categoryId = nonExistentCatId, amount = 50, frequency = RecurringFrequency.MONTHLY, nextDueDate = 10)
        )
      }
    }
  }

  @Test
  fun updateAndIndividualDelete_modifiesAndRemovesItem() = runTest {
    val id = recurringItemDao.insert(
      RecurringItem(categoryId = defaultCategoryId, amount = 100, frequency = RecurringFrequency.WEEKLY, nextDueDate = 500)
    )
    val loaded = recurringItemDao.getByIdOnce(id)!!

    recurringItemDao.update(loaded.copy(amount = 200, frequency = RecurringFrequency.DAILY, nextDueDate = 600))
    val updated = recurringItemDao.getByIdOnce(id)!!
    assertEquals(200L, updated.amount)
    assertEquals(RecurringFrequency.DAILY, updated.frequency)
    assertEquals(600L, updated.nextDueDate)

    recurringItemDao.delete(updated)
    assertNull(recurringItemDao.getByIdOnce(id))
  }
}
