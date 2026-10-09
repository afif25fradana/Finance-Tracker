package com.financetracker.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermText

@Composable
fun BackupRestoreRoute(onBack: () -> Unit) {
  val context = LocalContext.current
  val viewModel: BackupRestoreViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        BackupRestoreViewModel(
          context = context.applicationContext,
          backupDao = db.backupDao(),
          savedStateHandle = this.createSavedStateHandle()
        )
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  BackupRestoreScreen(
    state = state,
    suggestedFileName = viewModel::suggestedFileName,
    onBack = onBack,
    onCreateBackup = viewModel::createBackup,
    onRestoreFilePicked = viewModel::onRestoreFilePicked,
    onConfirmRestore = viewModel::confirmRestore,
    onCancelRestore = viewModel::cancelRestore
  )
}

@Composable
private fun BackupRestoreScreen(
  state: BackupRestoreUiState,
  suggestedFileName: () -> String,
  onBack: () -> Unit,
  onCreateBackup: (Uri) -> Unit,
  onRestoreFilePicked: (Uri) -> Unit,
  onConfirmRestore: () -> Unit,
  onCancelRestore: () -> Unit
) {
  val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    if (uri != null) onCreateBackup(uri)
  }
  val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) onRestoreFilePicked(uri)
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
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TermMuted,
            modifier = Modifier.size(18.dp)
          )
        }
        Text(
          text = "Backup & Restore",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TermText
        )
      }
    }

    item {
      ActionCard(
        title = "Create Backup",
        body = "Save all categories, transactions and recurring reminders to a JSON file. " +
          "Keep it somewhere safe so you can restore your data later.",
        actionLabel = "Create Backup",
        icon = Icons.Default.Save,
        accent = SignalPositive,
        enabled = !state.isWorking,
        onClick = {
          saveLauncher.launch(suggestedFileName())
        }
      )
    }

    item {
      ActionCard(
        title = "Restore Backup",
        body = "Replace all current data with the contents of a backup file. " +
          "You'll be asked to confirm before anything is changed.",
        actionLabel = "Choose Backup File",
        icon = Icons.Default.Restore,
        accent = SignalNegative,
        enabled = !state.isWorking,
        onClick = { openLauncher.launch(arrayOf("*/*")) }
      )
    }

    if (state.isWorking) {
      item {
        Text(
          text = "Working…",
          style = MaterialTheme.typography.bodySmall,
          color = TermMuted
        )
      }
    }

    val message = state.message
    if (message != null) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(2.dp),
          color = TermPanel,
          border = BorderStroke(1.dp, if (state.isError) SignalNegative else SignalPositive)
        ) {
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.isError) SignalNegative else SignalPositive,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          )
        }
      }
    }
  }

  val pending = state.pendingRestore
  if (pending != null) {
    AlertDialog(
      onDismissRequest = onCancelRestore,
      containerColor = TermPanel,
      titleContentColor = TermText,
      textContentColor = TermMuted,
      title = { Text("Restore backup?") },
      text = {
        Text(
          "Restoring will replace all current data. This cannot be undone.\n\n" +
            "This backup contains ${pending.categories.size} categories, " +
            "${pending.transactions.size} transactions, " +
            "${pending.recurringItems.size} recurring items."
        )
      },
      confirmButton = {
        TextButton(onClick = onConfirmRestore) {
          Text("Restore", color = SignalNegative, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = onCancelRestore) {
          Text("Cancel", color = TermMuted)
        }
      }
    )
  }
}

@Composable
private fun ActionCard(
  title: String,
  body: String,
  actionLabel: String,
  icon: ImageVector,
  accent: Color,
  enabled: Boolean,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = TermText
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = body,
        style = MaterialTheme.typography.labelSmall,
        color = TermMuted
      )
      Spacer(modifier = Modifier.height(14.dp))
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(2.dp),
        color = if (enabled) accent else TermPanel,
        border = if (enabled) null else BorderStroke(1.dp, TermBorder)
      ) {
        Row(
          modifier = Modifier.padding(vertical = 10.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) TermBg else TermMuted,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) TermBg else TermMuted,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
