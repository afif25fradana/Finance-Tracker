package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction

@Dao
interface BackupDao {
  @Query("SELECT * FROM categories ORDER BY id ASC")
  suspend fun getAllCategoriesOnce(): List<Category>

  @Query("SELECT * FROM transactions ORDER BY id ASC")
  suspend fun getAllTransactionsOnce(): List<Transaction>

  @Query("SELECT * FROM recurring_items ORDER BY id ASC")
  suspend fun getAllRecurringOnce(): List<RecurringItem>
}
