package com.financetracker.app.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.financetracker.app.backup.BackupCategory
import com.financetracker.app.backup.BackupCodec
import com.financetracker.app.backup.BackupFile
import com.financetracker.app.backup.BackupRecurringItem
import com.financetracker.app.backup.BackupRestorer
import com.financetracker.app.backup.BackupTransaction
import com.financetracker.app.data.dao.BackupDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.Transaction
import com.financetracker.app.data.entity.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val dummyContext = object : android.content.ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getContentResolver(): android.content.ContentResolver? = null
    override fun getPackageName(): String = "com.financetracker.app"
  }

  private val dummyUri = android.net.TestUri("content://test/backup.json")

  private open class FakeBackupDao(
    val executionLog: MutableList<String> = mutableListOf()
  ) : BackupDao() {
    var existingCategories = mutableListOf<Category>()
    var existingRecurring = mutableListOf<RecurringItem>()
    var existingTransactions = mutableListOf<Transaction>()

    var replacedCategories = listOf<Category>()
    var replacedRecurring = listOf<RecurringItem>()
    var replacedTransactions = listOf<Transaction>()
    var onReplaceAllHook: (suspend () -> Unit)? = null

    override suspend fun getAllCategoriesOnce(): List<Category> = existingCategories
    override suspend fun getAllTransactionsOnce(): List<Transaction> = existingTransactions
    override suspend fun getAllRecurringOnce(): List<RecurringItem> {
      executionLog += "getAllRecurringOnce"
      return existingRecurring
    }

    override suspend fun clearTransactions() { executionLog += "clearTransactions" }
    override suspend fun clearRecurring() { executionLog += "clearRecurring" }
    override suspend fun clearCategories() { executionLog += "clearCategories" }
    override suspend fun insertCategories(items: List<Category>) {}
    override suspend fun insertRecurring(items: List<RecurringItem>) {}
    override suspend fun insertTransactions(items: List<Transaction>) {}

    override suspend fun replaceAll(
      categories: List<Category>,
      recurringItems: List<RecurringItem>,
      transactions: List<Transaction>
    ) {
      executionLog += "replaceAll"
      onReplaceAllHook?.invoke()
      replacedCategories = categories
      replacedRecurring = recurringItems
      replacedTransactions = transactions
    }
  }

  private fun sampleValidJson(): String = BackupCodec.encode(
    appVersion = "1.0",
    currency = "IDR",
    createdAt = "2026-09-23T00:00:00Z",
    categories = listOf(BackupCategory(1L, "Food", "EXPENSE", 0xFF123456, "restaurant", true)),
    transactions = listOf(BackupTransaction(10L, 50_000L, "EXPENSE", 1L, 20000L, "Lunch")),
    recurringItems = listOf(BackupRecurringItem(100L, 1L, 250_000L, "MONTHLY", 20050L))
  )

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun confirmRestore_cancelsAlarmsBeforeReplacingDatabase() = runTest(testDispatcher) {
    val executionLog = mutableListOf<String>()
    val dao = FakeBackupDao(executionLog).apply {
      existingRecurring.add(RecurringItem(101L, 1L, 100_000L, RecurringFrequency.MONTHLY, 20010L))
      existingRecurring.add(RecurringItem(102L, 1L, 200_000L, RecurringFrequency.WEEKLY, 20015L))
    }

    var cancelledAlarmIds: List<Long>? = null
    var rescheduledAlarmItems: List<RecurringItem>? = null

    val restorer = BackupRestorer(
      backupDao = dao,
      cancelReminders = { ids ->
        executionLog += "cancelReminders:$ids"
        cancelledAlarmIds = ids
      },
      rescheduleReminders = { items ->
        executionLog += "rescheduleReminders:${items.map { it.id }}"
        rescheduledAlarmItems = items
      }
    )

    val validJson = sampleValidJson()
    val viewModel = BackupRestoreViewModel(
      context = dummyContext,
      backupDao = dao,
      restorer = restorer,
      openInputStream = { ByteArrayInputStream(validJson.toByteArray(Charsets.UTF_8)) }
    )

    // Pick file to set pendingRestore
    viewModel.onRestoreFilePicked(dummyUri)
    val pickedState = viewModel.uiState.first { !it.isWorking && it.pendingRestore != null }
    assertNotNull(pickedState.pendingRestore)

    // Confirm restore
    viewModel.confirmRestore()
    val restoredState = viewModel.uiState.first { !it.isWorking && it.message != null }

    assertEquals(listOf(101L, 102L), cancelledAlarmIds)
    assertEquals(listOf(100L), rescheduledAlarmItems?.map { it.id })

    // Verify ordering: cancelReminders BEFORE replaceAll, and replaceAll BEFORE rescheduleReminders
    val cancelIdx = executionLog.indexOfFirst { it.startsWith("cancelReminders") }
    val replaceIdx = executionLog.indexOf("replaceAll")
    val rescheduleIdx = executionLog.indexOfFirst { it.startsWith("rescheduleReminders") }

    assertTrue("cancelReminders must execute", cancelIdx != -1)
    assertTrue("replaceAll must execute", replaceIdx != -1)
    assertTrue("rescheduleReminders must execute", rescheduleIdx != -1)
    assertTrue("Alarm cancellation must run before DB replace", cancelIdx < replaceIdx)
    assertTrue("DB replace must run before alarm rescheduling", replaceIdx < rescheduleIdx)

    assertFalse(restoredState.isError)
    assertNull(restoredState.pendingRestore)
    assertTrue(restoredState.message!!.contains("Restore completed"))
  }

  @Test
  fun confirmRestore_completesUnderSimulatedCancellationPressure_viaNonCancellable() = runTest(testDispatcher) {
    val dao = FakeBackupDao()
    var replaceAllStarted = false
    var replaceAllCompleted = false
    var contextActiveDuringReplace: Boolean? = null

    dao.onReplaceAllHook = {
      replaceAllStarted = true
      // Context should still be active even if parent coroutine scope is cancelled, due to NonCancellable
      contextActiveDuringReplace = currentCoroutineContext().isActive
      replaceAllCompleted = true
    }

    val restorer = BackupRestorer(backupDao = dao)
    val validJson = sampleValidJson()

    val viewModel = BackupRestoreViewModel(
      context = dummyContext,
      backupDao = dao,
      restorer = restorer,
      openInputStream = { ByteArrayInputStream(validJson.toByteArray(Charsets.UTF_8)) }
    )

    viewModel.onRestoreFilePicked(dummyUri)
    val pickedState = viewModel.uiState.first { !it.isWorking && it.pendingRestore != null }
    assertNotNull(pickedState.pendingRestore)

    // Launch confirmRestore and verify NonCancellable protection
    viewModel.confirmRestore()
    val restoredState = viewModel.uiState.first { !it.isWorking && it.message != null }

    assertTrue("replaceAll should have started", replaceAllStarted)
    assertTrue("replaceAll should have completed under NonCancellable", replaceAllCompleted)
    assertEquals(true, contextActiveDuringReplace)
    assertEquals(1, dao.replacedCategories.size)
    assertEquals(1, dao.replacedTransactions.size)
  }

  @Test
  fun cancelRestore_clearsPendingRestore() = runTest(testDispatcher) {
    val validJson = sampleValidJson()
    val viewModel = BackupRestoreViewModel(
      context = dummyContext,
      backupDao = FakeBackupDao(),
      openInputStream = { ByteArrayInputStream(validJson.toByteArray(Charsets.UTF_8)) }
    )

    viewModel.onRestoreFilePicked(dummyUri)
    val pickedState = viewModel.uiState.first { !it.isWorking && it.pendingRestore != null }
    assertNotNull(pickedState.pendingRestore)

    viewModel.cancelRestore()
    assertNull(viewModel.uiState.value.pendingRestore)
  }

  @Test
  fun onRestoreFilePicked_whenMalformed_surfacesError() = runTest(testDispatcher) {
    val viewModel = BackupRestoreViewModel(
      context = dummyContext,
      backupDao = FakeBackupDao(),
      openInputStream = { ByteArrayInputStream("{ malformed json".toByteArray(Charsets.UTF_8)) }
    )

    viewModel.onRestoreFilePicked(dummyUri)
    val errorState = viewModel.uiState.first { !it.isWorking && it.message != null }

    assertTrue(errorState.isError)
    assertNull(errorState.pendingRestore)
    assertNotNull(errorState.message)
  }
}
