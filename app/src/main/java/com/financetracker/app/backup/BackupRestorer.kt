package com.financetracker.app.backup

import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import java.io.InputStream
import kotlinx.coroutines.CancellationException

class BackupRestorer(
  private val backupDao: BackupDao,
  private val cancelReminders: (List<Long>) -> Unit = {},
  private val rescheduleReminders: (List<RecurringItem>) -> Unit = {}
) {

  fun validateFromText(text: String): BackupResult =
    validateFromStream(text.byteInputStream(Charsets.UTF_8))

  fun validateFromStream(stream: InputStream): BackupResult =
    when (val decoded = BackupCodec.decode(stream)) {
      is BackupResult.Invalid -> decoded
      is BackupResult.Valid -> BackupValidator.validate(decoded.file)
    }

  suspend fun restore(file: BackupFile): BackupResult {
    val categories: List<Category>
    val recurringItems: List<RecurringItem>
    val transactions: List<Transaction>
    try {
      categories = file.categories.map { it.toEntity() }
      recurringItems = file.recurringItems.map { it.toEntity() }
      transactions = file.transactions.map { it.toEntity() }

      val existingRecurringIds = backupDao.getAllRecurringOnce().map { it.id }
      cancelReminders(existingRecurringIds)

      backupDao.replaceAll(categories, recurringItems, transactions)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return BackupResult.Invalid("database", "The restore could not be completed.")
    }
    runCatching { rescheduleReminders(recurringItems) }
    return BackupResult.Valid(file)
  }
}
