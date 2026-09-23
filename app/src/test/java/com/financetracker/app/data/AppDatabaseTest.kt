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
}
