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
      "Date,Amount,Category,Type,Note\n" +
        "2024-01-05,50000,Groceries,expense,weekly shop\n" +
        "\n" +
        ",0,Total Income,,\n" +
        ",50000,Total Expenses,,\n" +
        ",'-50000,Net Balance,,\n",
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
      """Date,Amount,Category,Type,Note
2024-02-01,100000,Salary & Income,income,"says ""hi"",
new line"

,100000,Total Income,,
,0,Total Expenses,,
,100000,Net Balance,,
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
      "Date,Amount,Category,Type,Note\n" +
        "2024-04-01,1,Test,expense,'=SUM(A1:A2)\n" +
        "\n" +
        ",0,Total Income,,\n" +
        ",1,Total Expenses,,\n" +
        ",'-1,Net Balance,,\n",
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
      "Date,Amount,Category,Type,Note\n" +
        "2024-04-01,1,Test,expense,\"'+A1,B2\"\n" +
        "\n" +
        ",0,Total Income,,\n" +
        ",1,Total Expenses,,\n" +
        ",'-1,Net Balance,,\n",
      exportToCsv(rows)
    )
  }

  @Test
  fun csv_formulaWithLeadingWhitespace_isGuarded() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-04-01"),
        amount = 1,
        type = TransactionType.EXPENSE,
        category = "Test",
        note = " =1+1"
      )
    )

    assertEquals(
      "Date,Amount,Category,Type,Note\n" +
        "2024-04-01,1,Test,expense,' =1+1\n" +
        "\n" +
        ",0,Total Income,,\n" +
        ",1,Total Expenses,,\n" +
        ",'-1,Net Balance,,\n",
      exportToCsv(rows)
    )
  }

  @Test
  fun csv_multiRow_computesSummaryCorrectly() {
    val rows = listOf(
      TransactionExport(
        date = day("2024-05-01"),
        amount = 150000,
        type = TransactionType.INCOME,
        category = "Salary & Income",
        note = "freelance"
      ),
      TransactionExport(
        date = day("2024-05-02"),
        amount = 45000,
        type = TransactionType.EXPENSE,
        category = "Dining & Cafes",
        note = "lunch"
      )
    )

    assertEquals(
      "Date,Amount,Category,Type,Note\n" +
        "2024-05-01,150000,Salary & Income,income,freelance\n" +
        "2024-05-02,45000,Dining & Cafes,expense,lunch\n" +
        "\n" +
        ",150000,Total Income,,\n" +
        ",45000,Total Expenses,,\n" +
        ",105000,Net Balance,,\n",
      exportToCsv(rows)
    )
  }

  @Test
  fun csv_emptyRows_hasHeaderAndZeroSummaries() {
    assertEquals(
      "Date,Amount,Category,Type,Note\n" +
        "\n" +
        ",0,Total Income,,\n" +
        ",0,Total Expenses,,\n" +
        ",0,Net Balance,,\n",
      exportToCsv(emptyList())
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
