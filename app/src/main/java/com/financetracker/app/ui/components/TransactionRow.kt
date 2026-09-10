package com.financetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.financetracker.app.ui.theme.TermText

data class TransactionRow(
  val id: Long,
  val note: String,
  val dateEpochDay: Long,
  val amount: Long,
  val type: TransactionType,
  val categoryName: String,
  val categoryColor: Long,
  val categoryIcon: String
)

@Composable
fun TransactionRowItem(
  row: TransactionRow,
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
