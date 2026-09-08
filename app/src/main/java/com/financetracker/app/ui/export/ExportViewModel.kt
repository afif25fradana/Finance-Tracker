package com.financetracker.app.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.export.exportToCsv
import com.financetracker.app.export.exportToJson
import com.financetracker.app.ui.components.epochDayToIso
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class ExportFormat(val label: String, val mime: String, val extension: String) {
  CSV("CSV", "text/csv", "csv"),
  JSON("JSON", "application/json", "json")
}

data class ExportUiState(
  val fromEpochDay: Long = LocalDate.now().withDayOfMonth(1).toEpochDay(),
  val toEpochDay: Long = todayEpochDay(),
  val format: ExportFormat = ExportFormat.CSV,
  val message: String? = null,
  val isError: Boolean = false
) {
  val suggestedFileName: String
    get() = "FinanceTrack_${epochDayToIso(fromEpochDay)}_${epochDayToIso(toEpochDay)}.${format.extension}"
}

class ExportViewModel(
  context: Context,
  private val transactionDao: TransactionDao
) : ViewModel() {

  private val contentResolver = context.applicationContext.contentResolver
  private val _uiState = MutableStateFlow(ExportUiState())
  val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

  fun onFromChange(epochDay: Long) {
    _uiState.value = _uiState.value.copy(fromEpochDay = epochDay, message = null)
  }

  fun onToChange(epochDay: Long) {
    _uiState.value = _uiState.value.copy(toEpochDay = epochDay, message = null)
  }

  fun onFormatChange(format: ExportFormat) {
    _uiState.value = _uiState.value.copy(format = format, message = null)
  }

  fun export(uri: Uri) {
    val state = _uiState.value
    if (state.fromEpochDay > state.toEpochDay) {
      _uiState.value = state.copy(message = "From date must be on or before To date.", isError = true)
      return
    }
    viewModelScope.launch {
      try {
        val rows = transactionDao.getBetweenOnce(state.fromEpochDay, state.toEpochDay)
        val content = when (state.format) {
          ExportFormat.CSV -> exportToCsv(rows)
          ExportFormat.JSON -> exportToJson(rows)
        }
        withContext(Dispatchers.IO) {
          contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("Could not open output file")
        }
        val noun = if (rows.size == 1) "transaction" else "transactions"
        _uiState.value = _uiState.value.copy(message = "Exported ${rows.size} $noun.", isError = false)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(message = "Export failed: ${e.message}", isError = true)
      }
    }
  }
}
