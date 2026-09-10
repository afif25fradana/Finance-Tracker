package com.financetracker.app.backup

import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

  private val category = Category(
    id = 1,
    name = "Groceries",
    type = TransactionType.EXPENSE,
    color = 0xFF276A54,
    icon = "shopping_cart",
    isDefault = true
  )
  private val transaction = Transaction(
    id = 7,
    amount = 50000,
    type = TransactionType.EXPENSE,
    categoryId = 1,
    date = 20000,
    note = "weekly"
  )
  private val recurring = RecurringItem(
    id = 3,
    categoryId = 1,
    amount = 250000,
    frequency = RecurringFrequency.MONTHLY,
    nextDueDate = 20100
  )

  private fun encode(): String = encodeBackup(
    appVersion = "1.0",
    currency = "IDR",
    createdAt = "2026-09-11T00:00:00Z",
    categories = listOf(category.toBackup()),
    transactions = listOf(transaction.toBackup()),
    recurringItems = listOf(recurring.toBackup())
  )

  @Test
  fun encode_includesRequiredTopLevelFields() {
    val root = Json.parseToJsonElement(encode()).jsonObject

    assertEquals(1, root["schemaVersion"]!!.jsonPrimitive.content.toInt())
    assertEquals("1.0", root["appVersion"]!!.jsonPrimitive.content)
    assertEquals("IDR", root["currency"]!!.jsonPrimitive.content)
    assertEquals("2026-09-11T00:00:00Z", root["createdAt"]!!.jsonPrimitive.content)
    assertTrue(root.containsKey("categories"))
    assertTrue(root.containsKey("transactions"))
    assertTrue(root.containsKey("recurringItems"))
  }

  @Test
  fun encode_amountIsJsonNumberNotString() {
    val tx = Json.parseToJsonElement(encode()).jsonObject["transactions"]!!.jsonArray[0].jsonObject
    val amount = tx["amount"]!!.jsonPrimitive

    assertFalse(amount.isString)
    assertEquals(50000L, amount.content.toLong())
  }

  @Test
  fun encode_enumSerializedByName() {
    val root = Json.parseToJsonElement(encode()).jsonObject
    val tx = root["transactions"]!!.jsonArray[0].jsonObject
    val rec = root["recurringItems"]!!.jsonArray[0].jsonObject

    assertEquals("EXPENSE", tx["type"]!!.jsonPrimitive.content)
    assertEquals("MONTHLY", rec["frequency"]!!.jsonPrimitive.content)
  }

  @Test
  fun encode_preservesIds() {
    val tx = Json.parseToJsonElement(encode()).jsonObject["transactions"]!!.jsonArray[0].jsonObject

    assertEquals(7L, tx["id"]!!.jsonPrimitive.content.toLong())
    assertEquals(1L, tx["categoryId"]!!.jsonPrimitive.content.toLong())
  }

  @Test
  fun encode_emptyTablesProduceEmptyArrays() {
    val out = encodeBackup(
      appVersion = "1.0",
      currency = "IDR",
      createdAt = "2026-09-11T00:00:00Z",
      categories = emptyList(),
      transactions = emptyList(),
      recurringItems = emptyList()
    )
    val root = Json.parseToJsonElement(out).jsonObject

    assertTrue(root["categories"]!!.jsonArray.isEmpty())
    assertTrue(root["transactions"]!!.jsonArray.isEmpty())
    assertTrue(root["recurringItems"]!!.jsonArray.isEmpty())
  }
}
