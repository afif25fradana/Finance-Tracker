package com.financetracker.app.data.entity

import java.time.LocalDate

enum class RecurringFrequency(val label: String) {
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly"),
  YEARLY("Yearly");

  /** Next occurrence strictly after [fromEpochDay], skipping any date <= today. */
  fun nextDueEpochDay(fromEpochDay: Long, todayEpochDay: Long): Long {
    var next = advance(LocalDate.ofEpochDay(fromEpochDay)).toEpochDay()
    while (next <= todayEpochDay) {
      next = advance(LocalDate.ofEpochDay(next)).toEpochDay()
    }
    return next
  }

  private fun advance(date: LocalDate): LocalDate = when (this) {
    DAILY -> date.plusDays(1)
    WEEKLY -> date.plusWeeks(1)
    MONTHLY -> date.plusMonths(1)
    YEARLY -> date.plusYears(1)
  }
}
