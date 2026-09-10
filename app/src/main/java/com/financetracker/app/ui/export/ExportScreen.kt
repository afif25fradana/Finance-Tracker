package com.financetracker.app.ui.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.ui.components.CreateDocumentWithName
import com.financetracker.app.ui.components.epochDayToDisplay
import com.financetracker.app.ui.components.epochDayToUtcMillis
import com.financetracker.app.ui.components.utcMillisToEpochDay
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermPanelAlt
import com.financetracker.app.ui.theme.TermText

@Composable
fun ExportRoute(onBack: () -> Unit) {
  val context = LocalContext.current
  val viewModel: ExportViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        ExportViewModel(
          context = context.applicationContext,
          transactionDao = db.transactionDao()
        )
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  ExportScreen(
    state = state,
    onBack = onBack,
    onFromChange = viewModel::onFromChange,
    onToChange = viewModel::onToChange,
    onFormatChange = viewModel::onFormatChange,
    onExport = viewModel::export
  )
}

private enum class PickerTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreen(
  state: ExportUiState,
  onBack: () -> Unit,
  onFromChange: (Long) -> Unit,
  onToChange: (Long) -> Unit,
  onFormatChange: (ExportFormat) -> Unit,
  onExport: (Uri) -> Unit
) {
  var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

  val saveLauncher = rememberLauncherForActivityResult(CreateDocumentWithName()) { uri ->
    if (uri != null) onExport(uri)
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(TermBg),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TermMuted,
            modifier = Modifier.size(18.dp)
          )
        }
        Text(
          text = "Export",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TermText
        )
      }
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2.dp),
        color = TermPanel,
        border = BorderStroke(1.dp, TermBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Date range",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            RangeField(
              label = "From",
              epochDay = state.fromEpochDay,
              onPick = { pickerTarget = PickerTarget.FROM },
              modifier = Modifier.weight(1f)
            )
            RangeField(
              label = "To",
              epochDay = state.toEpochDay,
              onPick = { pickerTarget = PickerTarget.TO },
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Format",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ExportFormat.entries.forEach { format ->
              val selected = state.format == format
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clickable { onFormatChange(format) },
                shape = RoundedCornerShape(2.dp),
                color = if (selected) TermPanelAlt else TermBg,
                border = BorderStroke(1.dp, if (selected) SignalPositive else TermBorder)
              ) {
                Text(
                  text = format.label,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                  color = if (selected) TermText else TermMuted,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                saveLauncher.launch(
                  CreateDocumentWithName.Request(fileName = state.suggestedFileName, mimeType = state.format.mime)
                )
              },
            shape = RoundedCornerShape(2.dp),
            color = SignalPositive
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Save, contentDescription = null, tint = TermBg, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Export ${state.format.label}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TermBg
              )
            }
          }

          val message = state.message
          if (message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = message,
              style = MaterialTheme.typography.bodySmall,
              color = if (state.isError) SignalNegative else SignalPositive
            )
          }
        }
      }
    }

    item {
      Text(
        text = "The save dialog pre-fills the name \"${state.suggestedFileName}\" — pick a location and save, or rename it there.",
        style = MaterialTheme.typography.labelSmall,
        color = TermMuted
      )
    }
  }

  val target = pickerTarget
  if (target != null) {
    val current = if (target == PickerTarget.FROM) state.fromEpochDay else state.toEpochDay
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = epochDayToUtcMillis(current)
    )
    DatePickerDialog(
      onDismissRequest = { pickerTarget = null },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            val epochDay = utcMillisToEpochDay(millis)
            if (target == PickerTarget.FROM) onFromChange(epochDay) else onToChange(epochDay)
          }
          pickerTarget = null
        }) {
          Text("OK", color = SignalPositive)
        }
      },
      dismissButton = {
        TextButton(onClick = { pickerTarget = null }) {
          Text("Cancel", color = TermMuted)
        }
      }
    ) {
      Surface(shape = RoundedCornerShape(2.dp), color = TermPanel) {
        DatePicker(state = datePickerState)
      }
    }
  }
}

@Composable
private fun RangeField(
  label: String,
  epochDay: Long,
  onPick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TermMuted
    )
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onPick),
      shape = RoundedCornerShape(2.dp),
      color = TermPanelAlt,
      border = BorderStroke(1.dp, TermBorder)
    ) {
      Text(
        text = epochDayToDisplay(epochDay),
        style = MaterialTheme.typography.bodySmall,
        color = TermText,
        modifier = Modifier.padding(10.dp)
      )
    }
  }
}
