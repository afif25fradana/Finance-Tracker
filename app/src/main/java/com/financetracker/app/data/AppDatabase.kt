package com.financetracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.RecurringItemDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType

@Database(
  entities = [Category::class, Transaction::class, RecurringItem::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun categoryDao(): CategoryDao
  abstract fun transactionDao(): TransactionDao
  abstract fun recurringItemDao(): RecurringItemDao
  abstract fun backupDao(): BackupDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "finance_tracker.db"
        )
          .addCallback(DEFAULT_CATEGORY_SEED)
          .build()
          .also { INSTANCE = it }
      }
  }
}

private val DEFAULT_CATEGORY_SEED = object : RoomDatabase.Callback() {
  override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    for (category in DEFAULT_CATEGORIES) {
      db.execSQL(
        "INSERT INTO categories (name, type, color, icon, isDefault) VALUES (?, ?, ?, ?, ?)",
        arrayOf(category.name, category.type.name, category.color, category.icon, 1)
      )
    }
  }
}

private data class SeedCategory(
  val name: String,
  val type: TransactionType,
  val color: Long,
  val icon: String
)

private val DEFAULT_CATEGORIES = listOf(
  SeedCategory("Salary & Income", TransactionType.INCOME, 0xFF1B5543, "payments"),
  SeedCategory("Rent & Housing", TransactionType.EXPENSE, 0xFF4245B3, "home"),
  SeedCategory("Groceries", TransactionType.EXPENSE, 0xFF276A54, "shopping_cart"),
  SeedCategory("Dining & Cafes", TransactionType.EXPENSE, 0xFFA5384B, "restaurant"),
  SeedCategory("Transport & Fuel", TransactionType.EXPENSE, 0xFF8B682D, "directions_car"),
  SeedCategory("Entertainment", TransactionType.EXPENSE, 0xFF793CB3, "movie"),
  SeedCategory("Bills & Utilities", TransactionType.EXPENSE, 0xFF266B77, "bolt"),
  SeedCategory("Tech & Subs", TransactionType.EXPENSE, 0xFF3761A5, "devices"),
  SeedCategory("Health & Wellness", TransactionType.EXPENSE, 0xFFA03D6E, "medical_services")
)
