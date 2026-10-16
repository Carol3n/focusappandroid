package com.focusday.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.provider.Settings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        createChannels(context)
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: ACTION_REFRESH
        when (action) {
            ACTION_START -> showAlert(context, intent?.getIntExtra(EXTRA_TASK_INDEX, -1) ?: -1, "start")
            ACTION_END -> showAlert(context, intent?.getIntExtra(EXTRA_TASK_INDEX, -1) ?: -1, "end")
        }
        showCurrentTask(context)
        scheduleAll(context)
    }

    companion object {
        private const val CHANNEL_STATUS = "focusday_status"
        private const val CHANNEL_ALARMS = "focusday_alarms_v2"
        private const val NOTIFICATION_STATUS = 4001
        private const val NOTIFICATION_ALARM_BASE = 5000
        private const val EXTRA_ACTION = "focusday_action"
        private const val EXTRA_TASK_INDEX = "focusday_task_index"
        private const val ACTION_REFRESH = "refresh"
        private const val ACTION_START = "task_start"
        private const val ACTION_END = "task_end"

        fun createChannels(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_STATUS, "FocusDay current task", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Ongoing task and time progress for the lock screen." })
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_ALARMS, "FocusDay task alarms", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sounds and vibrates when a task starts or its time is up."
                    enableVibration(true)
                    setSound(
                        Settings.System.DEFAULT_NOTIFICATION_URI,
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                    )
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                })
            }
        }

        private fun showAlert(context: Context, index: Int, kind: String) {
            if (index !in TaskData.tasks.indices) return
            val task = TaskData.tasks[index]
            val isStart = kind == "start"
            val title = if (isStart) "Task starting now: ${task.title}" else "Task finished: ${task.title}"
            val body = if (isStart) {
                "${displayTime(task.start)}–${displayTime(task.end)}. ${task.detail}"
            } else {
                val next = TaskData.tasks.getOrNull(index + 1)
                if (next != null) "Time's up. Next: ${next.title} at ${displayTime(next.start)}." else "Time's up. Your planned tasks for today are finished."
            }
            val openIntent = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, CHANNEL_ALARMS) else Notification.Builder(context)
            val notification = builder
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_ALARM)
                .setDefaults(Notification.DEFAULT_ALL)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ALARM_BASE + index * 2 + if (isStart) 0 else 1, notification)
        }

        fun showCurrentTask(context: Context) {
            val now = LocalTime.now()
            val minute = now.hour * 60 + now.minute
            val task = TaskData.tasks.firstOrNull { minute >= it.startMinute() && minute < it.endMinute() }
            val nextTask = TaskData.tasks.firstOrNull { minute < it.startMinute() }

            val title: String
            val detail: String
            val progress: Int
            if (task != null) {
                title = "Now: ${task.title}"
                detail = "${displayTime(task.start)}–${displayTime(task.end)} · ${task.detail}"
                val elapsed = minute - task.startMinute()
                progress = ((elapsed.toFloat() / (task.endMinute() - task.startMinute())) * 100).toInt().coerceIn(0, 100)
            } else if (nextTask != null) {
                title = "Up next: ${nextTask.title}"
                detail = "Starts ${displayTime(nextTask.start)} · ${nextTask.detail}"
                progress = 0
            } else {
                title = "Plan complete for today"
                detail = "You've reached the end of today's plan. Nice work."
                progress = 100
            }

            val openIntent = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, CHANNEL_STATUS) else Notification.Builder(context)
            val notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(title)
                .setContentText(detail)
                .setStyle(Notification.BigTextStyle().bigText(detail))
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_STATUS, notification)
        }

        fun scheduleAll(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nowMs = System.currentTimeMillis()
            val today = LocalDate.now()
            TaskData.tasks.forEachIndexed { index, task ->
                scheduleBoundary(context, am, today, index, LocalTime.parse(task.start), ACTION_START, nowMs)
                scheduleBoundary(context, am, today, index, LocalTime.parse(task.end), ACTION_END, nowMs)
            }
            scheduleRefresh(context, am, nowMs + 5 * 60 * 1000L)
        }

        private fun scheduleBoundary(
            context: Context, am: AlarmManager, date: LocalDate, index: Int,
            time: LocalTime, action: String, nowMs: Long
        ) {
            val target = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (target <= nowMs) return
            val requestCode = 6000 + index * 2 + if (action == ACTION_START) 0 else 1
            val intent = Intent(context, TaskReceiver::class.java).apply {
                putExtra(EXTRA_ACTION, action)
                putExtra(EXTRA_TASK_INDEX, index)
            }
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Exact alarms may require the user to allow "Alarms & reminders".
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
                }
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, target, pi)
            }
        }

        private fun scheduleRefresh(context: Context, am: AlarmManager, target: Long) {
            val pi = PendingIntent.getBroadcast(
                context, 4002, Intent(context, TaskReceiver::class.java).apply {
                    putExtra(EXTRA_ACTION, ACTION_REFRESH)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
            else am.set(AlarmManager.RTC_WAKEUP, target, pi)
        }

        fun refreshNow(context: Context) {
            createChannels(context)
            showCurrentTask(context)
            scheduleAll(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) TaskReceiver.refreshNow(context)
    }
}
