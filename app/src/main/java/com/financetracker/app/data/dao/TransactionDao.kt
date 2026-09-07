package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.financetracker.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
  fun getAll(): Flow<List<Transaction>>

  @Query("SELECT * FROM transactions WHERE id = :id")
  fun getById(id: Long): Flow<Transaction?>

  @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
  fun getByCategory(categoryId: Long): Flow<List<Transaction>>

  @Insert
  suspend fun insert(transaction: Transaction): Long

  @Update
  suspend fun update(transaction: Transaction)

  @Delete
  suspend fun delete(transaction: Transaction)
}
