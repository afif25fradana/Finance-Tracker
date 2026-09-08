package com.financetracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.RecurringItemDao
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType

class Converters {
  @TypeConverter
  fun fromTransactionType(type: TransactionType): String = type.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

  @TypeConverter
  fun fromRecurringFrequency(frequency: RecurringFrequency): String = frequency.name

  @TypeConverter
  fun toRecurringFrequency(value: String): RecurringFrequency = RecurringFrequency.valueOf(value)
}

@Database(
  entities = [Category::class, Transaction::class, RecurringItem::class],
  version = 1,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun categoryDao(): CategoryDao
  abstract fun transactionDao(): TransactionDao
  abstract fun recurringItemDao(): RecurringItemDao

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
  SeedCategory("Salary & Income", TransactionType.INCOME, 0xFF059669, "payments"),
  SeedCategory("Rent & Housing", TransactionType.EXPENSE, 0xFF6366F1, "home"),
  SeedCategory("Groceries", TransactionType.EXPENSE, 0xFF10B981, "shopping_cart"),
  SeedCategory("Dining & Cafes", TransactionType.EXPENSE, 0xFFF43F5E, "restaurant"),
  SeedCategory("Transport & Fuel", TransactionType.EXPENSE, 0xFFF59E0B, "directions_car"),
  SeedCategory("Entertainment", TransactionType.EXPENSE, 0xFFA855F7, "movie"),
  SeedCategory("Bills & Utilities", TransactionType.EXPENSE, 0xFF06B6D4, "bolt"),
  SeedCategory("Tech & Subs", TransactionType.EXPENSE, 0xFF3B82F6, "devices"),
  SeedCategory("Health & Wellness", TransactionType.EXPENSE, 0xFFEC4899, "medical_services")
)
