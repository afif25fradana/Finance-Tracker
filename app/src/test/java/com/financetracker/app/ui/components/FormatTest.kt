package com.financetracker.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormatTest {

  @Test
  fun epochDayToUtcMillis_and_utcMillisToEpochDay_roundTrip() {
    val testEpochDay = LocalDate.of(2026, 10, 1).toEpochDay()
    val millis = epochDayToUtcMillis(testEpochDay)
    val convertedBack = utcMillisToEpochDay(millis)

    assertEquals(testEpochDay, convertedBack)
    assertEquals(testEpochDay * 86_400_000L, millis)
  }

  @Test
  fun parseAmount_validNumericStrings_returnsLong() {
    assertEquals(50000L, parseAmount("50000"))
    assertEquals(123456789012L, parseAmount("123456789012"))
    assertEquals(100L, parseAmount("  100  "))
  }

  @Test
  fun parseAmount_invalidOrEmpty_returnsNull() {
    assertNull(parseAmount(""))
    assertNull(parseAmount("   "))
    assertNull(parseAmount("abc"))
    assertNull(parseAmount("12.34"))
    assertNull(parseAmount("12,34"))
  }
}
