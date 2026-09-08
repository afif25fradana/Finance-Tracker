package com.financetracker.app.export

import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.TransactionType
import java.time.LocalDate

private val EXPORT_HEADER = listOf("date", "amount", "category", "type", "note")

fun exportToCsv(rows: List<TransactionExport>): String = buildString {
  append(EXPORT_HEADER.joinToString(","))
  append('\n')
  rows.forEach { row ->
    val fields = listOf(
      isoDate(row.date),
      row.amount.toString(),
      row.category,
      typeLabel(row.type),
      row.note
    )
    append(fields.joinToString(",") { csvEscape(it) })
    append('\n')
  }
}

fun exportToJson(rows: List<TransactionExport>): String = buildString {
  append("[")
  rows.forEachIndexed { index, row ->
    if (index > 0) append(",")
    append("{")
    append("\"date\":").append(jsonString(isoDate(row.date))).append(",")
    append("\"amount\":").append(row.amount).append(",")
    append("\"category\":").append(jsonString(row.category)).append(",")
    append("\"type\":").append(jsonString(typeLabel(row.type))).append(",")
    append("\"note\":").append(jsonString(row.note))
    append("}")
  }
  append("]")
}

private fun typeLabel(type: TransactionType): String =
  if (type == TransactionType.INCOME) "income" else "expense"

private fun isoDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

// RFC 4180: quote when the field contains a delimiter, quote, CR or LF; double embedded quotes.
private fun csvEscape(value: String): String {
  val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
  return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
}

private fun jsonString(value: String): String = buildString {
  append('"')
  value.forEach { c ->
    when (c) {
      '"' -> append("\\\"")
      '\\' -> append("\\\\")
      '\b' -> append("\\b")
      '\u000C' -> append("\\f")
      '\n' -> append("\\n")
      '\r' -> append("\\r")
      '\t' -> append("\\t")
      else -> if (c < ' ') {
        append("\\u").append(c.code.toString(16).padStart(4, '0'))
      } else {
        append(c)
      }
    }
  }
  append('"')
}
