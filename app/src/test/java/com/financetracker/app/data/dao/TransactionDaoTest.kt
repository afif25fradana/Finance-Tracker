package com.financetracker.app.data.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.Transaction
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
class TransactionDaoTest {

  private lateinit var db: AppDatabase
  private lateinit var categoryDao: CategoryDao
  private lateinit var transactionDao: TransactionDao
  private var defaultCategoryId: Long = 0

  @Before
  fun setUp() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    db = AppDatabase.createInMemory(context, withDefaultSeed = false)
    categoryDao = db.categoryDao()
    transactionDao = db.transactionDao()
    defaultCategoryId = categoryDao.insert(
      Category(name = "General", type = TransactionType.EXPENSE, color = 0xFF000000, icon = "category", isDefault = true)
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun insertAndGetByIdOnce_returnsTransaction() = runTest {
    val tx = Transaction(
      amount = 50000,
      type = TransactionType.EXPENSE,
      categoryId = defaultCategoryId,
      date = 1000,
      note = "Lunch"
    )
    val id = transactionDao.insert(tx)

    val loadedOnce = transactionDao.getByIdOnce(id)
    assertNotNull(loadedOnce)
    assertEquals(id, loadedOnce!!.id)
    assertEquals(50000L, loadedOnce.amount)
    assertEquals(TransactionType.EXPENSE, loadedOnce.type)
    assertEquals(defaultCategoryId, loadedOnce.categoryId)
    assertEquals(1000L, loadedOnce.date)
    assertEquals("Lunch", loadedOnce.note)
  }

  @Test
  fun getAll_ordersByDateDescThenIdDesc() = runTest {
    val tx1 = transactionDao.insert(Transaction(amount = 10, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 100, note = "d100"))
    val tx2 = transactionDao.insert(Transaction(amount = 20, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 200, note = "d200_id1"))
    val tx3 = transactionDao.insert(Transaction(amount = 30, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 200, note = "d200_id2"))
    val tx4 = transactionDao.insert(Transaction(amount = 40, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 50, note = "d50"))

    val all = transactionDao.getAll().first()
    assertEquals(listOf(tx3, tx2, tx1, tx4), all.map { it.id })
    assertEquals(listOf(200L, 200L, 100L, 50L), all.map { it.date })
  }

  @Test
  fun getBetweenOnce_filtersInclusiveDateRangeAndOrdersDateAscIdAsc() = runTest {
    val incomeCatId = categoryDao.insert(
      Category(name = "Salary", type = TransactionType.INCOME, color = 0xFF00FF00, icon = "salary", isDefault = false)
    )

    transactionDao.insert(Transaction(amount = 50, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 5, note = "Before range"))
    val id10a = transactionDao.insert(Transaction(amount = 100, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 10, note = "Day 10 First"))
    val id10b = transactionDao.insert(Transaction(amount = 200, type = TransactionType.INCOME, categoryId = incomeCatId, date = 10, note = "Day 10 Second"))
    val id15 = transactionDao.insert(Transaction(amount = 300, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 15, note = "Day 15 Boundary"))
    transactionDao.insert(Transaction(amount = 400, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 20, note = "After range"))

    val exports = transactionDao.getBetweenOnce(fromEpochDay = 10, toEpochDay = 15)

    assertEquals(3, exports.size)
    assertEquals(
      listOf(
        TransactionExport(date = 10, amount = 100, type = TransactionType.EXPENSE, category = "General", note = "Day 10 First"),
        TransactionExport(date = 10, amount = 200, type = TransactionType.INCOME, category = "Salary", note = "Day 10 Second"),
        TransactionExport(date = 15, amount = 300, type = TransactionType.EXPENSE, category = "General", note = "Day 15 Boundary")
      ),
      exports
    )
  }

  @Test
  fun insertWithNonExistentCategoryId_throwsForeignKeyConstraint() = runTest {
    val invalidCategoryId = 999999L

    assertThrows(SQLiteConstraintException::class.java) {
      kotlinx.coroutines.runBlocking {
        transactionDao.insert(
          Transaction(amount = 100, type = TransactionType.EXPENSE, categoryId = invalidCategoryId, date = 10, note = "Invalid FK")
        )
      }
    }
  }

  @Test
  fun updateAndDeleteById_modifiesAndRemovesTransaction() = runTest {
    val id = transactionDao.insert(Transaction(amount = 100, type = TransactionType.EXPENSE, categoryId = defaultCategoryId, date = 10, note = "Initial"))
    val loaded = transactionDao.getByIdOnce(id)!!

    transactionDao.update(loaded.copy(amount = 250, note = "Modified", type = TransactionType.INCOME))
    val updated = transactionDao.getByIdOnce(id)!!
    assertEquals(250L, updated.amount)
    assertEquals("Modified", updated.note)
    assertEquals(TransactionType.INCOME, updated.type)

    transactionDao.deleteById(id)
    assertNull(transactionDao.getByIdOnce(id))
  }
}
