package com.financetracker.app.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.financetracker.app.MainActivity
import com.financetracker.app.R
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.RecurringItem
import com.financetracker.app.ui.components.formatRupiah
import com.financetracker.app.ui.components.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ReminderScheduler.ACTION_FIRE -> {
        val itemId = intent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return
        val pendingResult = goAsync()
        receiverScope.launch {
          try {
            onReminderFired(context.applicationContext, itemId)
          } finally {
            pendingResult.finish()
          }
        }
      }

      Intent.ACTION_BOOT_COMPLETED -> {
        val pendingResult = goAsync()
        receiverScope.launch {
          try {
            val items = AppDatabase.getInstance(context.applicationContext)
              .recurringItemDao()
              .getAllOnce()
            ReminderScheduler.rescheduleAll(context.applicationContext, items)
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }

  /** Post a notification only — never writes a Transaction — then advance to the next occurrence. */
  private suspend fun onReminderFired(context: Context, itemId: Long) {
    val db = AppDatabase.getInstance(context)
    val item = db.recurringItemDao().getByIdOnce(itemId)
    if (item == null) {
      ReminderScheduler.cancel(context, itemId)
      return
    }

    if (canPostNotifications(context)) {
      ReminderScheduler.ensureChannel(context)
      val category = db.categoryDao().getByIdOnce(item.categoryId)
      val categoryName = category?.name ?: "Reminder"
      val contentIntent = PendingIntent.getActivity(
        context,
        itemId.toInt(),
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("$categoryName · ${formatRupiah(item.amount)}")
        .setContentText("Due today. Tap to log it manually — nothing was auto-added.")
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .build()
      NotificationManagerCompat.from(context).notify(itemId.toInt(), notification)
    }

    val nextDue = item.frequency.nextDueEpochDay(item.nextDueDate, todayEpochDay())
    val advanced = item.copy(nextDueDate = nextDue)
    db.recurringItemDao().update(advanced)
    ReminderScheduler.schedule(context, advanced)
  }

  private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
}
