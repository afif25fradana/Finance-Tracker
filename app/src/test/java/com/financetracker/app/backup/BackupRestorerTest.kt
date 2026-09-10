package com.financetracker.app.backup

import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestorerTest {

  private class FakeBackupDao : BackupDao() {
    val ops = mutableListOf<String>()
    var categories = mutableListOf<Category>()
    var recurring = mutableListOf<RecurringItem>()
    var transactions = mutableListOf<Transaction>()

    override suspend fun getAllCategoriesOnce(): List<Category> = categories.toList()
    override suspend fun getAllTransactionsOnce(): List<Transaction> = transactions.toList()
    override suspend fun getAllRecurringOnce(): List<RecurringItem> = recurring.toList()

    override suspend fun insertCategories(items: List<Category>) {
      ops += "insertCategories"
      categories = items.toMutableList()
    }

    override suspend fun insertRecurring(items: List<RecurringItem>) {
      ops += "insertRecurring"
      recurring = items.toMutableList()
    }

    override suspend fun insertTransactions(items: List<Transaction>) {
      ops += "insertTransactions"
      transactions = items.toMutableList()
    }

    override suspend fun clearTransactions() {
      ops += "clearTransactions"
      transactions.clear()
    }

    override suspend fun clearRecurring() {
      ops += "clearRecurring"
      recurring.clear()
    }

    override suspend fun clearCategories() {
      ops += "clearCategories"
      categories.clear()
    }
  }

  private fun backupJson(): String = BackupCodec.encode(
    appVersion = "1.0",
    currency = "IDR",
    createdAt = "2026-09-11T00:00:00Z",
    categories = listOf(BackupCategory(1, "Groceries", "EXPENSE", 255, "shopping_cart", true)),
    transactions = listOf(BackupTransaction(7, 50000, "EXPENSE", 1, 20000, "weekly")),
    recurringItems = listOf(BackupRecurringItem(3, 1, 250000, "MONTHLY", 20100))
  )

  private fun emptyJson(): String = BackupCodec.encode(
    appVersion = "1.0",
    currency = "IDR",
    createdAt = "2026-09-11T00:00:00Z",
    categories = emptyList(),
    transactions = emptyList(),
    recurringItems = emptyList()
  )

  private suspend fun BackupRestorer.restoreFromText(text: String): BackupResult =
    when (val validated = validateFromText(text)) {
      is BackupResult.Invalid -> validated
      is BackupResult.Valid -> restore(validated.file)
    }

  @Test
  fun restore_replacesAllDataInFkSafeOrderAndPreservesIds() = runBlocking {
    val dao = FakeBackupDao().apply {
      categories.add(Category(99, "Old", TransactionType.EXPENSE, 1, "old", false))
      transactions.add(Transaction(98, 1, TransactionType.EXPENSE, 99, 1, "old"))
    }
    val rescheduled = mutableListOf<List<RecurringItem>>()
    val restorer = BackupRestorer(dao) { rescheduled += it }

    val result = restorer.restoreFromText(backupJson())

    assertTrue(result is BackupResult.Valid)
    assertEquals(
      listOf(
        "clearTransactions",
        "clearRecurring",
        "clearCategories",
        "insertCategories",
        "insertRecurring",
        "insertTransactions"
      ),
      dao.ops
    )
    assertEquals(listOf(1L), dao.categories.map { it.id })
    assertEquals(listOf(7L), dao.transactions.map { it.id })
    assertEquals(listOf(3L), dao.recurring.map { it.id })
    assertEquals(listOf(3L), rescheduled.single().map { it.id })
  }

  @Test
  fun restore_transactionCategoryIdResolvesInRestoredCategories() = runBlocking {
    val dao = FakeBackupDao()
    BackupRestorer(dao) {}.restoreFromText(backupJson())

    val categoryIds = dao.categories.map { it.id }.toSet()

    assertTrue(dao.transactions.isNotEmpty())
    assertTrue(dao.transactions.all { it.categoryId in categoryIds })
  }

  @Test
  fun restore_malformedFile_doesNotTouchDatabase() = runBlocking {
    val dao = FakeBackupDao()
    val result = BackupRestorer(dao) {}.restoreFromText("{ not a backup")

    assertTrue(result is BackupResult.Invalid)
    assertTrue(dao.ops.isEmpty())
  }

  @Test
  fun restore_validationFailure_doesNotTouchDatabase() = runBlocking {
    val dao = FakeBackupDao()
    val badCurrency = BackupCodec.encode(
      appVersion = "1.0",
      currency = "USD",
      createdAt = "2026-09-11T00:00:00Z",
      categories = emptyList(),
      transactions = emptyList(),
      recurringItems = emptyList()
    )

    val result = BackupRestorer(dao) {}.restoreFromText(badCurrency)

    assertTrue(result is BackupResult.Invalid)
    assertTrue(dao.ops.isEmpty())
  }

  @Test
  fun restore_emptyBackup_clearsEverything() = runBlocking {
    val dao = FakeBackupDao().apply {
      categories.add(Category(99, "Old", TransactionType.EXPENSE, 1, "old", false))
      transactions.add(Transaction(98, 1, TransactionType.EXPENSE, 99, 1, "old"))
    }

    BackupRestorer(dao) {}.restoreFromText(emptyJson())

    assertTrue(dao.categories.isEmpty())
    assertTrue(dao.transactions.isEmpty())
    assertTrue(dao.recurring.isEmpty())
  }
}
