package com.financetracker.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.CategoryIconTile
import com.financetracker.app.ui.components.epochDayToDisplay
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermPanelAlt
import com.financetracker.app.ui.theme.TermText

@Composable
fun HistoryScreen(
  onEditTransaction: (Long) -> Unit
) {
  val context = LocalContext.current
  val viewModel: HistoryViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        HistoryViewModel(
          transactionDao = db.transactionDao(),
          categoryDao = db.categoryDao()
        )
      }
    }
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(TermBg)
      .padding(start = 14.dp, end = 14.dp, top = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "History",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TermText
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(2.dp),
      color = TermPanel,
      border = BorderStroke(1.dp, TermBorder)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = TermMuted,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
          value = state.searchQuery,
          onValueChange = viewModel::onSearchQueryChange,
          textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = TermText
          ),
          cursorBrush = SolidColor(SignalPositive),
          singleLine = true,
          modifier = Modifier.weight(1f),
          decorationBox = { innerTextField ->
            if (state.searchQuery.isEmpty()) {
              Text(
                text = "Search transactions...",
                style = MaterialTheme.typography.bodySmall,
                color = TermMuted
              )
            }
            innerTextField()
          }
        )
        if (state.searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { viewModel.onSearchQueryChange("") },
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear",
              tint = TermMuted,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val chips = listOf(
        Triple("All", null, TermText),
        Triple("Expenses", TransactionType.EXPENSE, SignalNegative),
        Triple("Income", TransactionType.INCOME, SignalPositive)
      )
      chips.forEach { (label, filter, color) ->
        val selected = state.typeFilter == filter
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = if (selected) TermPanelAlt else TermPanel,
          border = BorderStroke(1.dp, if (selected) color else TermBorder),
          modifier = Modifier.clickable { viewModel.onTypeFilterChange(filter) }
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TermMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    if (state.months.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No transactions found",
          style = MaterialTheme.typography.bodySmall,
          color = TermMuted
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        state.months.forEach { month ->
          item(key = "header_${month.label}") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = month.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TermMuted
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (month.income > 0) {
                  Text(
                    text = "+${formatRupiah(month.income)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalPositive
                  )
                }
                if (month.expense > 0) {
                  Text(
                    text = "-${formatRupiah(month.expense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalNegative
                  )
                }
              }
            }
          }

          items(month.rows, key = { it.id }) { row ->
            HistoryRowItem(row = row, onEdit = { onEditTransaction(row.id) })
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryRowItem(
  row: HistoryRow,
  onEdit: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onEdit),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        CategoryIconTile(
          iconKey = row.categoryIcon,
          colorArgb = row.categoryColor,
          containerSize = 20.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = row.note.ifEmpty { row.categoryName },
            style = MaterialTheme.typography.bodySmall,
            color = TermText,
            maxLines = 1
          )
          Text(
            text = "${epochDayToDisplay(row.dateEpochDay)} · ${row.categoryName}",
            style = MaterialTheme.typography.labelSmall,
            color = TermMuted
          )
        }
      }
      Text(
        text = (if (row.type == TransactionType.EXPENSE) "-" else "+") + formatRupiah(row.amount),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = if (row.type == TransactionType.EXPENSE) SignalNegative else SignalPositive
      )
    }
  }
}
