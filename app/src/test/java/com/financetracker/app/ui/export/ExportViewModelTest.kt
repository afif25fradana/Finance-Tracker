package com.financetracker.app.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.financetracker.app.data.dao.TransactionDao
import com.financetracker.app.data.dao.TransactionExport
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val dummyContext = object : android.content.ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getContentResolver(): android.content.ContentResolver? = null
  }

  private val dummyUri = android.net.TestUri("content://test/file.csv")

  private class FakeTransactionDao(
    var exportRows: List<TransactionExport> = emptyList()
  ) : TransactionDao {
    var lastFrom: Long? = null
    var lastTo: Long? = null

    override fun getAll(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    override fun getById(id: Long): Flow<Transaction?> = MutableStateFlow(null)
    override suspend fun getByIdOnce(id: Long): Transaction? = null
    override suspend fun getBetweenOnce(fromEpochDay: Long, toEpochDay: Long): List<TransactionExport> {
      lastFrom = fromEpochDay
      lastTo = toEpochDay
      return exportRows
    }
    override suspend fun insert(transaction: Transaction): Long = 1L
    override suspend fun update(transaction: Transaction) {}
    override suspend fun deleteById(id: Long) {}
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun export_whenFromDateAfterToDate_blocksExportAndSurfacesError() = runTest(testDispatcher) {
    val txDao = FakeTransactionDao()
    val viewModel = ExportViewModel(dummyContext, txDao)

    viewModel.onFromChange(20010L)
    viewModel.onToChange(20000L)

    viewModel.export(dummyUri)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isError)
    assertEquals("From date must be on or before To date.", viewModel.uiState.value.message)
    assertEquals(null, txDao.lastFrom)
  }

  @Test
  fun formChanges_updateUiStateAndPersistToSavedStateHandle() {
    val savedState = SavedStateHandle()
    val viewModel = ExportViewModel(dummyContext, FakeTransactionDao(), savedState)

    viewModel.onFromChange(19500L)
    assertEquals(19500L, viewModel.uiState.value.fromEpochDay)
    assertEquals(19500L, savedState.get<Long>("export_from"))

    viewModel.onToChange(19600L)
    assertEquals(19600L, viewModel.uiState.value.toEpochDay)
    assertEquals(19600L, savedState.get<Long>("export_to"))

    viewModel.onFormatChange(ExportFormat.JSON)
    assertEquals(ExportFormat.JSON, viewModel.uiState.value.format)
    assertEquals("JSON", savedState.get<String>("export_format"))
  }

  @Test
  fun export_csv_writesContentAndUpdatesSuccessMessage() = runTest(testDispatcher) {
    val rows = listOf(
      TransactionExport(20000L, 50_000L, TransactionType.EXPENSE, "Groceries", "Weekly groceries"),
      TransactionExport(20001L, 10_000_000L, TransactionType.INCOME, "Salary", "Bonus")
    )
    val txDao = FakeTransactionDao(rows)
    val outStream = ByteArrayOutputStream()

    val viewModel = ExportViewModel(
      context = dummyContext,
      transactionDao = txDao,
      openOutputStream = { outStream }
    )

    viewModel.onFormatChange(ExportFormat.CSV)
    viewModel.onFromChange(20000L)
    viewModel.onToChange(20005L)

    viewModel.export(dummyUri)
    val state = viewModel.uiState.first { it.message != null }

    assertFalse(state.isError)
    assertEquals("Exported 2 transactions.", state.message)

    val written = outStream.toString(Charsets.UTF_8.name())
    assertTrue(written.startsWith("Date,Amount,Category,Type,Note"))
    assertTrue(written.contains("50000"))
    assertTrue(written.contains("Groceries"))
    assertTrue(written.contains("Bonus"))
  }

  @Test
  fun export_json_writesValidJsonAndUpdatesSuccessMessage() = runTest(testDispatcher) {
    val rows = listOf(
      TransactionExport(20000L, 75_000L, TransactionType.EXPENSE, "Dining", "Dinner")
    )
    val txDao = FakeTransactionDao(rows)
    val outStream = ByteArrayOutputStream()

    val viewModel = ExportViewModel(
      context = dummyContext,
      transactionDao = txDao,
      openOutputStream = { outStream }
    )

    viewModel.onFormatChange(ExportFormat.JSON)
    viewModel.export(dummyUri)
    val state = viewModel.uiState.first { it.message != null }

    assertFalse(state.isError)
    assertEquals("Exported 1 transaction.", state.message)

    val written = outStream.toString(Charsets.UTF_8.name())
    assertTrue(written.trim().startsWith("["))
    assertTrue(written.contains("\"Dining\""))
    assertTrue(written.contains("75000"))
  }

  @Test
  fun export_whenOutputStreamFails_surfacesErrorMessage() = runTest(testDispatcher) {
    val txDao = FakeTransactionDao(listOf(TransactionExport(20000L, 1000L, TransactionType.EXPENSE, "A", "B")))
    val viewModel = ExportViewModel(
      context = dummyContext,
      transactionDao = txDao,
      openOutputStream = { throw IOException("Disk full or permission denied") }
    )

    viewModel.export(dummyUri)
    val state = viewModel.uiState.first { it.message != null }

    assertTrue(state.isError)
    assertNotNull(state.message)
    assertTrue(state.message!!.contains("Export failed: Disk full or permission denied"))
  }
}
