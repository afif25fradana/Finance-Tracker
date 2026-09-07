package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.financetracker.app.data.entity.RecurringItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringItemDao {
  @Query("SELECT * FROM recurring_items ORDER BY nextDueDate ASC")
  fun getAll(): Flow<List<RecurringItem>>

  @Query("SELECT * FROM recurring_items WHERE id = :id")
  fun getById(id: Long): Flow<RecurringItem?>

  @Insert
  suspend fun insert(item: RecurringItem): Long

  @Update
  suspend fun update(item: RecurringItem)

  @Delete
  suspend fun delete(item: RecurringItem)
}
