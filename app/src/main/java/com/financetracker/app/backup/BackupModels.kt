package com.financetracker.app.backup

import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 1
const val BACKUP_CURRENCY = "IDR"

sealed interface BackupResult {
  data class Valid(val file: BackupFile) : BackupResult
  data class Invalid(val field: String, val reason: String) : BackupResult
}

@Serializable
data class BackupFile(
  val schemaVersion: Int,
  val appVersion: String,
  val currency: String,
  val createdAt: String,
  val categories: List<BackupCategory>,
  val transactions: List<BackupTransaction>,
  val recurringItems: List<BackupRecurringItem>
)

@Serializable
data class BackupCategory(
  val id: Long,
  val name: String,
  val type: String,
  val color: Long,
  val icon: String,
  val isDefault: Boolean
)

@Serializable
data class BackupTransaction(
  val id: Long,
  val amount: Long,
  val type: String,
  val categoryId: Long,
  val date: Long,
  val note: String
)

@Serializable
data class BackupRecurringItem(
  val id: Long,
  val categoryId: Long,
  val amount: Long,
  val frequency: String,
  val nextDueDate: Long
)

fun Category.toBackup(): BackupCategory = BackupCategory(id, name, type.name, color, icon, isDefault)

fun Transaction.toBackup(): BackupTransaction =
  BackupTransaction(id, amount, type.name, categoryId, date, note)

fun RecurringItem.toBackup(): BackupRecurringItem =
  BackupRecurringItem(id, categoryId, amount, frequency.name, nextDueDate)

fun BackupCategory.toEntity(): Category =
  Category(id, name, TransactionType.valueOf(type), color, icon, isDefault)

fun BackupTransaction.toEntity(): Transaction =
  Transaction(id, amount, TransactionType.valueOf(type), categoryId, date, note)

fun BackupRecurringItem.toEntity(): RecurringItem =
  RecurringItem(id, categoryId, amount, RecurringFrequency.valueOf(frequency), nextDueDate)
