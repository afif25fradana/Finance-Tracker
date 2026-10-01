package com.financetracker.app.export

import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.epochDayToIso
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val EXPORT_HEADER = listOf("Date", "Amount", "Category", "Type", "Note")

fun exportToCsv(rows: List<TransactionExport>): String = buildString {
  append(EXPORT_HEADER.joinToString(","))
  append('\n')
  var totalIncome = 0L
  var totalExpenses = 0L
  rows.forEach { row ->
    when (row.type) {
      TransactionType.INCOME -> totalIncome += row.amount
      TransactionType.EXPENSE -> totalExpenses += row.amount
    }
    val fields = listOf(
      epochDayToIso(row.date),
      row.amount.toString(),
      row.category,
      typeLabel(row.type),
      row.note
    )
    append(fields.joinToString(",") { csvEscape(it) })
    append('\n')
  }

  append('\n')

  val netBalance = totalIncome - totalExpenses
  val summaryRows = listOf(
    listOf("", totalIncome.toString(), "Total Income", "", ""),
    listOf("", totalExpenses.toString(), "Total Expenses", "", ""),
    listOf("", netBalance.toString(), "Net Balance", "", "")
  )
  summaryRows.forEach { row ->
    append(row.joinToString(",") { csvEscape(it) })
    append('\n')
  }
}

fun exportToJson(rows: List<TransactionExport>): String =
  Json.encodeToString(EXPORT_ROW_LIST, rows.map { it.toExportRow() })

@Serializable
private data class ExportJsonRow(
  val date: String,
  val amount: Long,
  val category: String,
  val type: String,
  val note: String
)

private val EXPORT_ROW_LIST = ListSerializer(ExportJsonRow.serializer())

private fun TransactionExport.toExportRow(): ExportJsonRow =
  ExportJsonRow(
    date = epochDayToIso(date),
    amount = amount,
    category = category,
    type = typeLabel(type),
    note = note
  )

private fun typeLabel(type: TransactionType): String =
  if (type == TransactionType.INCOME) "income" else "expense"

// RFC 4180: quote when the field contains a delimiter, quote, CR or LF; double embedded quotes.
private fun csvEscape(value: String): String {
  val trimmed = value.trimStart()
  val isFormula = trimmed.isNotEmpty() && (trimmed[0] in "=+-@" || value.startsWith("\t") || value.startsWith("\r"))
  val guarded = if (isFormula) "'$value" else value
  val needsQuoting = guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
  return if (needsQuoting) "\"${guarded.replace("\"", "\"\"")}\"" else guarded
}
