package com.financetracker.app.data

import android.content.Context
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

  private var db: AppDatabase? = null

  @After
  fun tearDown() {
    db?.close()
  }

  @Test
  fun createInMemory_withDefaultSeedFalse_createsEmptyDatabase() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    val database = AppDatabase.createInMemory(context, withDefaultSeed = false)
    db = database

    val categories = database.categoryDao().getAll().first()
    assertTrue(categories.isEmpty())
  }

  @Test
  fun createInMemory_withDefaultSeedTrue_seedsNineDefaultCategories() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    val database = AppDatabase.createInMemory(context, withDefaultSeed = true)
    db = database

    val categories = database.categoryDao().getAll().first()
    assertEquals(9, categories.size)

    val expectedCategories = listOf(
      "Bills & Utilities",
      "Dining & Cafes",
      "Entertainment",
      "Groceries",
      "Health & Wellness",
      "Rent & Housing",
      "Salary & Income",
      "Tech & Subs",
      "Transport & Fuel"
    )
    assertEquals(expectedCategories, categories.map { it.name })
    assertTrue(categories.all { it.isDefault })

    val salaryCat = categories.first { it.name == "Salary & Income" }
    assertEquals(TransactionType.INCOME, salaryCat.type)
    assertEquals(0xFF1B5543, salaryCat.color)
    assertEquals("payments", salaryCat.icon)

    val groceriesCat = categories.first { it.name == "Groceries" }
    assertEquals(TransactionType.EXPENSE, groceriesCat.type)
    assertEquals(0xFF276A54, groceriesCat.color)
    assertEquals("shopping_cart", groceriesCat.icon)
  }

  @Test
  fun migration_1_to_2_preservesExistingDataAndCreatesCompositeIndex() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    val dbName = "migration_test.db"
    context.deleteDatabase(dbName)

    // 1. Create a version 1 database using raw SQLite helper
    val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
      androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
          override fun onCreate(sdb: androidx.sqlite.db.SupportSQLiteDatabase) {
            sdb.execSQL(
              """
              CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                color INTEGER NOT NULL,
                icon TEXT NOT NULL,
                isDefault INTEGER NOT NULL
              )
              """.trimIndent()
            )
            sdb.execSQL(
              """
              CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount INTEGER NOT NULL,
                type TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                date INTEGER NOT NULL,
                note TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
              )
              """.trimIndent()
            )
            sdb.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
            sdb.execSQL(
              """
              CREATE TABLE IF NOT EXISTS recurring_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                frequency TEXT NOT NULL,
                nextDueDate INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
              )
              """.trimIndent()
            )
            sdb.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_items_categoryId ON recurring_items(categoryId)")
            sdb.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            sdb.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'legacy_v1_hash')")
          }

          override fun onUpgrade(sdb: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
        .build()
    )

    val v1Db = helper.writableDatabase
    v1Db.execSQL("INSERT INTO categories (id, name, type, color, icon, isDefault) VALUES (1, 'Food', 'EXPENSE', 12345, 'food', 0)")
    v1Db.execSQL("INSERT INTO transactions (id, amount, type, categoryId, date, note) VALUES (100, 50000, 'EXPENSE', 1, 20720, 'Lunch')")
    v1Db.execSQL("INSERT INTO transactions (id, amount, type, categoryId, date, note) VALUES (101, 75000, 'EXPENSE', 1, 20721, 'Dinner')")
    v1Db.close()
    helper.close()

    // 2. Open via Room at version 2 with MIGRATION_1_2
    val migratedDb = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, dbName)
      .addMigrations(AppDatabase.MIGRATION_1_2)
      .allowMainThreadQueries()
      .build()
    db = migratedDb

    // 3. Verify all existing rows survive untouched
    val txs = migratedDb.transactionDao().getAll().first()
    assertEquals(2, txs.size)
    assertEquals(101L, txs[0].id)
    assertEquals(75000L, txs[0].amount)
    assertEquals(20721L, txs[0].date)
    assertEquals("Dinner", txs[0].note)

    assertEquals(100L, txs[1].id)
    assertEquals(50000L, txs[1].amount)
    assertEquals(20720L, txs[1].date)
    assertEquals("Lunch", txs[1].note)

    // 4. Verify index exists in sqlite_master
    val cursor = migratedDb.openHelper.writableDatabase.query(
      "SELECT name, sql FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_date_id'"
    )
    cursor.use {
      assertTrue("Index index_transactions_date_id must exist in sqlite_master", it.moveToFirst())
      val indexName = it.getString(0)
      val sql = it.getString(1)
      assertEquals("index_transactions_date_id", indexName)
      assertTrue("SQL must define index on date, id", sql.contains("transactions") && sql.contains("date") && sql.contains("id"))
    }

    context.deleteDatabase(dbName)
  }

  @Test
  fun explainQueryPlan_usesCompositeIndexForDateQueries() = runTest {
    val context: Context = RuntimeEnvironment.getApplication()
    val database = AppDatabase.createInMemory(context, withDefaultSeed = true)
    db = database

    val rawDb = database.openHelper.writableDatabase

    // 1. Verify getAll() query plan uses composite index
    val cursorGetAll = rawDb.query("EXPLAIN QUERY PLAN SELECT * FROM transactions ORDER BY date DESC, id DESC")
    val getAllPlan = buildList {
      cursorGetAll.use {
        while (it.moveToNext()) {
          add(it.getString(3))
        }
      }
    }
    println("Actual compiled Room query plan (getAll): $getAllPlan")
    assertTrue(
      "Query plan must use index_transactions_date_id index",
      getAllPlan.any { it.contains("index_transactions_date_id") }
    )
    assertTrue(
      "Query plan must not require temporary B-tree for sorting",
      getAllPlan.none { it.contains("USE TEMP B-TREE") }
    )

    // 2. Verify getBetweenOnce() export query plan uses composite index
    val cursorBetween = rawDb.query(
      """
      EXPLAIN QUERY PLAN
      SELECT t.date AS date, t.amount AS amount, t.type AS type,
             COALESCE(c.name, '') AS category, t.note AS note
      FROM transactions t LEFT JOIN categories c ON c.id = t.categoryId
      WHERE t.date BETWEEN 20000 AND 20100
      ORDER BY t.date ASC, t.id ASC
      """.trimIndent()
    )
    val betweenPlan = buildList {
      cursorBetween.use {
        while (it.moveToNext()) {
          add(it.getString(3))
        }
      }
    }
    println("Actual compiled Room query plan (getBetweenOnce): $betweenPlan")
    assertTrue(
      "getBetweenOnce must use index_transactions_date_id",
      betweenPlan.any { it.contains("index_transactions_date_id") }
    )
    assertTrue(
      "getBetweenOnce must not perform a full table scan on t",
      betweenPlan.none { it == "SCAN t" || it == "SCAN transactions" }
    )
  }
}

