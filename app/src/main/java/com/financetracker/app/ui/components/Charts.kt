package com.financetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.financetracker.app.ui.dashboard.CashflowPoint
import com.financetracker.app.ui.dashboard.TrendPoint
import com.financetracker.app.ui.theme.SignalNegative
import com.financetracker.app.ui.theme.SignalPositive
import com.financetracker.app.ui.theme.TermBorder
import com.financetracker.app.ui.theme.TermMuted
import com.financetracker.app.ui.theme.TermPanel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val monthLabelsKey = ExtraStore.Key<List<String>>()

private val indexFormatter = CartesianValueFormatter { context, value, _ ->
  context.model.extraStore[monthLabelsKey].getOrNull(value.toInt()) ?: ""
}

@Composable
private fun ChartCard(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(2.dp),
    color = TermPanel,
    border = BorderStroke(1.dp, TermBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = TermMuted,
          fontWeight = FontWeight.Medium
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      content()
    }
  }
}

@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Surface(shape = RoundedCornerShape(1.dp), color = color, modifier = Modifier.size(8.dp)) {}
    Spacer(modifier = Modifier.width(4.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = TermMuted)
  }
}

@Composable
fun CashflowChart(
  data: List<CashflowPoint>,
  modifier: Modifier = Modifier
) {
  val modelProducer = remember { CartesianChartModelProducer() }
  val months = data.map { it.label }

  LaunchedEffect(data) {
    modelProducer.runTransaction {
      columnModel {
        series(x = List(data.size) { it }, y = data.map { it.income.toFloat() }, key = "income")
        series(x = List(data.size) { it }, y = data.map { it.expense.toFloat() }, key = "expense")
      }
      extras { it[monthLabelsKey] = months }
    }
  }

  val incomeColumn = rememberLineComponent(fill = Fill(SignalPositive), thickness = 10.dp)
  val expenseColumn = rememberLineComponent(fill = Fill(SignalNegative), thickness = 10.dp)
  val columnProvider = ColumnCartesianLayer.ColumnProvider.series(incomeColumn, expenseColumn)
  val layer = rememberColumnCartesianLayer(columnProvider = columnProvider)

  ChartCard(
    title = "Cashflow",
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      LegendDot(SignalPositive, "Income")
      Spacer(modifier = Modifier.width(12.dp))
      LegendDot(SignalNegative, "Expenses")
    }
    Spacer(modifier = Modifier.height(14.dp))
    CartesianChartHost(
      chart = rememberCartesianChart(
        layer,
        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = indexFormatter),
        layerPadding = { CartesianLayerPadding(unscalableEnd = 12f.dp) }
      ),
      modelProducer = modelProducer,
      modifier = Modifier.fillMaxWidth().height(150.dp)
    )
  }
}

@Composable
fun TrendLineChart(
  data: List<TrendPoint>,
  modifier: Modifier = Modifier
) {
  val modelProducer = remember { CartesianChartModelProducer() }
  val months = data.map { it.label }

  LaunchedEffect(data) {
    modelProducer.runTransaction {
      lineModel {
        series(x = List(data.size) { it }, y = data.map { it.amount.toFloat() }, key = "expense")
      }
      extras { it[monthLabelsKey] = months }
    }
  }

  val line = LineCartesianLayer.rememberLine(
    fill = LineCartesianLayer.LineFill.single(Fill(SignalPositive)),
    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp)
  )
  val layer = rememberLineCartesianLayer(
    lineProvider = LineCartesianLayer.LineProvider.series(line)
  )

  ChartCard(
    title = "Spending Trend",
    modifier = modifier
  ) {
    CartesianChartHost(
      chart = rememberCartesianChart(
        layer,
        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = indexFormatter)
      ),
      modelProducer = modelProducer,
      modifier = Modifier.fillMaxWidth().height(130.dp)
    )
  }
}
