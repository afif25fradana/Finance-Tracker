package com.financetracker.app.ui.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.createSavedStateHandle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.CategoryChip
import com.financetracker.app.ui.components.MAX_NOTE_LENGTH
import com.financetracker.app.ui.components.TypeToggle
import com.financetracker.app.ui.components.epochDayToDisplay
import com.financetracker.app.ui.components.epochDayToUtcMillis
import com.financetracker.app.ui.components.formatAmountInput
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.components.utcMillisToEpochDay
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermPanelAlt
import com.financetracker.app.ui.theme.TermText

private val QUICK_ADD_AMOUNTS = listOf(10_000L, 25_000L, 50_000L, 100_000L, 500_000L)

@Composable
fun AddEditTransactionRoute(
  transactionId: Long?,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val viewModel: AddEditTransactionViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        AddEditTransactionViewModel(
          transactionDao = db.transactionDao(),
          categoryDao = db.categoryDao(),
          transactionId = transactionId,
          savedStateHandle = this.createSavedStateHandle()
        )
      }
    }
  )

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val saved by viewModel.saved.collectAsStateWithLifecycle()
  val deleted by viewModel.deleted.collectAsStateWithLifecycle()

  LaunchedEffect(saved) {
    if (saved) {
      viewModel.onSaveHandled()
      onBack()
    }
  }

  LaunchedEffect(deleted) {
    if (deleted) {
      viewModel.onDeleteHandled()
      onBack()
    }
  }

  AddEditTransactionScreen(
    state = state,
    onBack = onBack,
    onTypeSelected = viewModel::onTypeSelected,
    onAmountChange = viewModel::onAmountChange,
    onQuickAdd = viewModel::onQuickAdd,
    onNoteChange = viewModel::onNoteChange,
    onCategorySelected = viewModel::onCategorySelected,
    onDateSelected = viewModel::onDateSelected,
    onSave = viewModel::save,
    onDelete = viewModel::delete
  )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTransactionScreen(
  state: EntryUiState,
  onBack: () -> Unit,
  onTypeSelected: (TransactionType) -> Unit,
  onAmountChange: (String) -> Unit,
  onQuickAdd: (Long) -> Unit,
  onNoteChange: (String) -> Unit,
  onCategorySelected: (Long) -> Unit,
  onDateSelected: (Long) -> Unit,
  onSave: () -> Unit,
  onDelete: () -> Unit
) {
  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
  val accent = if (state.transactionType == TransactionType.EXPENSE) SignalNegative else SignalPositive
  val keyboardController = LocalSoftwareKeyboardController.current

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
      .background(TermBg),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TermMuted,
              modifier = Modifier.size(18.dp)
            )
          }
          Text(
            text = if (state.isEditing) "Edit Transaction" else "New Transaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TermText
          )
        }
        if (state.isEditing) {
          Surface(
            shape = RoundedCornerShape(2.dp),
            color = TermPanel,
            border = BorderStroke(1.dp, SignalNegative),
            modifier = Modifier.clickable { showDeleteConfirm = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = SignalNegative,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Delete", style = MaterialTheme.typography.labelSmall, color = SignalNegative)
            }
          }
        }
      }
    }

    item {
      TypeToggle(
        selected = state.transactionType,
        onSelected = onTypeSelected
      )
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
            text = "Amount",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Rp",
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              color = accent
            )
            Spacer(modifier = Modifier.width(6.dp))
            val formattedAmount = remember(state.amountText) { formatAmountInput(state.amountText) }
            BasicTextField(
              value = formattedAmount,
              onValueChange = onAmountChange,
                textStyle = TextStyle(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Bold,
                  color = TermText
                ),
                cursorBrush = SolidColor(SignalPositive),
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Number,
                  imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  Box {
                    if (state.amountText.isEmpty()) {
                      Text(
                        text = "0",
                        style = TextStyle(
                          fontFamily = FontFamily.Monospace,
                          fontSize = 28.sp,
                          fontWeight = FontWeight.Bold,
                          color = TermMuted
                        )
                      )
                    }
                    innerTextField()
                  }
                }
              )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(TermBorder)
          )
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            QUICK_ADD_AMOUNTS.forEach { amount ->
              Surface(
                shape = RoundedCornerShape(2.dp),
                color = TermBg,
                border = BorderStroke(1.dp, TermBorder),
                modifier = Modifier.clickable { onQuickAdd(amount) }
              ) {
                Text(
                  text = "+" + formatRupiah(amount),
                  style = MaterialTheme.typography.labelSmall,
                  color = TermMuted,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
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
          Text(text = "Note", style = MaterialTheme.typography.labelSmall, color = TermMuted)
          Spacer(modifier = Modifier.height(6.dp))
          BasicTextField(
            value = state.note,
            onValueChange = { onNoteChange(it.take(MAX_NOTE_LENGTH)) },
            textStyle = TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 14.sp,
              color = TermText
            ),
            cursorBrush = SolidColor(SignalPositive),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
              Box {
                if (state.note.isEmpty()) {
                  Text(
                    text = "Optional note...",
                    style = TextStyle(
                      fontFamily = FontFamily.Monospace,
                      fontSize = 14.sp,
                      color = TermMuted
                    )
                  )
                }
                innerTextField()
              }
            }
          )
        }
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
          Text(text = "Category", style = MaterialTheme.typography.labelSmall, color = TermMuted)
          Spacer(modifier = Modifier.height(10.dp))
          val categories = state.categories.filter { it.type == state.transactionType }
          if (categories.isEmpty()) {
            Text(
              text = "No ${state.transactionType.name.lowercase()} categories yet",
              style = MaterialTheme.typography.bodySmall,
              color = TermMuted
            )
          } else {
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              categories.forEach { cat ->
                CategoryChip(
                  category = cat,
                  selected = cat.id == state.selectedCategoryId,
                  selectedBorderColor = accent,
                  onClick = { onCategorySelected(cat.id) },
                  showIcon = true
                )
              }
            }
          }
        }
      }
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2.dp),
        color = TermPanel,
        border = BorderStroke(1.dp, TermBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Date", style = MaterialTheme.typography.labelSmall, color = TermMuted)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = epochDayToDisplay(state.dateEpochDay),
              style = MaterialTheme.typography.bodySmall,
              color = TermText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = null,
              tint = TermMuted,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    if (state.error != null) {
      item {
        Text(
          text = state.error.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = SignalNegative
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .clickable { onBack() },
          shape = RoundedCornerShape(2.dp),
          color = TermPanel,
          border = BorderStroke(1.dp, TermBorder)
        ) {
          Text(
            text = "Cancel",
            style = MaterialTheme.typography.bodyMedium,
            color = TermMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }
        Surface(
          modifier = Modifier
            .weight(1.5f)
            .clickable { onSave() },
          shape = RoundedCornerShape(2.dp),
          color = SignalPositive
        ) {
          Text(
            text = "Save",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TermBg,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = epochDayToUtcMillis(state.dateEpochDay)
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { onDateSelected(utcMillisToEpochDay(it)) }
          showDatePicker = false
        }) {
          Text("OK", color = SignalPositive)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel", color = TermMuted)
        }
      }
    ) {
      Surface(shape = RoundedCornerShape(2.dp), color = TermPanel) {
        DatePicker(state = datePickerState)
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      containerColor = TermPanel,
      titleContentColor = TermText,
      textContentColor = TermMuted,
      confirmButton = {
        TextButton(onClick = {
          showDeleteConfirm = false
          onDelete()
        }) {
          Text("Delete", color = SignalNegative)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel", color = TermMuted)
        }
      },
      title = { Text("Delete transaction?") },
      text = { Text("This cannot be undone.") }
    )
  }
}
