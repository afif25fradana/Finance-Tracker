package com.financetracker.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.Category
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.components.todayEpochDay
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderReceiverTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var shadowAlarmManager: ShadowAlarmManager
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    db = AppDatabase.createInMemory(context, withDefaultSeed = false)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    shadowAlarmManager = Shadows.shadowOf(alarmManager)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowNotificationManager = Shadows.shadowOf(notificationManager)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun onReceive_bootCompleted_reschedulesAllRecurringItemsFromDatabase() = runTest(testDispatcher) {
    val catId = db.categoryDao().insert(
      Category(name = "Bills", type = TransactionType.EXPENSE, color = 0xFF112233, icon = "receipt", isDefault = true)
    )
    val item1Id = db.recurringItemDao().insert(
      RecurringItem(categoryId = catId, amount = 100_000L, frequency = RecurringFrequency.MONTHLY, nextDueDate = 20000L)
    )
    val item2Id = db.recurringItemDao().insert(
      RecurringItem(categoryId = catId, amount = 250_000L, frequency = RecurringFrequency.WEEKLY, nextDueDate = 20010L)
    )

    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    receiver.lastJob?.join()

    val scheduled = shadowAlarmManager.scheduledAlarms
    assertEquals(2, scheduled.size)

    val alarm1 = scheduled.first { Shadows.shadowOf(it.operation).requestCode == item1Id.toInt() }
    assertEquals(AlarmManager.RTC_WAKEUP, alarm1.type)
    assertTrue(alarm1.isAllowWhileIdle)
    val expectedTrigger1 = LocalDate.ofEpochDay(20000L)
      .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedTrigger1, alarm1.triggerAtMs)
    val shadowPi1 = Shadows.shadowOf(alarm1.operation)
    assertEquals(ReminderScheduler.ACTION_FIRE, shadowPi1.savedIntent.action)
    assertEquals(item1Id, shadowPi1.savedIntent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L))

    val alarm2 = scheduled.first { Shadows.shadowOf(it.operation).requestCode == item2Id.toInt() }
    assertEquals(AlarmManager.RTC_WAKEUP, alarm2.type)
    assertTrue(alarm2.isAllowWhileIdle)
    val expectedTrigger2 = LocalDate.ofEpochDay(20010L)
      .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedTrigger2, alarm2.triggerAtMs)
    val shadowPi2 = Shadows.shadowOf(alarm2.operation)
    assertEquals(ReminderScheduler.ACTION_FIRE, shadowPi2.savedIntent.action)
    assertEquals(item2Id, shadowPi2.savedIntent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L))
  }

  @Test
  fun onReceive_bootCompleted_withNoItems_schedulesNothing() = runTest(testDispatcher) {
    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    receiver.lastJob?.join()

    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  @Test
  fun onReceive_fire_postsNotificationAndAdvancesDueDateAndReschedules() = runTest(testDispatcher) {
    Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val catId = db.categoryDao().insert(
      Category(name = "Groceries", type = TransactionType.EXPENSE, color = 0xFF445566, icon = "shopping_cart", isDefault = true)
    )
    val initialDueDate = 20000L
    val itemId = db.recurringItemDao().insert(
      RecurringItem(categoryId = catId, amount = 150_000L, frequency = RecurringFrequency.MONTHLY, nextDueDate = initialDueDate)
    )

    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    val intent = Intent(ReminderScheduler.ACTION_FIRE).putExtra(ReminderScheduler.EXTRA_ITEM_ID, itemId)
    receiver.onReceive(context, intent)
    receiver.lastJob?.join()

    val notification = shadowNotificationManager.getNotification(itemId.toInt())
    assertNotNull(notification)
    assertEquals(ReminderScheduler.CHANNEL_ID, notification.channelId)
    assertEquals("Groceries · ${formatRupiah(150_000L)}", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    assertEquals("Due today. Tap to log it manually — nothing was auto-added.", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())

    val updatedItem = db.recurringItemDao().getByIdOnce(itemId)
    assertNotNull(updatedItem)
    val expectedNextDue = RecurringFrequency.MONTHLY.nextDueEpochDay(initialDueDate, todayEpochDay())
    assertEquals(expectedNextDue, updatedItem!!.nextDueDate)
    assertTrue(updatedItem.nextDueDate > initialDueDate)

    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    val scheduled = shadowAlarmManager.scheduledAlarms.first()
    val expectedTrigger = LocalDate.ofEpochDay(expectedNextDue)
      .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedTrigger, scheduled.triggerAtMs)
    val shadowPi = Shadows.shadowOf(scheduled.operation)
    assertEquals(itemId.toInt(), shadowPi.requestCode)
    assertEquals(ReminderScheduler.ACTION_FIRE, shadowPi.savedIntent.action)
    assertEquals(itemId, shadowPi.savedIntent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L))
  }

  @Test
  fun onReceive_fire_whenItemNotFound_cancelsAlarmWithoutPostingNotification() = runTest(testDispatcher) {
    Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val nonExistentId = 999L
    ReminderScheduler.schedule(
      context,
      RecurringItem(id = nonExistentId, categoryId = 1L, amount = 50_000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)
    )
    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    val intent = Intent(ReminderScheduler.ACTION_FIRE).putExtra(ReminderScheduler.EXTRA_ITEM_ID, nonExistentId)
    receiver.onReceive(context, intent)
    receiver.lastJob?.join()

    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    assertTrue(shadowNotificationManager.allNotifications.isEmpty())
  }

  @Test
  fun onReceive_fire_whenNotificationPermissionDenied_advancesDueDateAndReschedulesWithoutNotification() = runTest(testDispatcher) {
    Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val catId = db.categoryDao().insert(
      Category(name = "Internet", type = TransactionType.EXPENSE, color = 0xFF778899, icon = "wifi", isDefault = true)
    )
    val initialDueDate = 20000L
    val itemId = db.recurringItemDao().insert(
      RecurringItem(categoryId = catId, amount = 300_000L, frequency = RecurringFrequency.MONTHLY, nextDueDate = initialDueDate)
    )

    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    val intent = Intent(ReminderScheduler.ACTION_FIRE).putExtra(ReminderScheduler.EXTRA_ITEM_ID, itemId)
    receiver.onReceive(context, intent)
    receiver.lastJob?.join()

    assertTrue(shadowNotificationManager.allNotifications.isEmpty())

    val updatedItem = db.recurringItemDao().getByIdOnce(itemId)
    assertNotNull(updatedItem)
    val expectedNextDue = RecurringFrequency.MONTHLY.nextDueEpochDay(initialDueDate, todayEpochDay())
    assertEquals(expectedNextDue, updatedItem!!.nextDueDate)

    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    val scheduled = shadowAlarmManager.scheduledAlarms.first()
    val expectedTrigger = LocalDate.ofEpochDay(expectedNextDue)
      .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedTrigger, scheduled.triggerAtMs)
  }

  @Test
  fun onReceive_fire_withNegativeItemId_doesNothing() = runTest(testDispatcher) {
    val catId = db.categoryDao().insert(
      Category(name = "Health", type = TransactionType.EXPENSE, color = 0xFFAABBCC, icon = "health", isDefault = true)
    )
    val initialDueDate = 20000L
    val itemId = db.recurringItemDao().insert(
      RecurringItem(categoryId = catId, amount = 75_000L, frequency = RecurringFrequency.MONTHLY, nextDueDate = initialDueDate)
    )

    val receiver = ReminderReceiver(coroutineContext = testDispatcher, dbProvider = { db })
    val intent = Intent(ReminderScheduler.ACTION_FIRE).putExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L)
    receiver.onReceive(context, intent)
    receiver.lastJob?.join()

    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    assertTrue(shadowNotificationManager.allNotifications.isEmpty())

    val item = db.recurringItemDao().getByIdOnce(itemId)
    assertNotNull(item)
    assertEquals(initialDueDate, item!!.nextDueDate)
  }
}
