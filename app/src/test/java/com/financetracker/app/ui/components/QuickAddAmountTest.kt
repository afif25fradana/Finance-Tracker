package com.financetracker.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddAmountTest {

  @Test
  fun emptyInputBecomesPreset() {
    assertEquals("10000", addPresetToAmount("", 10_000L))
  }

  @Test
  fun zeroInputBecomesPreset() {
    assertEquals("50000", addPresetToAmount("0", 50_000L))
  }

  @Test
  fun addsToExistingAmount() {
    assertEquals("22345", addPresetToAmount("12345", 10_000L))
  }

  @Test
  fun clampsToTwelveDigitCeiling() {
    assertEquals("999999999999", addPresetToAmount("999999999900", 10_000L))
    assertEquals("999999999999", addPresetToAmount("999999999999", 1L))
  }

  @Test
  fun nonNumericInputTreatedAsZero() {
    assertEquals("25000", addPresetToAmount("not-a-number", 25_000L))
  }
}
