package com.financetracker.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
  Math.multiplyExact(epochDay, 86_400_000L)

fun utcMillisToEpochDay(millis: Long): Long =
  Math.floorDiv(millis, 86_400_000L)

fun formatAmountInput(raw: String): String = groupDigits(raw)

class ThousandsSeparatorVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text
    if (raw.isEmpty()) {
      return TransformedText(text, OffsetMapping.Identity)
    }
    val formatted = formatAmountInput(raw)

    val offsetMapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int {
        val coerced = offset.coerceIn(0, raw.length)
        if (coerced == 0) return 0
        val remaining = raw.length - coerced
        val totalDots = (raw.length - 1) / 3
        val dotsAfter = if (remaining > 0) (remaining - 1) / 3 else 0
        val dotsBefore = totalDots - dotsAfter
        return (coerced + dotsBefore).coerceIn(0, formatted.length)
      }

      override fun transformedToOriginal(offset: Int): Int {
        val coerced = offset.coerceIn(0, formatted.length)
        val dotsBefore = formatted.take(coerced).count { it == '.' }
        return (coerced - dotsBefore).coerceIn(0, raw.length)
      }
    }

    return TransformedText(AnnotatedString(formatted), offsetMapping)
  }
}

fun digitsOnly(text: String): String = text.filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)

private fun groupDigits(digits: String): String =
  digits.reversed().chunked(3).joinToString(".").reversed()

const val MAX_AMOUNT_DIGITS = 12
const val MAX_CATEGORY_NAME_LENGTH = 36
const val MAX_NOTE_LENGTH = 100
const val MAX_SEARCH_QUERY_LENGTH = 50
private const val MAX_INPUT_AMOUNT = 999_999_999_999L
 
fun addPresetToAmount(current: String, preset: Long): String {
  val base = current.toLongOrNull() ?: 0L
  return (base + preset).coerceAtMost(MAX_INPUT_AMOUNT).toString()
}

fun parseAmount(text: String): Long? = text.trim().toLongOrNull()

fun formatRupiah(amount: Long): String {
  val sign = if (amount < 0) "-" else ""
  return "${sign}Rp${groupDigits(abs(amount).toString())}"
}
