package com.financetracker.app.ui.backup

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.backup.BACKUP_CURRENCY
import com.financetracker.app.backup.BackupCodec
import com.financetracker.app.backup.BackupFile
import com.financetracker.app.backup.BackupRestorer
import com.financetracker.app.backup.BackupResult
import com.financetracker.app.backup.toBackup
import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.reminder.ReminderScheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupRestoreUiState(
  val isWorking: Boolean = false,
  val pendingRestore: BackupFile? = null,
  val message: String? = null,
  val isError: Boolean = false
)

private val FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

class BackupRestoreViewModel(
  context: Context,
  private val backupDao: BackupDao,
  private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

  companion object {
    private const val KEY_MESSAGE = "backup_message"
    private const val KEY_IS_ERROR = "backup_is_error"
  }

  private val appContext = context.applicationContext
  private val contentResolver = appContext.contentResolver
  private val restorer = BackupRestorer(
    backupDao = backupDao,
    rescheduleReminders = { items ->
      ReminderScheduler.rescheduleAll(appContext, items)
    },
    cancelReminders = { ids ->
      ReminderScheduler.cancelAll(appContext, ids)
    }
  )

  private val _uiState = MutableStateFlow(
    BackupRestoreUiState(
      message = savedStateHandle?.get<String>(KEY_MESSAGE),
      isError = savedStateHandle?.get<Boolean>(KEY_IS_ERROR) ?: false
    )
  )
  val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

  fun suggestedFileName(): String =
    "FinanceTrack_Backup_${LocalDateTime.now().format(FILE_NAME_FORMATTER)}.json"

  fun createBackup(uri: Uri) {
    if (_uiState.value.isWorking) return
    viewModelScope.launch {
      _uiState.update { it.copy(isWorking = true, message = null) }
      try {
        val categories = withContext(Dispatchers.IO) { backupDao.getAllCategoriesOnce() }
        val transactions = withContext(Dispatchers.IO) { backupDao.getAllTransactionsOnce() }
        val recurring = withContext(Dispatchers.IO) { backupDao.getAllRecurringOnce() }
        val json = BackupCodec.encode(
          appVersion = appVersion(),
          currency = BACKUP_CURRENCY,
          createdAt = Instant.now().toString(),
          categories = categories.map { it.toBackup() },
          transactions = transactions.map { it.toBackup() },
          recurringItems = recurring.map { it.toBackup() }
        )
        withContext(Dispatchers.IO) {
          contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("Could not open the selected file for writing.")
        }
        val msg = "Backup created: ${transactions.size} transaction(s)."
        savedStateHandle?.set(KEY_MESSAGE, msg)
        savedStateHandle?.set(KEY_IS_ERROR, false)
        _uiState.update {
          it.copy(
            isWorking = false,
            message = msg,
            isError = false
          )
        }
      } catch (e: Exception) {
        val msg = "Backup failed: ${e.message}"
        savedStateHandle?.set(KEY_MESSAGE, msg)
        savedStateHandle?.set(KEY_IS_ERROR, true)
        _uiState.update {
          it.copy(isWorking = false, message = msg, isError = true)
        }
      }
    }
  }

  fun onRestoreFilePicked(uri: Uri) {
    if (_uiState.value.isWorking) return
    viewModelScope.launch {
      _uiState.update { it.copy(isWorking = true, message = null) }
      try {
        val text = withContext(Dispatchers.IO) {
          contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Could not open the selected file.")
        }
        when (val result = restorer.validateFromText(text)) {
          is BackupResult.Valid ->
            _uiState.update { it.copy(isWorking = false, pendingRestore = result.file) }
          is BackupResult.Invalid -> {
            val msg = result.displayMessage()
            savedStateHandle?.set(KEY_MESSAGE, msg)
            savedStateHandle?.set(KEY_IS_ERROR, true)
            _uiState.update { it.copy(isWorking = false, message = msg, isError = true) }
          }
        }
      } catch (e: Exception) {
        val msg = "Could not read the file: ${e.message}"
        savedStateHandle?.set(KEY_MESSAGE, msg)
        savedStateHandle?.set(KEY_IS_ERROR, true)
        _uiState.update {
          it.copy(isWorking = false, message = msg, isError = true)
        }
      }
    }
  }

  fun confirmRestore() {
    val file = _uiState.value.pendingRestore ?: return
    if (_uiState.value.isWorking) return
    viewModelScope.launch {
      _uiState.update { it.copy(isWorking = true, pendingRestore = null, message = null) }
      when (val result = withContext(Dispatchers.IO) { restorer.restore(file) }) {
        is BackupResult.Valid -> {
          val msg = "Restore completed. ${file.transactions.size} transactions restored."
          savedStateHandle?.set(KEY_MESSAGE, msg)
          savedStateHandle?.set(KEY_IS_ERROR, false)
          _uiState.update {
            it.copy(
              isWorking = false,
              message = msg,
              isError = false
            )
          }
        }
        is BackupResult.Invalid -> {
          val msg = result.displayMessage()
          savedStateHandle?.set(KEY_MESSAGE, msg)
          savedStateHandle?.set(KEY_IS_ERROR, true)
          _uiState.update { it.copy(isWorking = false, message = msg, isError = true) }
        }
      }
    }
  }

  fun cancelRestore() {
    _uiState.update { it.copy(pendingRestore = null) }
  }

  private fun appVersion(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    }
    return info.versionName ?: "unknown"
  }
}

private fun BackupResult.Invalid.displayMessage(): String = "$field: $reason"
