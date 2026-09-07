// TEMPORARY HOME — Phase 2 landing screen.
// Lists transactions so add/edit can be exercised end to end and persistence
// can be verified (kill & reopen). Phase 3 replaces this file and its NavHost
// route with the real History screen; delete both then.

package com.financetracker.app.ui.history

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.epochDayToDisplay
import com.financetracker.app.ui.components.formatCents
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermText

@Composable
fun TemporaryHomeScreen(
  onAddTransaction: () -> Unit,
  onEditTransaction: (Long) -> Unit
) {
  val context = LocalContext.current
  val viewModel: TemporaryHomeViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val db = AppDatabase.getInstance(context.applicationContext)
        TemporaryHomeViewModel(
          transactionDao = db.transactionDao(),
          categoryDao = db.categoryDao()
        )
      }
    }
  )
  val rows by viewModel.rows.collectAsStateWithLifecycle()

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
        text = "Transactions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TermText
      )
      Surface(
        shape = RoundedCornerShape(2.dp),
        color = SignalPositive,
        modifier = Modifier.clickable(onClick = onAddTransaction)
      ) {
        Text(
          text = "+ New",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TermBg,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
      }
    }

    Spacer(modifier = Modifier.size(10.dp))

    if (rows.isEmpty()) {
      Text(
        text = "No transactions yet. Tap + New to add one.",
        style = MaterialTheme.typography.bodySmall,
        color = TermMuted,
        modifier = Modifier.fillMaxWidth()
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(rows, key = { it.id }) { row ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onEditTransaction(row.id) },
            shape = RoundedCornerShape(2.dp),
            color = TermPanel,
            border = BorderStroke(1.dp, TermBorder)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(1.dp),
                color = Color(row.categoryColor),
                modifier = Modifier.size(10.dp)
              ) {}
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = row.note.ifEmpty { row.categoryName },
                  style = MaterialTheme.typography.bodyMedium,
                  color = TermText,
                  maxLines = 1
                )
                Text(
                  text = "${row.categoryName} · ${epochDayToDisplay(row.dateEpochDay)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TermMuted
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = (if (row.type == TransactionType.EXPENSE) "-" else "+") + "$" + formatCents(row.amountCents),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (row.type == TransactionType.EXPENSE) SignalNegative else SignalPositive
              )
            }
          }
        }
      }
    }
  }
}
