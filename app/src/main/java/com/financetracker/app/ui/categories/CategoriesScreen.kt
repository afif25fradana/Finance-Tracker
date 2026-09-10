package com.financetracker.app.ui.categories

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.CategoryChip
import com.financetracker.app.ui.components.CategoryIconTile
import com.financetracker.app.ui.components.TypeToggle
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermPanelAlt
import com.financetracker.app.ui.theme.TermText

private val CATEGORY_PALETTE = listOf(
  0xFF1B5543, 0xFF276A54, 0xFF266B77, 0xFF3761A5, 0xFF4245B3,
  0xFF793CB3, 0xFFA03D6E, 0xFFA5384B, 0xFF8B682D
)

@Composable
fun CategoriesRoute() {
  val context = LocalContext.current
  val viewModel: CategoriesViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        CategoriesViewModel(categoryDao = db.categoryDao())
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  CategoriesScreen(
    state = state,
    onAdd = viewModel::add,
    onUpdate = viewModel::update,
    onDelete = viewModel::delete
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesScreen(
  state: CategoriesUiState,
  onAdd: (String, TransactionType, Long) -> Unit,
  onUpdate: (Category) -> Unit,
  onDelete: (Category, Long?) -> Unit
) {
  var adding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<Category?>(null) }
  var deleteTarget by remember { mutableStateOf<CategoryRow?>(null) }

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
        Text(
          text = "Categories",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TermText
        )
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
              text = "Add Category",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = TermBg
            )
          }
        }
      }
    }

    if (state.expense.isEmpty() && state.income.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          Text(
            text = "No categories yet",
            style = MaterialTheme.typography.bodySmall,
            color = TermMuted
          )
        }
      }
    }

    if (state.expense.isNotEmpty()) {
      item {
        CategorySectionHeader(label = "Expense")
      }
      items(state.expense, key = { it.category.id }) { row ->
        CategoryRowItem(
          row = row,
          onEdit = { editing = row.category },
          onDelete = { deleteTarget = row }
        )
      }
    }

    if (state.income.isNotEmpty()) {
      item {
        CategorySectionHeader(label = "Income")
      }
      items(state.income, key = { it.category.id }) { row ->
        CategoryRowItem(
          row = row,
          onEdit = { editing = row.category },
          onDelete = { deleteTarget = row }
        )
      }
    }
  }

  if (adding || editing != null) {
    AddEditCategoryDialog(
      editing = editing,
      existingNames = sameTypeNames(state, editing?.type ?: TransactionType.EXPENSE, excludeId = editing?.id),
      onDismiss = {
        adding = false
        editing = null
      },
      onSave = { name, type, color ->
        if (editing != null) {
          onUpdate(editing!!.copy(name = name, color = color))
        } else {
          onAdd(name, type, color)
        }
        adding = false
        editing = null
      }
    )
  }

  val target = deleteTarget
  if (target != null) {
    val sameType = if (target.category.type == TransactionType.EXPENSE) state.expense else state.income
    val others = sameType.filter { it.category.id != target.category.id }
    when {
      target.totalReferences == 0 -> ConfirmDeleteDialog(
        categoryName = target.category.name,
        isLastOfType = sameType.size == 1,
        typeLabel = typeLabel(target.category.type),
        onDismiss = { deleteTarget = null },
        onConfirm = {
          onDelete(target.category, null)
          deleteTarget = null
        }
      )

      others.isEmpty() -> BlockedDeleteDialog(
        categoryName = target.category.name,
        referenceCount = target.totalReferences,
        typeLabel = typeLabel(target.category.type),
        onDismiss = { deleteTarget = null }
      )

      else -> ReassignDeleteDialog(
        categoryName = target.category.name,
        referenceCount = target.totalReferences,
        targets = others.map { it.category },
        onDismiss = { deleteTarget = null },
        onConfirm = { newId ->
          onDelete(target.category, newId)
          deleteTarget = null
        }
      )
    }
  }
}

private fun sameTypeNames(state: CategoriesUiState, type: TransactionType, excludeId: Long?): Set<String> {
  val rows = if (type == TransactionType.EXPENSE) state.expense else state.income
  return rows
    .filter { it.category.id != excludeId }
    .map { it.category.name.trim().lowercase() }
    .toSet()
}

private fun typeLabel(type: TransactionType): String =
  if (type == TransactionType.EXPENSE) "expense" else "income"

@Composable
private fun CategorySectionHeader(label: String) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Bold,
    color = TermMuted,
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
  )
}

@Composable
private fun CategoryRowItem(
  row: CategoryRow,
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
      CategoryIconTile(
        iconKey = row.category.icon,
        colorArgb = row.category.color,
        containerSize = 16.dp
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = row.category.name,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = TermText
        )
        val usage = if (row.transactionCount > 0) "${row.transactionCount} transactions" else "no transactions yet"
        Text(
          text = if (row.isDefault) "default · $usage" else usage,
          style = MaterialTheme.typography.labelSmall,
          color = TermMuted
        )
      }
      IconButton(onClick = onEdit) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Edit",
          tint = TermMuted,
          modifier = Modifier.size(14.dp)
        )
      }
      IconButton(onClick = onDelete) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditCategoryDialog(
  editing: Category?,
  existingNames: Set<String>,
  onDismiss: () -> Unit,
  onSave: (String, TransactionType, Long) -> Unit
) {
  val isEditing = editing != null
  val fixedType = editing?.type
  var nameInput by remember { mutableStateOf(editing?.name ?: "") }
  var typeInput by remember { mutableStateOf(editing?.type ?: TransactionType.EXPENSE) }
  var colorInput by remember { mutableStateOf(editing?.color ?: CATEGORY_PALETTE.first()) }
  var error by remember { mutableStateOf<String?>(null) }

  fun attemptSave() {
    val name = nameInput.trim()
    when {
      name.isEmpty() -> error = "Name is required"
      name.lowercase() in existingNames -> error = "A ${typeLabel(typeInput)} category with this name already exists"
      else -> onSave(name, typeInput, colorInput)
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
        text = if (isEditing) "Edit Category" else "New Category",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TermText
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Name", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = TermPanelAlt,
          border = BorderStroke(1.dp, TermBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          BasicTextField(
            value = nameInput,
            onValueChange = {
              nameInput = it
              error = null
            },
            textStyle = TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              color = TermText
            ),
            cursorBrush = SolidColor(SignalPositive),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            modifier = Modifier.padding(8.dp)
          )
        }

        if (isEditing) {
          Text(
            text = "${typeLabel(fixedType!!).replaceFirstChar { it.uppercase() }} category",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
        } else {
          Text(text = "Type", style = MaterialTheme.typography.labelSmall, color = TermMuted)
          TypeToggle(
            selected = typeInput,
            onSelected = {
              typeInput = it
              error = null
            }
          )
        }

        Text(text = "Color", style = MaterialTheme.typography.labelSmall, color = TermMuted)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          CATEGORY_PALETTE.forEach { colorLong ->
            val selected = colorInput == colorLong
            Surface(
              shape = RoundedCornerShape(1.dp),
              color = if (selected) TermPanelAlt else TermBg,
              border = BorderStroke(1.dp, if (selected) SignalPositive else TermBorder),
              modifier = Modifier
                .size(24.dp)
                .clickable {
                  colorInput = colorLong
                  error = null
                }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Surface(
                  shape = RoundedCornerShape(1.dp),
                  color = Color(colorLong),
                  modifier = Modifier.size(12.dp)
                ) {}
              }
            }
          }
        }

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
}

@Composable
private fun ConfirmDeleteDialog(
  categoryName: String,
  isLastOfType: Boolean,
  typeLabel: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = TermPanel,
    titleContentColor = TermText,
    textContentColor = TermMuted,
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text("Delete", color = SignalNegative, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TermMuted)
      }
    },
    title = { Text("Delete category?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Delete \"$categoryName\"? This cannot be undone.")
        if (isLastOfType) {
          Text(
            text = "This is your last $typeLabel category. Add Transaction will show no $typeLabel categories until you add one.",
            color = SignalNegative
          )
        }
      }
    }
  )
}

@Composable
private fun BlockedDeleteDialog(
  categoryName: String,
  referenceCount: Int,
  typeLabel: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = TermPanel,
    titleContentColor = TermText,
    textContentColor = TermMuted,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("OK", color = TermMuted)
      }
    },
    title = { Text("Can't delete category") },
    text = {
      Text(
        "Delete \"$categoryName\"? It is your only $typeLabel category and $referenceCount transaction(s) use it. " +
          "Add another $typeLabel category first, then delete this one."
      )
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReassignDeleteDialog(
  categoryName: String,
  referenceCount: Int,
  targets: List<Category>,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit
) {
  var selectedId by remember { mutableStateOf(targets.first().id) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = TermPanel,
    titleContentColor = TermText,
    textContentColor = TermMuted,
    confirmButton = {
      TextButton(onClick = { onConfirm(selectedId) }) {
        Text("Reassign & Delete", color = SignalNegative, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TermMuted)
      }
    },
    title = { Text("Move transactions?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$referenceCount transaction(s) use \"$categoryName\". Move them to:")
        FlowRow(
          modifier = Modifier
            .heightIn(max = 160.dp)
            .verticalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          targets.forEach { cat ->
            CategoryChip(
              category = cat,
              selected = cat.id == selectedId,
              selectedBorderColor = SignalNegative,
              onClick = { selectedId = cat.id }
            )
          }
        }
      }
    }
  )
}
