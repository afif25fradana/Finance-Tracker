package com.financetracker.app.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val backupJson = Json {
  prettyPrint = true
}

fun encodeBackup(
  appVersion: String,
  currency: String,
  createdAt: String,
  categories: List<BackupCategory>,
  transactions: List<BackupTransaction>,
  recurringItems: List<BackupRecurringItem>
): String = backupJson.encodeToString(
  BackupFile(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    appVersion = appVersion,
    currency = currency,
    createdAt = createdAt,
    categories = categories,
    transactions = transactions,
    recurringItems = recurringItems
  )
)
