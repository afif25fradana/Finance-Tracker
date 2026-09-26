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

fun formatAmountInput(raw: String): String = groupDigits(raw)

fun digitsOnly(text: String): String = text.filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)

private fun groupDigits(digits: String): String =
  digits.reversed().chunked(3).joinToString(".").reversed()

const val MAX_AMOUNT_DIGITS = 12
const val MAX_CATEGORY_NAME_LENGTH = 36
const val MAX_NOTE_LENGTH = 100
const val MAX_SEARCH_QUERY_LENGTH = 50
private const val MAX_INPUT_AMOUNT = 999_999_999_999L
private val AMOUNT_REGEX = Regex("""\d{1,$MAX_AMOUNT_DIGITS}""")

fun addPresetToAmount(current: String, preset: Long): String {
  val base = current.toLongOrNull() ?: 0L
  return (base + preset).coerceAtMost(MAX_INPUT_AMOUNT).toString()
}

fun parseAmount(text: String): Long? {
  val t = text.trim()
  if (t.isEmpty() || !AMOUNT_REGEX.matches(t)) return null
  return t.toLongOrNull()
}

fun formatRupiah(amount: Long): String {
  val sign = if (amount < 0) "-" else ""
  return "${sign}Rp${groupDigits(abs(amount).toString())}"
}
