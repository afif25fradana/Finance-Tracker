package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction

@Dao
abstract class BackupDao {
  @Query("SELECT * FROM categories ORDER BY id ASC")
  abstract suspend fun getAllCategoriesOnce(): List<Category>

  @Query("SELECT * FROM transactions ORDER BY id ASC")
  abstract suspend fun getAllTransactionsOnce(): List<Transaction>

  @Query("SELECT * FROM recurring_items ORDER BY id ASC")
  abstract suspend fun getAllRecurringOnce(): List<RecurringItem>

  @Query("DELETE FROM transactions")
  abstract suspend fun clearTransactions()

  @Query("DELETE FROM recurring_items")
  abstract suspend fun clearRecurring()

  @Query("DELETE FROM categories")
  abstract suspend fun clearCategories()

  @Insert
  abstract suspend fun insertCategories(items: List<Category>)

  @Insert
  abstract suspend fun insertRecurring(items: List<RecurringItem>)

  @Insert
  abstract suspend fun insertTransactions(items: List<Transaction>)

  @RoomTransaction
  open suspend fun replaceAll(
    categories: List<Category>,
    recurringItems: List<RecurringItem>,
    transactions: List<Transaction>
  ) {
    clearTransactions()
    clearRecurring()
    clearCategories()
    insertCategories(categories)
    insertRecurring(recurringItems)
    insertTransactions(transactions)
  }
}
