package com.financetracker.app.data.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import org.robolectric.RuntimeEnvironment
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryDaoTest {

  private lateinit var db: AppDatabase
  private lateinit var categoryDao: CategoryDao
  private lateinit var transactionDao: TransactionDao
  private lateinit var recurringItemDao: RecurringItemDao

  @Before
  fun setUp() {
    val context: Context = RuntimeEnvironment.getApplication()
    db = AppDatabase.createInMemory(context, withDefaultSeed = false)
    categoryDao = db.categoryDao()
    transactionDao = db.transactionDao()
    recurringItemDao = db.recurringItemDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun insertAndGetByIdOnce_returnsInsertedCategory() = runTest {
    val cat = Category(name = "Salary", type = TransactionType.INCOME, color = 0xFF123456, icon = "payments", isDefault = true)
    val id = categoryDao.insert(cat)

    val loaded = categoryDao.getByIdOnce(id)
    assertNotNull(loaded)
    assertEquals(id, loaded!!.id)
    assertEquals("Salary", loaded.name)
    assertEquals(TransactionType.INCOME, loaded.type)
    assertEquals(0xFF123456, loaded.color)
    assertEquals("payments", loaded.icon)
    assertEquals(true, loaded.isDefault)
  }

  @Test
  fun getAll_ordersByDefaultDescThenNameAsc() = runTest {
    categoryDao.insert(Category(name = "Groceries", type = TransactionType.EXPENSE, color = 1, icon = "c1", isDefault = false))
    categoryDao.insert(Category(name = "Utilities", type = TransactionType.EXPENSE, color = 2, icon = "c2", isDefault = true))
    categoryDao.insert(Category(name = "Bills", type = TransactionType.EXPENSE, color = 3, icon = "c3", isDefault = true))
    categoryDao.insert(Category(name = "Entertainment", type = TransactionType.EXPENSE, color = 4, icon = "c4", isDefault = false))

    val list = categoryDao.getAll().first()
    assertEquals(listOf("Bills", "Utilities", "Entertainment", "Groceries"), list.map { it.name })
    assertEquals(listOf(true, true, false, false), list.map { it.isDefault })
  }

  @Test
  fun updateAndUnreferencedDelete_succeeds() = runTest {
    val id = categoryDao.insert(Category(name = "Food", type = TransactionType.EXPENSE, color = 1, icon = "f", isDefault = false))
    val loaded = categoryDao.getByIdOnce(id)!!

    categoryDao.update(loaded.copy(name = "Dining", color = 99))
    val updated = categoryDao.getByIdOnce(id)!!
    assertEquals("Dining", updated.name)
    assertEquals(99L, updated.color)

    categoryDao.deleteById(id)
    assertNull(categoryDao.getByIdOnce(id))
  }

  @Test
  fun deleteReferencedByTransaction_throwsForeignKeyConstraint() = runTest {
    val catId = categoryDao.insert(Category(name = "Rent", type = TransactionType.EXPENSE, color = 1, icon = "r", isDefault = true))
    transactionDao.insert(Transaction(amount = 1000, type = TransactionType.EXPENSE, categoryId = catId, date = 100, note = "Rent payment"))

    assertThrows(SQLiteConstraintException::class.java) {
      kotlinx.coroutines.runBlocking {
        categoryDao.deleteById(catId)
      }
    }
  }

  @Test
  fun transactionAndRecurringCounts_returnCorrectAggregates() = runTest {
    val cat1Id = categoryDao.insert(Category(name = "Cat1", type = TransactionType.EXPENSE, color = 1, icon = "1"))
    val cat2Id = categoryDao.insert(Category(name = "Cat2", type = TransactionType.EXPENSE, color = 2, icon = "2"))

    transactionDao.insert(Transaction(amount = 10, type = TransactionType.EXPENSE, categoryId = cat1Id, date = 10, note = "n1"))
    transactionDao.insert(Transaction(amount = 20, type = TransactionType.EXPENSE, categoryId = cat1Id, date = 20, note = "n2"))
    transactionDao.insert(Transaction(amount = 30, type = TransactionType.EXPENSE, categoryId = cat2Id, date = 30, note = "n3"))

    recurringItemDao.insert(RecurringItem(categoryId = cat1Id, amount = 100, frequency = RecurringFrequency.MONTHLY, nextDueDate = 50))

    val txCounts = categoryDao.transactionCounts().first().associate { it.categoryId to it.refCount }
    assertEquals(2, txCounts[cat1Id])
    assertEquals(1, txCounts[cat2Id])

    val recCounts = categoryDao.recurringCounts().first().associate { it.categoryId to it.refCount }
    assertEquals(1, recCounts[cat1Id])
    assertNull(recCounts[cat2Id])
  }

  @Test
  fun reassignAndDelete_reassignsChildrenAndDeletesOldCategorySafely() = runTest {
    val oldCatId = categoryDao.insert(Category(name = "Old", type = TransactionType.EXPENSE, color = 1, icon = "o"))
    val newCatId = categoryDao.insert(Category(name = "New", type = TransactionType.EXPENSE, color = 2, icon = "n"))

    val txId = transactionDao.insert(Transaction(amount = 500, type = TransactionType.EXPENSE, categoryId = oldCatId, date = 100, note = "Old tx"))
    val recId = recurringItemDao.insert(RecurringItem(categoryId = oldCatId, amount = 250, frequency = RecurringFrequency.WEEKLY, nextDueDate = 150))

    categoryDao.reassignAndDelete(oldId = oldCatId, newId = newCatId)

    assertNull(categoryDao.getByIdOnce(oldCatId))
    assertNotNull(categoryDao.getByIdOnce(newCatId))

    val updatedTx = transactionDao.getByIdOnce(txId)
    assertNotNull(updatedTx)
    assertEquals(newCatId, updatedTx!!.categoryId)

    val updatedRec = recurringItemDao.getByIdOnce(recId)
    assertNotNull(updatedRec)
    assertEquals(newCatId, updatedRec!!.categoryId)
  }
}
