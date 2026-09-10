package com.financetracker.app.backup

import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.TransactionType

object BackupValidator {

  private val TRANSACTION_TYPES = TransactionType.entries.map { it.name }.toSet()
  private val FREQUENCIES = RecurringFrequency.entries.map { it.name }.toSet()

  fun validate(file: BackupFile): BackupResult {
    if (file.schemaVersion != BACKUP_SCHEMA_VERSION) {
      return BackupResult.Invalid(
        "schemaVersion",
        "Unsupported backup schema version ${file.schemaVersion}; this app supports $BACKUP_SCHEMA_VERSION."
      )
    }
    if (file.currency != BACKUP_CURRENCY) {
      return BackupResult.Invalid(
        "currency",
        "Unsupported currency \"${file.currency}\"; expected \"$BACKUP_CURRENCY\"."
      )
    }

    validateIds("categories", file.categories.map { it.id })?.let { return it }

    val categoryIds = file.categories.map { it.id }.toSet()

    validateIds("transactions", file.transactions.map { it.id })?.let { return it }
    file.transactions.forEachIndexed { index, tx ->
      if (tx.type !in TRANSACTION_TYPES) {
        return BackupResult.Invalid("transactions[$index].type", "Unknown transaction type \"${tx.type}\".")
      }
      if (tx.categoryId !in categoryIds) {
        return BackupResult.Invalid(
          "transactions[$index].categoryId",
          "categoryId ${tx.categoryId} is not present in this backup's categories."
        )
      }
    }

    validateIds("recurringItems", file.recurringItems.map { it.id })?.let { return it }
    file.recurringItems.forEachIndexed { index, item ->
      if (item.frequency !in FREQUENCIES) {
        return BackupResult.Invalid(
          "recurringItems[$index].frequency",
          "Unknown frequency \"${item.frequency}\"."
        )
      }
      if (item.categoryId !in categoryIds) {
        return BackupResult.Invalid(
          "recurringItems[$index].categoryId",
          "categoryId ${item.categoryId} is not present in this backup's categories."
        )
      }
    }

    return BackupResult.Valid(file)
  }

  private fun validateIds(arrayName: String, ids: List<Long>): BackupResult.Invalid? {
    for (id in ids) {
      if (id <= 0) return BackupResult.Invalid(arrayName, "Ids must be positive; found $id.")
    }
    val seen = HashSet<Long>()
    for (id in ids) {
      if (!seen.add(id)) return BackupResult.Invalid(arrayName, "Duplicate id $id.")
    }
    return null
  }
}
