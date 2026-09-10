package com.financetracker.app.export

import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExporterTest {

  private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

  @Test
  fun csv_plainRow_isHeaderPlusRow() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-01-05"),
        amount = 50000,
        type = TransactionType.EXPENSE,
        category = "Groceries",
        note = "weekly shop"
      )
    )

    val csv = exportToCsv(rows)

    assertEquals(
      "date,amount,category,type,note\n" +
        "2024-01-05,50000,Groceries,expense,weekly shop\n",
      csv
    )
  }

  @Test
  fun csv_noteWithDelimiters_isQuotedAndEscaped() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-02-01"),
        amount = 100000,
        type = TransactionType.INCOME,
        category = "Salary & Income",
        note = "says \"hi\",\nnew line"
      )
    )

    val csv = exportToCsv(rows)

    assertEquals(
      """date,amount,category,type,note
2024-02-01,100000,Salary & Income,income,"says ""hi"",
new line"
""",
      csv
    )
  }

  @Test
  fun csv_formulaLikeNote_isGuardedWithLeadingQuote() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-04-01"),
        amount = 1,
        type = TransactionType.EXPENSE,
        category = "Test",
        note = "=SUM(A1:A2)"
      )
    )

    assertEquals(
      "date,amount,category,type,note\n" +
        "2024-04-01,1,Test,expense,'=SUM(A1:A2)\n",
      exportToCsv(rows)
    )
  }

  @Test
  fun csv_guardedFieldWithComma_isAlsoQuoted() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-04-01"),
        amount = 1,
        type = TransactionType.EXPENSE,
        category = "Test",
        note = "+A1,B2"
      )
    )

    assertEquals(
      "date,amount,category,type,note\n" +
        "2024-04-01,1,Test,expense,\"'+A1,B2\"\n",
      exportToCsv(rows)
    )
  }

  @Test
  fun json_containsAllFieldsWithEscaping() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-03-10"),
        amount = 25000,
        type = TransactionType.EXPENSE,
        category = "Cafe",
        note = "quote\" and backslash\\ and newline\n"
      )
    )

    val json = exportToJson(rows)

    assertEquals(
      """[{"date":"2024-03-10","amount":25000,"category":"Cafe","type":"expense","note":"quote\" and backslash\\ and newline\n"}]""",
      json
    )
  }

  @Test
  fun json_emptyRows_isEmptyArray() {
    assertEquals("[]", exportToJson(emptyList()))
  }
}
