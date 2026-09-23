package com.financetracker.app.data.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupDaoTest {

  private lateinit var db: AppDatabase
  private lateinit var backupDao: BackupDao

  @Before
  fun setUp() {
    val context: Context = RuntimeEnvironment.getApplication()
    db = AppDatabase.createInMemory(context, withDefaultSeed = false)
    backupDao = db.backupDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun getAllSnapshots_returnsRowsOrderedByIdAsc() = runTest {
    val cat1 = Category(id = 1, name = "Groceries", type = TransactionType.EXPENSE, color = 1, icon = "c1")
    val cat2 = Category(id = 2, name = "Salary", type = TransactionType.INCOME, color = 2, icon = "c2")
    backupDao.insertCategories(listOf(cat2, cat1))

    val categories = backupDao.getAllCategoriesOnce()
    assertEquals(listOf(1L, 2L), categories.map { it.id })

    val tx1 = Transaction(id = 1, amount = 100, type = TransactionType.EXPENSE, categoryId = 1, date = 10, note = "t1")
    val tx2 = Transaction(id = 2, amount = 200, type = TransactionType.INCOME, categoryId = 2, date = 20, note = "t2")
    backupDao.insertTransactions(listOf(tx2, tx1))

    val transactions = backupDao.getAllTransactionsOnce()
    assertEquals(listOf(1L, 2L), transactions.map { it.id })

    val rec1 = RecurringItem(id = 1, categoryId = 1, amount = 50, frequency = RecurringFrequency.MONTHLY, nextDueDate = 30)
    val rec2 = RecurringItem(id = 2, categoryId = 2, amount = 500, frequency = RecurringFrequency.YEARLY, nextDueDate = 60)
    backupDao.insertRecurring(listOf(rec2, rec1))

    val recurring = backupDao.getAllRecurringOnce()
    assertEquals(listOf(1L, 2L), recurring.map { it.id })
  }

  @Test
  fun clearCategoriesDirectlyWithExistingTransactions_throwsForeignKeyConstraint() = runTest {
    val cat = Category(id = 1, name = "Rent", type = TransactionType.EXPENSE, color = 1, icon = "r")
    backupDao.insertCategories(listOf(cat))
    backupDao.insertTransactions(listOf(Transaction(id = 1, amount = 1000, type = TransactionType.EXPENSE, categoryId = 1, date = 10, note = "Rent")))

    assertThrows(SQLiteConstraintException::class.java) {
      kotlinx.coroutines.runBlocking {
        backupDao.clearCategories()
      }
    }
  }

  @Test
  fun replaceAll_clearsAndReplacesInForeignKeySafeOrder() = runTest {
    val oldCat = Category(id = 10, name = "OldCat", type = TransactionType.EXPENSE, color = 1, icon = "old")
    val oldTx = Transaction(id = 100, amount = 100, type = TransactionType.EXPENSE, categoryId = 10, date = 10, note = "OldTx")
    val oldRec = RecurringItem(id = 1000, categoryId = 10, amount = 50, frequency = RecurringFrequency.DAILY, nextDueDate = 15)

    backupDao.insertCategories(listOf(oldCat))
    backupDao.insertTransactions(listOf(oldTx))
    backupDao.insertRecurring(listOf(oldRec))

    val newCat = Category(id = 20, name = "NewCat", type = TransactionType.INCOME, color = 2, icon = "new")
    val newTx = Transaction(id = 200, amount = 200, type = TransactionType.INCOME, categoryId = 20, date = 20, note = "NewTx")
    val newRec = RecurringItem(id = 2000, categoryId = 20, amount = 75, frequency = RecurringFrequency.WEEKLY, nextDueDate = 25)

    backupDao.replaceAll(
      categories = listOf(newCat),
      recurringItems = listOf(newRec),
      transactions = listOf(newTx)
    )

    val currentCategories = backupDao.getAllCategoriesOnce()
    assertEquals(listOf(newCat), currentCategories)

    val currentTransactions = backupDao.getAllTransactionsOnce()
    assertEquals(listOf(newTx), currentTransactions)

    val currentRecurring = backupDao.getAllRecurringOnce()
    assertEquals(listOf(newRec), currentRecurring)
  }

  @Test
  fun replaceAll_withEmptyListsWipesAllDataCleanly() = runTest {
    val cat = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, color = 1, icon = "f")
    val tx = Transaction(id = 1, amount = 50, type = TransactionType.EXPENSE, categoryId = 1, date = 1, note = "t")
    val rec = RecurringItem(id = 1, categoryId = 1, amount = 20, frequency = RecurringFrequency.MONTHLY, nextDueDate = 5)

    backupDao.insertCategories(listOf(cat))
    backupDao.insertTransactions(listOf(tx))
    backupDao.insertRecurring(listOf(rec))

    backupDao.replaceAll(
      categories = emptyList(),
      recurringItems = emptyList(),
      transactions = emptyList()
    )

    assertTrue(backupDao.getAllCategoriesOnce().isEmpty())
    assertTrue(backupDao.getAllTransactionsOnce().isEmpty())
    assertTrue(backupDao.getAllRecurringOnce().isEmpty())
  }
}
