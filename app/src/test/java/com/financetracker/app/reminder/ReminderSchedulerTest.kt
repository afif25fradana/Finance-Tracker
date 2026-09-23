package com.financetracker.app.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import com.financetracker.app.data.entity.RecurringFrequency
import com.financetracker.app.data.entity.RecurringItem
import java.time.LocalDate
import java.time.ZoneId
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {

  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var shadowAlarmManager: ShadowAlarmManager
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    shadowAlarmManager = Shadows.shadowOf(alarmManager)
    notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowNotificationManager = Shadows.shadowOf(notificationManager)
  }

  @Test
  fun schedule_registersInexactAlarmAtNineAmLocalTime() {
    val dueDate = 20050L
    val item = RecurringItem(
      id = 42L,
      categoryId = 1L,
      amount = 100_000L,
      frequency = RecurringFrequency.MONTHLY,
      nextDueDate = dueDate
    )

    ReminderScheduler.schedule(context, item)

    val scheduled = shadowAlarmManager.scheduledAlarms
    assertEquals(1, scheduled.size)
    val alarm = scheduled.first()
    assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
    assertTrue(alarm.isAllowWhileIdle)

    val expectedTrigger = LocalDate.ofEpochDay(dueDate)
      .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedTrigger, alarm.triggerAtMs)
  }

  @Test
  fun schedule_createsPendingIntentWithItemMetadata() {
    val item = RecurringItem(
      id = 99L,
      categoryId = 2L,
      amount = 50_000L,
      frequency = RecurringFrequency.WEEKLY,
      nextDueDate = 20000L
    )

    ReminderScheduler.schedule(context, item)

    val alarm = shadowAlarmManager.scheduledAlarms.first()
    val shadowPi = Shadows.shadowOf(alarm.operation)
    assertTrue(shadowPi.isBroadcast)
    assertEquals(99, shadowPi.requestCode)
    assertTrue(shadowPi.isImmutable)
    assertEquals(ReminderScheduler.ACTION_FIRE, shadowPi.savedIntent.action)
    assertEquals(99L, shadowPi.savedIntent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L))
    assertEquals(ReminderReceiver::class.java.name, shadowPi.savedIntent.component?.className)
  }

  @Test
  fun schedule_createsNotificationChannelWithDefaultImportance() {
    val item = RecurringItem(
      id = 1L,
      categoryId = 1L,
      amount = 10_000L,
      frequency = RecurringFrequency.DAILY,
      nextDueDate = 20000L
    )

    ReminderScheduler.schedule(context, item)

    val channel = notificationManager.getNotificationChannel(ReminderScheduler.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(ReminderScheduler.CHANNEL_ID, channel.id)
    assertEquals("Recurring reminders", channel.name.toString())
    assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
  }

  @Test
  fun cancel_removesMatchingAlarmFromAlarmManager() {
    val item1 = RecurringItem(id = 10L, categoryId = 1L, amount = 1000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)
    val item2 = RecurringItem(id = 20L, categoryId = 1L, amount = 2000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)

    ReminderScheduler.schedule(context, item1)
    ReminderScheduler.schedule(context, item2)
    assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

    ReminderScheduler.cancel(context, 10L)

    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    val remaining = shadowAlarmManager.scheduledAlarms.first()
    assertEquals(20, Shadows.shadowOf(remaining.operation).requestCode)
  }

  @Test
  fun cancelAll_cancelsEverySpecifiedAlarm() {
    val item1 = RecurringItem(id = 101L, categoryId = 1L, amount = 1000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)
    val item2 = RecurringItem(id = 102L, categoryId = 1L, amount = 2000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)
    val item3 = RecurringItem(id = 103L, categoryId = 1L, amount = 3000L, frequency = RecurringFrequency.DAILY, nextDueDate = 20000L)

    ReminderScheduler.schedule(context, item1)
    ReminderScheduler.schedule(context, item2)
    ReminderScheduler.schedule(context, item3)
    assertEquals(3, shadowAlarmManager.scheduledAlarms.size)

    ReminderScheduler.cancelAll(context, listOf(101L, 103L))

    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    val remaining = shadowAlarmManager.scheduledAlarms.first()
    assertEquals(102, Shadows.shadowOf(remaining.operation).requestCode)

    ReminderScheduler.cancelAll(context, listOf(102L))
    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  @Test
  fun rescheduleAll_schedulesAlarmsForAllItems() {
    val items = listOf(
      RecurringItem(id = 1L, categoryId = 1L, amount = 100L, frequency = RecurringFrequency.DAILY, nextDueDate = 20001L),
      RecurringItem(id = 2L, categoryId = 1L, amount = 200L, frequency = RecurringFrequency.WEEKLY, nextDueDate = 20007L),
      RecurringItem(id = 3L, categoryId = 1L, amount = 300L, frequency = RecurringFrequency.MONTHLY, nextDueDate = 20030L)
    )

    ReminderScheduler.rescheduleAll(context, items)

    val scheduled = shadowAlarmManager.scheduledAlarms
    assertEquals(3, scheduled.size)
    val requestCodes = scheduled.map { Shadows.shadowOf(it.operation).requestCode }.toSet()
    assertEquals(setOf(1, 2, 3), requestCodes)

    for (item in items) {
      val expectedTrigger = LocalDate.ofEpochDay(item.nextDueDate)
        .atTime(ReminderScheduler.DEFAULT_REMINDER_HOUR, ReminderScheduler.DEFAULT_REMINDER_MINUTE)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
      val alarm = scheduled.first { Shadows.shadowOf(it.operation).requestCode == item.id.toInt() }
      assertEquals(expectedTrigger, alarm.triggerAtMs)
    }
  }
}
