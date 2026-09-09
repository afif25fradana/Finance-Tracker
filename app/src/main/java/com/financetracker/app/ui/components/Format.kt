package com.financetracker.app.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")

fun epochDayToDisplay(epochDay: Long): String =
  LocalDate.ofEpochDay(epochDay).format(DAY_FORMATTER)

fun epochDayToMonthLabel(epochDay: Long): String =
  LocalDate.ofEpochDay(epochDay).format(MONTH_FORMATTER)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun epochDayToIso(epochDay: Long): String =
  LocalDate.ofEpochDay(epochDay).toString()

fun epochDayToUtcMillis(epochDay: Long): Long =
  LocalDate.ofEpochDay(epochDay).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToEpochDay(millis: Long): Long =
  java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()

fun amountToInputText(amount: Long): String = amount.toString()

fun formatAmountInput(raw: String): String =
  raw.reversed().chunked(3).joinToString(".").reversed()

private const val MAX_INPUT_AMOUNT = 999_999_999_999L

fun addPresetToAmount(current: String, preset: Long): String {
  val base = current.toLongOrNull() ?: 0L
  return (base + preset).coerceAtMost(MAX_INPUT_AMOUNT).toString()
}

fun parseAmount(text: String): Long? {
  val t = text.trim()
  if (t.isEmpty() || !t.matches(Regex("""\d{1,12}"""))) return null
  return t.toLongOrNull()
}

fun formatRupiah(amount: Long): String {
  val sign = if (amount < 0) "-" else ""
  val grouped = abs(amount).toString()
    .reversed()
    .chunked(3)
    .joinToString(".")
    .reversed()
  return "${sign}Rp$grouped"
}
