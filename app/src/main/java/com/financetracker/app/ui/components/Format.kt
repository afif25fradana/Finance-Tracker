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

fun epochDayToUtcMillis(epochDay: Long): Long =
  LocalDate.ofEpochDay(epochDay).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToEpochDay(millis: Long): Long =
  java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()

fun centsToInputText(cents: Long): String =
  "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

fun parseAmountToCents(text: String): Long? {
  val t = text.trim()
  if (t.isEmpty() || !t.matches(Regex("""\d*\.?\d{0,2}"""))) return null
  val parts = t.split(".")
  val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
  val frac = parts.getOrNull(1).orEmpty().padEnd(2, '0').ifEmpty { "0" }
  return whole * 100 + frac.toLong()
}

fun formatCents(cents: Long): String {
  val sign = if (cents < 0) "-" else ""
  val absCents = abs(cents)
  val whole = absCents / 100
  val frac = (absCents % 100).toString().padStart(2, '0')
  return "$sign$whole.$frac"
}
