package com.financetracker.app.ui.recurring

import android.content.Context
import com.financetracker.app.data.dao.CategoryDao
import com.financetracker.app.data.dao.CategoryRefCount
import com.financetracker.app.data.dao.RecurringItemDao
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

import android.app.AlarmManager
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val dummyContext = object : android.content.ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getContentResolver(): android.content.ContentResolver? = null
  }

  private class FakeCategoryDao(categories: List<Category> = emptyList()) : CategoryDao() {
    val flow = MutableStateFlow(categories)
    override fun getAll(): Flow<List<Category>> = flow
    override suspend fun getByIdOnce(id: Long): Category? = flow.value.find { it.id == id }
    override suspend fun insert(category: Category): Long = 1L
    override suspend fun update(category: Category) {}
    override suspend fun delete(category: Category) {}
    override fun transactionCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override fun recurringCounts(): Flow<List<CategoryRefCount>> = MutableStateFlow(emptyList())
    override suspend fun reassignTransactions(oldId: Long, newId: Long) {}
    override suspend fun reassignRecurring(oldId: Long, newId: Long) {}
    override suspend fun deleteById(id: Long) {}
  }

  private class FakeRecurringItemDao(items: List<RecurringItem> = emptyList()) : RecurringItemDao {
    val flow = MutableStateFlow(items)
    val inserted = mutableListOf<RecurringItem>()
    val updated = mutableListOf<RecurringItem>()
    val deleted = mutableListOf<RecurringItem>()
    private var nextId = 100L

    override fun getAll(): Flow<List<RecurringItem>> = flow
    override suspend fun getAllOnce(): List<RecurringItem> = flow.value
    override suspend fun getByIdOnce(id: Long): RecurringItem? = flow.value.find { it.id == id }

    override suspend fun insert(item: RecurringItem): Long {
      val generatedId = if (item.id == 0L) nextId++ else item.id
      val saved = item.copy(id = generatedId)
      inserted.add(saved)
      flow.value = flow.value + saved
      return generatedId
    }

    override suspend fun update(item: RecurringItem) {
      updated.add(item)
      flow.value = flow.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(item: RecurringItem) {
      deleted.add(item)
      flow.value = flow.value.filter { it.id != item.id }
    }
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
  fun uiState_combinesRecurringItemsAndCategories() = runTest(testDispatcher) {
    val cat = Category(1L, "Housing", TransactionType.EXPENSE, 0xFF4245B3, "home", true)
    val item = RecurringItem(10L, 1L, 3_000_000L, RecurringFrequency.MONTHLY, 20050L)

    val recDao = FakeRecurringItemDao(listOf(item))
    val catDao = FakeCategoryDao(listOf(cat))

    val viewModel = RecurringViewModel(dummyContext, recDao, catDao)

    val state = viewModel.uiState.first { it.rows.isNotEmpty() }
    assertEquals(1, state.rows.size)
    assertEquals(10L, state.rows[0].item.id)
    assertEquals("Housing", state.rows[0].category?.name)
    assertEquals(1, state.categories.size)
  }

  @Test
  fun add_insertsItemAndSchedulesReminder() = runTest(testDispatcher) {
    val recDao = FakeRecurringItemDao()
    val catDao = FakeCategoryDao()
    val scheduled = mutableListOf<RecurringItem>()

    val viewModel = RecurringViewModel(
      context = dummyContext,
      recurringItemDao = recDao,
      categoryDao = catDao,
      scheduleReminder = { _, item -> scheduled.add(item) },
      cancelReminder = { _, _ -> }
    )

    viewModel.add(
      amount = 500_000L,
      categoryId = 2L,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = 20100L
    )
    advanceUntilIdle()

    assertEquals(1, recDao.inserted.size)
    assertEquals(500_000L, recDao.inserted[0].amount)
    assertEquals(2L, recDao.inserted[0].categoryId)

    assertEquals(1, scheduled.size)
    assertEquals(recDao.inserted[0].id, scheduled[0].id)
    assertEquals(500_000L, scheduled[0].amount)
  }

  @Test
  fun update_updatesItemAndSchedulesReminder() = runTest(testDispatcher) {
    val existing = RecurringItem(15L, 1L, 100_000L, RecurringFrequency.WEEKLY, 20000L)
    val recDao = FakeRecurringItemDao(listOf(existing))
    val catDao = FakeCategoryDao()
    val scheduled = mutableListOf<RecurringItem>()

    val viewModel = RecurringViewModel(
      context = dummyContext,
      recurringItemDao = recDao,
      categoryDao = catDao,
      scheduleReminder = { _, item -> scheduled.add(item) },
      cancelReminder = { _, _ -> }
    )

    viewModel.update(
      item = existing,
      amount = 150_000L,
      categoryId = 1L,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = 20030L
    )
    advanceUntilIdle()

    assertEquals(1, recDao.updated.size)
    assertEquals(150_000L, recDao.updated[0].amount)
    assertEquals(RecurringFrequency.MONTHLY, recDao.updated[0].frequency)
    assertEquals(20030L, recDao.updated[0].nextDueDate)

    assertEquals(1, scheduled.size)
    assertEquals(150_000L, scheduled[0].amount)
  }

  @Test
  fun delete_deletesItemAndCancelsReminder() = runTest(testDispatcher) {
    val existing = RecurringItem(25L, 1L, 200_000L, RecurringFrequency.MONTHLY, 20000L)
    val recDao = FakeRecurringItemDao(listOf(existing))
    val catDao = FakeCategoryDao()
    val cancelledIds = mutableListOf<Long>()

    val viewModel = RecurringViewModel(
      context = dummyContext,
      recurringItemDao = recDao,
      categoryDao = catDao,
      scheduleReminder = { _, _ -> },
      cancelReminder = { _, id -> cancelledIds.add(id) }
    )

    viewModel.delete(existing)
    advanceUntilIdle()

    assertEquals(1, recDao.deleted.size)
    assertEquals(25L, recDao.deleted[0].id)

    assertEquals(listOf(25L), cancelledIds)
  }

  @Test
  fun defaultReminderWiring_schedulesAndCancelsAlarmsInAlarmManager() = runTest(testDispatcher) {
    val context = RuntimeEnvironment.getApplication()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAlarmManager = Shadows.shadowOf(alarmManager)

    val recDao = FakeRecurringItemDao()
    val catDao = FakeCategoryDao()

    val viewModel = RecurringViewModel(
      context = context,
      recurringItemDao = recDao,
      categoryDao = catDao
    )

    viewModel.add(
      amount = 150_000L,
      categoryId = 1L,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = 20050L
    )
    advanceUntilIdle()

    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    val scheduledAlarm = shadowAlarmManager.scheduledAlarms.first()
    val insertedItem = recDao.inserted.single()
    assertEquals(insertedItem.id.toInt(), Shadows.shadowOf(scheduledAlarm.operation).requestCode)
    assertEquals(AlarmManager.RTC_WAKEUP, scheduledAlarm.type)
    assertTrue(scheduledAlarm.isAllowWhileIdle)

    viewModel.delete(insertedItem)
    advanceUntilIdle()

    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  @Test
  fun add_whenScheduleReminderThrowsException_doesNotCrash() = runTest(testDispatcher) {
    val recDao = FakeRecurringItemDao()
    val catDao = FakeCategoryDao()

    val viewModel = RecurringViewModel(
      context = dummyContext,
      recurringItemDao = recDao,
      categoryDao = catDao,
      scheduleReminder = { _, _ -> throw RuntimeException("AlarmManager failed") },
      cancelReminder = { _, _ -> }
    )

    viewModel.add(
      amount = 500_000L,
      categoryId = 2L,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = 20100L
    )
    advanceUntilIdle()

    assertEquals(1, recDao.inserted.size)
  }
}
