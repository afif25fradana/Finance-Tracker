package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.financetracker.app.data.entity.Category
import kotlinx.coroutines.flow.Flow

data class CategoryRefCount(val categoryId: Long, val refCount: Int)

@Dao
abstract class CategoryDao {
  @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
  abstract fun getAll(): Flow<List<Category>>

  @Query("SELECT * FROM categories WHERE id = :id")
  abstract suspend fun getByIdOnce(id: Long): Category?

  @Insert
  abstract suspend fun insert(category: Category): Long

  @Update
  abstract suspend fun update(category: Category)

  @Delete
  abstract suspend fun delete(category: Category)

  @Query("SELECT categoryId, COUNT(*) AS refCount FROM transactions GROUP BY categoryId")
  abstract fun transactionCounts(): Flow<List<CategoryRefCount>>

  @Query("SELECT categoryId, COUNT(*) AS refCount FROM recurring_items GROUP BY categoryId")
  abstract fun recurringCounts(): Flow<List<CategoryRefCount>>

  @Query("UPDATE transactions SET categoryId = :newId WHERE categoryId = :oldId")
  abstract suspend fun reassignTransactions(oldId: Long, newId: Long)

  @Query("UPDATE recurring_items SET categoryId = :newId WHERE categoryId = :oldId")
  abstract suspend fun reassignRecurring(oldId: Long, newId: Long)

  @Query("DELETE FROM categories WHERE id = :id")
  abstract suspend fun deleteById(id: Long)

  // Reassign children before deleting so the FK RESTRICT constraint never fires.
  @Transaction
  open suspend fun reassignAndDelete(oldId: Long, newId: Long) {
    reassignTransactions(oldId, newId)
    reassignRecurring(oldId, newId)
    deleteById(oldId)
  }
}
