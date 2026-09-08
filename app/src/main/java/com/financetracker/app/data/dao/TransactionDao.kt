package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class TransactionExport(
  val date: Long,
  val amount: Long,
  val type: TransactionType,
  val category: String,
  val note: String
)

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
  fun getAll(): Flow<List<Transaction>>

  @Query("SELECT * FROM transactions WHERE id = :id")
  fun getById(id: Long): Flow<Transaction?>

  @Query(
    """
    SELECT t.date AS date, t.amount AS amount, t.type AS type,
           COALESCE(c.name, '') AS category, t.note AS note
    FROM transactions t LEFT JOIN categories c ON c.id = t.categoryId
    WHERE t.date BETWEEN :fromEpochDay AND :toEpochDay
    ORDER BY t.date ASC, t.id ASC
    """
  )
  suspend fun getBetweenOnce(fromEpochDay: Long, toEpochDay: Long): List<TransactionExport>

  @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
  fun getByCategory(categoryId: Long): Flow<List<Transaction>>

  @Insert
  suspend fun insert(transaction: Transaction): Long

  @Update
  suspend fun update(transaction: Transaction)

  @Delete
  suspend fun delete(transaction: Transaction)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: Long)
}
