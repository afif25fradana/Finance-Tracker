package com.financetracker.app.backup

import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 1
const val BACKUP_CURRENCY = "IDR"

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
  val type: TransactionType,
  val color: Long,
  val icon: String,
  val isDefault: Boolean
)

@Serializable
data class BackupTransaction(
  val id: Long,
  val amount: Long,
  val type: TransactionType,
  val categoryId: Long,
  val date: Long,
  val note: String
)

@Serializable
data class BackupRecurringItem(
  val id: Long,
  val categoryId: Long,
  val amount: Long,
  val frequency: RecurringFrequency,
  val nextDueDate: Long
)

fun Category.toBackup(): BackupCategory = BackupCategory(id, name, type, color, icon, isDefault)

fun Transaction.toBackup(): BackupTransaction =
  BackupTransaction(id, amount, type, categoryId, date, note)

fun RecurringItem.toBackup(): BackupRecurringItem =
  BackupRecurringItem(id, categoryId, amount, frequency, nextDueDate)
