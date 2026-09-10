package com.financetracker.app.backup

import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction

class BackupRestorer(
  private val backupDao: BackupDao,
  private val rescheduleReminders: (List<RecurringItem>) -> Unit
) {

  fun validateFromText(text: String): BackupResult =
    when (val decoded = BackupCodec.decode(text)) {
      is BackupResult.Invalid -> decoded
      is BackupResult.Valid -> BackupValidator.validate(decoded.file)
    }

  suspend fun restoreFromText(text: String): BackupResult =
    when (val validated = validateFromText(text)) {
      is BackupResult.Invalid -> validated
      is BackupResult.Valid -> restore(validated.file)
    }

  suspend fun restore(file: BackupFile): BackupResult {
    val categories: List<Category>
    val recurringItems: List<RecurringItem>
    val transactions: List<Transaction>
    try {
      categories = file.categories.map { it.toEntity() }
      recurringItems = file.recurringItems.map { it.toEntity() }
      transactions = file.transactions.map { it.toEntity() }
      backupDao.replaceAll(categories, recurringItems, transactions)
    } catch (e: Exception) {
      return BackupResult.Invalid("database", e.message ?: "The restore could not be completed.")
    }
    rescheduleReminders(recurringItems)
    return BackupResult.Valid(file)
  }
}
