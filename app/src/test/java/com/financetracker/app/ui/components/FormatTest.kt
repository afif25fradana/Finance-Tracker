package com.financetracker.app.ui.components

import androidx.compose.ui.text.AnnotatedString
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

  @Test
  fun thousandsSeparatorVisualTransformation_formatsExpectedLengths() {
    val transformation = ThousandsSeparatorVisualTransformation()

    assertEquals("", transformation.filter(AnnotatedString("")).text.text)
    assertEquals("1", transformation.filter(AnnotatedString("1")).text.text)
    assertEquals("123", transformation.filter(AnnotatedString("123")).text.text)
    assertEquals("1.234", transformation.filter(AnnotatedString("1234")).text.text)
    assertEquals("1.234.567", transformation.filter(AnnotatedString("1234567")).text.text)
  }

  @Test
  fun thousandsSeparatorVisualTransformation_offsetMappingBoundariesAndMidOffsets() {
    val transformation = ThousandsSeparatorVisualTransformation()

    // 4-digit test: "1234" -> "1.234"
    val transformed4 = transformation.filter(AnnotatedString("1234"))
    val map4 = transformed4.offsetMapping

    // originalToTransformed
    assertEquals(0, map4.originalToTransformed(0))
    assertEquals(2, map4.originalToTransformed(1))
    assertEquals(3, map4.originalToTransformed(2))
    assertEquals(4, map4.originalToTransformed(3))
    assertEquals(5, map4.originalToTransformed(4))

    // transformedToOriginal
    assertEquals(0, map4.transformedToOriginal(0))
    assertEquals(1, map4.transformedToOriginal(1))
    assertEquals(1, map4.transformedToOriginal(2))
    assertEquals(2, map4.transformedToOriginal(3))
    assertEquals(3, map4.transformedToOriginal(4))
    assertEquals(4, map4.transformedToOriginal(5))

    // 7-digit test: "1234567" -> "1.234.567"
    val transformed7 = transformation.filter(AnnotatedString("1234567"))
    val map7 = transformed7.offsetMapping

    assertEquals(0, map7.originalToTransformed(0))
    assertEquals(2, map7.originalToTransformed(1))
    assertEquals(4, map7.originalToTransformed(3))
    assertEquals(6, map7.originalToTransformed(4))
    assertEquals(9, map7.originalToTransformed(7))

    assertEquals(0, map7.transformedToOriginal(0))
    assertEquals(1, map7.transformedToOriginal(1))
    assertEquals(1, map7.transformedToOriginal(2))
    assertEquals(4, map7.transformedToOriginal(6))
    assertEquals(7, map7.transformedToOriginal(9))

    // Empty test boundary
    val emptyTransformed = transformation.filter(AnnotatedString(""))
    val emptyMap = emptyTransformed.offsetMapping
    assertEquals(0, emptyMap.originalToTransformed(0))
    assertEquals(0, emptyMap.transformedToOriginal(0))
  }
}
