package com.financetracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.financetracker.app.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
  fun getAll(): Flow<List<Category>>

  @Query("SELECT * FROM categories WHERE id = :id")
  fun getById(id: Long): Flow<Category?>

  @Insert
  suspend fun insert(category: Category): Long

  @Update
  suspend fun update(category: Category)

  @Delete
  suspend fun delete(category: Category)

  @Query("SELECT COUNT(*) FROM categories")
  suspend fun count(): Int
}
