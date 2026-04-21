package com.jnetai.subaudit.ui.notifications

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jnetai.subaudit.R
import com.jnetai.subaudit.ui.dashboard.DashboardActivity
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "subaudit_reminders"
        const val EXTRA_SUB_NAME = "sub_name"
        const val EXTRA_SUB_COST = "sub_cost"
        const val EXTRA_SUB_CURRENCY = "sub_currency"
        const val EXTRA_SUB_ID = "sub_id"

        fun scheduleReminder(context: Context, subId: String, subName: String, cost: Double, currency: String, paymentDate: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_SUB_ID, subId)
                putExtra(EXTRA_SUB_NAME, subName)
                putExtra(EXTRA_SUB_COST, cost)
                putExtra(EXTRA_SUB_CURRENCY, currency)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                subId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Remind 1 day before payment
            val reminderTime = paymentDate - (24 * 60 * 60 * 1000)
            if (reminderTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            }
        }

        fun cancelReminder(context: Context, subId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                subId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Subscription Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for upcoming subscription payments"
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val subName = intent.getStringExtra(EXTRA_SUB_NAME) ?: "Subscription"
        val subCost = intent.getDoubleExtra(EXTRA_SUB_COST, 0.0)
        val subCurrency = intent.getStringExtra(EXTRA_SUB_CURRENCY) ?: "GBP"

        val currencySymbol = when (subCurrency) {
            "USD" -> "$"
            "EUR" -> "€"
            else -> "£"
        }

        createNotificationChannel(context)

        val mainIntent = Intent(context, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Payment")
            .setContentText("$subName - $currencySymbol${String.format("%.2f", subCost)} due tomorrow")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(subName.hashCode(), notification)
    }
}