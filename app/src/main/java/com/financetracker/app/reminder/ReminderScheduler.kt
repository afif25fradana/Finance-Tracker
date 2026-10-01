package com.financetracker.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.financetracker.app.data.entity.RecurringItem
import java.time.LocalDate
import java.time.ZoneId

object ReminderScheduler {
  const val CHANNEL_ID = "recurring_reminders"
  const val ACTION_FIRE = "com.financetracker.app.reminder.FIRE"
  const val EXTRA_ITEM_ID = "itemId"
  const val DEFAULT_REMINDER_HOUR = 9
  const val DEFAULT_REMINDER_MINUTE = 0

  fun ensureChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Recurring reminders",
      NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
  }

  /** Fires an inexact one-shot alarm at 09:00 local time of [RecurringItem.nextDueDate]. */
  fun schedule(context: Context, item: RecurringItem) {
    ensureChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = LocalDate.ofEpochDay(item.nextDueDate)
      .atTime(DEFAULT_REMINDER_HOUR, DEFAULT_REMINDER_MINUTE)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      triggerAtMillis,
      firePendingIntent(context, item.id)
    )
  }

  fun cancel(context: Context, itemId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(firePendingIntent(context, itemId))
  }

  fun cancelAll(context: Context, itemIds: List<Long>) {
    itemIds.forEach { cancel(context, it) }
  }

  fun rescheduleAll(context: Context, items: List<RecurringItem>) {
    items.forEach { schedule(context, it) }
  }

  private fun firePendingIntent(context: Context, itemId: Long): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
      .setAction(ACTION_FIRE)
      .putExtra(EXTRA_ITEM_ID, itemId)
    return PendingIntent.getBroadcast(
      context,
      itemId.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}
