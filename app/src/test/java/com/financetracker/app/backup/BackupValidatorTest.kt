package com.financetracker.app.backup

import com.financetracker.app.data.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

  private fun category(id: Long = 1) =
    BackupCategory(id, "Groceries", TransactionType.EXPENSE.name, 255, "shopping_cart", true)

  private fun transaction(id: Long = 7, categoryId: Long = 1, type: String = TransactionType.EXPENSE.name) =
    BackupTransaction(id, 50000, type, categoryId, 20000, "weekly")

  private fun recurring(id: Long = 3, categoryId: Long = 1, frequency: String = "MONTHLY") =
    BackupRecurringItem(id, categoryId, 250000, frequency, 20100)

  private fun file(
    schemaVersion: Int = 1,
    currency: String = "IDR",
    categories: List<BackupCategory> = listOf(category()),
    transactions: List<BackupTransaction> = listOf(transaction()),
    recurringItems: List<BackupRecurringItem> = listOf(recurring())
  ) = BackupFile(
    schemaVersion = schemaVersion,
    appVersion = "1.0",
    currency = currency,
    createdAt = "2026-09-11T00:00:00Z",
    categories = categories,
    transactions = transactions,
    recurringItems = recurringItems
  )

  private fun invalid(result: BackupResult): BackupResult.Invalid {
    assertTrue("expected Invalid but was $result", result is BackupResult.Invalid)
    return result as BackupResult.Invalid
  }

  @Test
  fun validate_happyPath_isValid() {
    assertEquals(BackupResult.Valid(file()), BackupValidator.validate(file()))
  }

  @Test
  fun validate_unknownSchemaVersion_isInvalid() {
    val r = invalid(BackupValidator.validate(file(schemaVersion = 2)))

    assertEquals("schemaVersion", r.field)
  }

  @Test
  fun validate_wrongCurrency_isInvalid() {
    val r = invalid(BackupValidator.validate(file(currency = "USD")))

    assertEquals("currency", r.field)
  }

  @Test
  fun validate_zeroId_isInvalid() {
    val r = invalid(BackupValidator.validate(file(categories = listOf(category(id = 0)))))

    assertEquals("categories", r.field)
  }

  @Test
  fun validate_duplicateId_isInvalid() {
    val r = invalid(BackupValidator.validate(file(categories = listOf(category(1), category(1)))))

    assertEquals("categories", r.field)
  }

  @Test
  fun validate_transactionFkMismatch_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction(categoryId = 99)))))

    assertEquals("transactions[0].categoryId", r.field)
  }

  @Test
  fun validate_recurringFkMismatch_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring(categoryId = 99)))))

    assertEquals("recurringItems[0].categoryId", r.field)
  }

  @Test
  fun validate_invalidTransactionType_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction(type = "TRANSFER")))))

    assertEquals("transactions[0].type", r.field)
  }

  @Test
  fun validate_invalidFrequency_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring(frequency = "FORTNIGHTLY")))))

    assertEquals("recurringItems[0].frequency", r.field)
  }

  @Test
  fun validate_categoryNameTooLong_isInvalid() {
    val longName = "A".repeat(37)
    val r = invalid(BackupValidator.validate(file(categories = listOf(category().copy(name = longName)))))

    assertEquals("categories[0].name", r.field)
  }

  @Test
  fun validate_transactionAmountNonPositive_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction().copy(amount = 0)))))

    assertEquals("transactions[0].amount", r.field)
  }

  @Test
  fun validate_transactionNoteTooLong_isInvalid() {
    val longNote = "A".repeat(101)
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction().copy(note = longNote)))))

    assertEquals("transactions[0].note", r.field)
  }

  @Test
  fun validate_recurringAmountNonPositive_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring().copy(amount = -100)))))

    assertEquals("recurringItems[0].amount", r.field)
  }

  @Test
  fun validate_recurringAmountZero_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring().copy(amount = 0)))))

    assertEquals("recurringItems[0].amount", r.field)
  }

  @Test
  fun validate_transactionAmountNegative_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction().copy(amount = -50)))))

    assertEquals("transactions[0].amount", r.field)
  }

  @Test
  fun validate_invalidCategoryType_isInvalid() {
    val r = invalid(BackupValidator.validate(file(categories = listOf(category().copy(type = "TRANSFER")))))

    assertEquals("categories[0].type", r.field)
  }

  @Test
  fun validate_blankCategoryName_isInvalid() {
    val r = invalid(BackupValidator.validate(file(categories = listOf(category().copy(name = "   ")))))

    assertEquals("categories[0].name", r.field)
  }

  @Test
  fun validate_transactionDateNegative_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction().copy(date = -1L)))))

    assertEquals("transactions[0].date", r.field)
  }

  @Test
  fun validate_transactionDateTooLarge_isInvalid() {
    val r = invalid(BackupValidator.validate(file(transactions = listOf(transaction().copy(date = 730_501L)))))

    assertEquals("transactions[0].date", r.field)
  }

  @Test
  fun validate_recurringNextDueDateNegative_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring().copy(nextDueDate = -1L)))))

    assertEquals("recurringItems[0].nextDueDate", r.field)
  }

  @Test
  fun validate_recurringNextDueDateTooLarge_isInvalid() {
    val r = invalid(BackupValidator.validate(file(recurringItems = listOf(recurring().copy(nextDueDate = 730_501L)))))

    assertEquals("recurringItems[0].nextDueDate", r.field)
  }
}
