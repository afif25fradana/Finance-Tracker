package com.financetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBg
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanelAlt
import com.financetracker.app.ui.theme.TermText

@Composable
fun TypeToggle(
  selected: TransactionType,
  onSelected: (TransactionType) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    listOf(
      TransactionType.EXPENSE to "Expense" to SignalNegative,
      TransactionType.INCOME to "Income" to SignalPositive
    ).forEach { option ->
      val type = option.first.first
      val label = option.first.second
      val color = option.second
      val isSelected = selected == type
      Surface(
        modifier = Modifier
          .weight(1f)
          .clickable { onSelected(type) },
        shape = RoundedCornerShape(2.dp),
        color = if (isSelected) color else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) color else TermBorder)
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          color = if (isSelected) TermBg else TermMuted,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }
    }
  }
}

@Composable
fun CategoryChip(
  category: Category,
  selected: Boolean,
  selectedBorderColor: Color,
  onClick: () -> Unit,
  showIcon: Boolean = false
) {
  Surface(
    shape = RoundedCornerShape(2.dp),
    color = if (selected) TermPanelAlt else TermBg,
    border = BorderStroke(1.dp, if (selected) selectedBorderColor else TermBorder),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (showIcon) {
        CategoryIconTile(
          iconKey = category.icon,
          colorArgb = category.color,
          containerSize = 16.dp
        )
      } else {
        Surface(
          shape = RoundedCornerShape(1.dp),
          color = Color(category.color),
          modifier = Modifier.size(10.dp)
        ) {}
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = category.name,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) TermText else TermMuted
      )
    }
  }
}
