package com.financetracker.app.data.entity

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringFrequencyTest {

  private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()
  private fun iso(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

  @Test
  fun daily_advancesPastToday() {
    val from = day("2026-01-01")
    val today = day("2026-01-05")
    val next = RecurringFrequency.DAILY.nextDueEpochDay(from, today)

    assertEquals("2026-01-06", iso(next))
  }

  @Test
  fun weekly_advancesPastToday() {
    val from = day("2026-01-01")
    val today = day("2026-01-10")
    val next = RecurringFrequency.WEEKLY.nextDueEpochDay(from, today)

    // 01-01 -> 01-08 -> 01-15
    assertEquals("2026-01-15", iso(next))
  }

  @Test
  fun monthly_advancesPastToday() {
    val from = day("2026-01-15")
    val today = day("2026-03-20")
    val next = RecurringFrequency.MONTHLY.nextDueEpochDay(from, today)

    // 01-15 -> 02-15 -> 03-15 -> 04-15
    assertEquals("2026-04-15", iso(next))
  }

  @Test
  fun yearly_advancesPastToday() {
    val from = day("2024-02-29") // leap year
    val today = day("2026-01-01")
    val next = RecurringFrequency.YEARLY.nextDueEpochDay(from, today)

    // 2024-02-29 -> 2025-02-28 -> 2026-02-28
    assertEquals("2026-02-28", iso(next))
  }

  @Test
  fun futureDate_advancesAtLeastOnceEvenIfAlreadyAfterToday() {
    val from = day("2026-05-01")
    val today = day("2026-01-01")
    val next = RecurringFrequency.MONTHLY.nextDueEpochDay(from, today)

    assertEquals("2026-06-01", iso(next))
  }
}
