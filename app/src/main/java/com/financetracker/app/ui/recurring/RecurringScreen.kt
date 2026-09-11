package com.financetracker.app.ui.recurring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.CategoryChip
import com.financetracker.app.ui.components.CategoryIconTile
import com.financetracker.app.ui.components.TypeToggle
import com.financetracker.app.ui.components.digitsOnly
import com.financetracker.app.ui.components.epochDayToDisplay
import com.financetracker.app.ui.components.epochDayToUtcMillis
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.components.parseAmount
import com.financetracker.app.ui.components.todayEpochDay
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
fun RecurringRoute(onBack: () -> Unit) {
  val context = LocalContext.current
  val viewModel: RecurringViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        RecurringViewModel(
          context = context.applicationContext,
          recurringItemDao = db.recurringItemDao(),
          categoryDao = db.categoryDao()
        )
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  RecurringScreen(
    state = state,
    onBack = onBack,
    onAdd = viewModel::add,
    onUpdate = viewModel::update,
    onDelete = viewModel::delete
  )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RecurringScreen(
  state: RecurringUiState,
  onBack: () -> Unit,
  onAdd: (Long, Long, RecurringFrequency, Long) -> Unit,
  onUpdate: (com.financetracker.app.data.entity.RecurringItem, Long, Long, RecurringFrequency, Long) -> Unit,
  onDelete: (com.financetracker.app.data.entity.RecurringItem) -> Unit
) {
  val context = LocalContext.current
  var adding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<RecurringRow?>(null) }
  var deleteTarget by remember { mutableStateOf<RecurringRow?>(null) }
  var pendingSave by remember { mutableStateOf<PendingReminderSave?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ ->
    pendingSave?.let { save ->
      if (save.editing != null) {
        onUpdate(save.editing!!, save.amount, save.categoryId, save.frequency, save.nextDueDate)
      } else {
        onAdd(save.amount, save.categoryId, save.frequency, save.nextDueDate)
      }
      pendingSave = null
    }
  }

  fun runSave(save: PendingReminderSave) {
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    if (needsPermission) {
      pendingSave = save
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      if (save.editing != null) {
        onUpdate(save.editing!!, save.amount, save.categoryId, save.frequency, save.nextDueDate)
      } else {
        onAdd(save.amount, save.categoryId, save.frequency, save.nextDueDate)
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(TermBg),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TermMuted,
              modifier = Modifier.size(18.dp)
            )
          }
          Text(
            text = "Reminders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TermText
          )
        }
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = SignalPositive,
          modifier = Modifier.clickable { adding = true }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = TermBg, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "New Reminder",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = TermBg
            )
          }
        }
      }
    }

    if (state.rows.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          Text(
            text = "No recurring reminders yet",
            style = MaterialTheme.typography.bodySmall,
            color = TermMuted
          )
        }
      }
    } else {
      items(state.rows, key = { it.item.id }) { row ->
        RecurringRowItem(
          row = row,
          onEdit = { editing = row },
          onDelete = { deleteTarget = row }
        )
      }
    }
  }

  if (adding || editing != null) {
    val editingRow = editing
    AddEditReminderDialog(
      editingRow = editingRow,
      allCategories = state.categories,
      onDismiss = {
        adding = false
        editing = null
      },
      onSave = { amount, categoryId, frequency, nextDueDate ->
        runSave(
          PendingReminderSave(
            editing = editingRow?.item,
            amount = amount,
            categoryId = categoryId,
            frequency = frequency,
            nextDueDate = nextDueDate
          )
        )
        adding = false
        editing = null
      }
    )
  }

  val target = deleteTarget
  if (target != null) {
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      containerColor = TermPanel,
      titleContentColor = TermText,
      textContentColor = TermMuted,
      confirmButton = {
        TextButton(onClick = {
          onDelete(target.item)
          deleteTarget = null
        }) {
          Text("Delete", color = SignalNegative, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) {
          Text("Cancel", color = TermMuted)
        }
      },
      title = { Text("Delete reminder?") },
      text = { Text("This cancels the reminder and cannot be undone. No transactions are deleted.") }
    )
  }
}

private data class PendingReminderSave(
  val editing: com.financetracker.app.data.entity.RecurringItem?,
  val amount: Long,
  val categoryId: Long,
  val frequency: RecurringFrequency,
  val nextDueDate: Long
)

@Composable
private fun RecurringRowItem(
  row: RecurringRow,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val category = row.category
      CategoryIconTile(
        iconKey = category?.icon ?: "",
        colorArgb = category?.color ?: 0xFF8A8A8AL,
        containerSize = 16.dp
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = category?.name ?: "Unknown category",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = TermText
        )
        Text(
          text = "${row.item.frequency.label} · next ${epochDayToDisplay(row.item.nextDueDate)}",
          style = MaterialTheme.typography.labelSmall,
          color = TermMuted
        )
      }
      Text(
        text = formatRupiah(row.item.amount),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = if (category?.type == TransactionType.INCOME) SignalPositive else TermText
      )
      IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Edit",
          tint = TermMuted,
          modifier = Modifier.size(14.dp)
        )
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = SignalNegative,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddEditReminderDialog(
  editingRow: RecurringRow?,
  allCategories: List<Category>,
  onDismiss: () -> Unit,
  onSave: (Long, Long, RecurringFrequency, Long) -> Unit
) {
  val editing = editingRow?.item
  val categoryType = editingRow?.category?.type
  var typeInput by remember { mutableStateOf(categoryType ?: TransactionType.EXPENSE) }
  var amountInput by remember {
    mutableStateOf(if (editing != null) editing.amount.toString() else "")
  }
  var categoryId by remember {
    mutableStateOf(editing?.categoryId ?: allCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id)
  }
  var frequencyInput by remember {
    mutableStateOf(editing?.frequency ?: RecurringFrequency.MONTHLY)
  }
  var nextDueInput by remember {
    mutableStateOf(editing?.nextDueDate ?: todayEpochDay())
  }
  var showDatePicker by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun attemptSave() {
    val amount = parseAmount(amountInput)
    when {
      amount == null || amount <= 0 -> error = "Enter an amount greater than zero"
      categoryId == null -> error = "Select a category"
      nextDueInput < todayEpochDay() -> error = "Next due date can't be in the past"
      else -> onSave(amount, categoryId!!, frequencyInput, nextDueInput)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = TermPanel,
    titleContentColor = TermText,
    textContentColor = TermMuted,
    confirmButton = {
      TextButton(onClick = ::attemptSave) {
        Text("Save", color = SignalPositive, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TermMuted)
      }
    },
    title = {
      Text(
        text = if (editing != null) "Edit Reminder" else "New Reminder",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TermText
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (categoryType == null) {
          Text(text = "Type", style = MaterialTheme.typography.labelSmall, color = TermMuted)
          TypeToggle(
            selected = typeInput,
            onSelected = {
              typeInput = it
              categoryId = allCategories.firstOrNull { c -> c.type == it }?.id
              error = null
            }
          )
        }

        Text(text = "Amount", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = TermPanelAlt,
          border = BorderStroke(1.dp, TermBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Rp",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = SignalPositive
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
              value = amountInput,
              onValueChange = { amountInput = digitsOnly(it) },
              textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TermText
              ),
              cursorBrush = SolidColor(SignalPositive),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        Text(text = "Category", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        val available = allCategories.filter { it.type == typeInput }
        if (available.isEmpty()) {
          Text(
            text = "No ${typeInput.name.lowercase()} categories yet",
            style = MaterialTheme.typography.bodySmall,
            color = TermMuted
          )
        } else {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            available.forEach { cat ->
              CategoryChip(
                category = cat,
                selected = cat.id == categoryId,
                selectedBorderColor = SignalPositive,
                onClick = {
                  categoryId = cat.id
                  error = null
                }
              )
            }
          }
        }

        Text(text = "Frequency", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          RecurringFrequency.entries.forEach { freq ->
            val selected = frequencyInput == freq
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { frequencyInput = freq },
              shape = RoundedCornerShape(2.dp),
              color = if (selected) TermPanelAlt else TermBg,
              border = BorderStroke(1.dp, if (selected) SignalPositive else TermBorder)
            ) {
              Text(
                text = freq.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) TermText else TermMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              )
            }
          }
        }

        Text(text = "Next due", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
          shape = RoundedCornerShape(2.dp),
          color = TermPanelAlt,
          border = BorderStroke(1.dp, TermBorder)
        ) {
          Text(
            text = epochDayToDisplay(nextDueInput),
            style = MaterialTheme.typography.bodySmall,
            color = TermText,
            modifier = Modifier.padding(10.dp)
          )
        }

        Text(
          text = "No transaction is auto-created — the reminder only notifies you to log it manually.",
          style = MaterialTheme.typography.labelSmall,
          color = TermMuted
        )

        if (error != null) {
          Text(
            text = error.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = SignalNegative
          )
        }
      }
    }
  )

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = epochDayToUtcMillis(nextDueInput)
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { nextDueInput = utcMillisToEpochDay(it) }
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
}
