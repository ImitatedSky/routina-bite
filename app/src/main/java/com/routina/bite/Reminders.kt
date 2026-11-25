package com.routina.bite

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.routina.bite.data.defaultMeal
import com.routina.bite.data.todayDate
import com.routina.bite.model.Meal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 記錄提醒：使用者設幾個每天的時間（"HH:mm"），到點跳一則「記一下這餐」的通知。
 * 每個時間一個鬧鐘，響了發通知再排明天。做法與 Routina Words 的單字通知相同。
 * 存在獨立的 SharedPreferences，不進備份：提醒是這台手機的設定，不是飲食資料。
 */
object Reminders {

    private const val PREFS = "routina_bite_reminders"
    private const val KEY_TIMES = "times"
    private const val CHANNEL_ID = "meal_reminders"
    private const val FALLBACK_WINDOW_MS = 10 * 60 * 1000L

    fun times(context: Context): List<String> =
        prefs(context).getStringSet(KEY_TIMES, emptySet()).orEmpty().sorted()

    fun add(context: Context, time: LocalTime) {
        val hhmm = "%02d:%02d".format(time.hour, time.minute)
        prefs(context).edit().putStringSet(KEY_TIMES, times(context).toSet() + hhmm).apply()
        schedule(context, hhmm)
    }

    fun remove(context: Context, hhmm: String) {
        prefs(context).edit().putStringSet(KEY_TIMES, times(context).toSet() - hhmm).apply()
        alarms(context).cancel(fireIntent(context, hhmm))
    }

    fun rescheduleAll(context: Context) {
        for (hhmm in times(context)) schedule(context, hhmm)
    }

    /** 排「下一次」：今天還沒到就今天，過了就明天 */
    fun schedule(context: Context, hhmm: String) {
        val time = runCatching { LocalTime.parse(hhmm) }.getOrNull() ?: return
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(time)
        val next = if (today.isAfter(now)) today else today.plusDays(1)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarms = alarms(context)
        val intent = fireIntent(context, hhmm)
        // 一般鬧鐘會被系統延後好幾小時，提醒就失去意義；拿不到精準鬧鐘才退回十分鐘時間窗
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, FALLBACK_WINDOW_MS, intent)
        }
    }

    /** 這一餐今天已經記過就不吵；點通知直接開新增紀錄頁 */
    fun post(context: Context, hhmm: String) {
        val time = runCatching { LocalTime.parse(hhmm) }.getOrNull() ?: return
        val meal = defaultMeal(time)
        val repository = (context.applicationContext as? BiteApp)?.repository ?: return
        val today = todayDate()
        if (repository.entries.value.any { it.date == today && it.meal == meal }) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ADD, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_meal)
            .setContentTitle(context.getString(R.string.reminder_title, context.getString(mealRes(meal))))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()
        // 同一個通知 id：上一則沒點掉的直接被新的取代，不會越堆越多
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun mealRes(meal: Meal) = when (meal) {
        Meal.BREAKFAST -> R.string.meal_breakfast
        Meal.LUNCH -> R.string.meal_lunch
        Meal.DINNER -> R.string.meal_dinner
        Meal.SNACK -> R.string.meal_snack
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alarms(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun fireIntent(context: Context, hhmm: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            // 每個時間一個 PendingIntent：只差在 extra 的會被系統當成同一個
            .setData(Uri.parse("routina-bite://remind/$hhmm"))
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val NOTIFICATION_ID = 1
}

/** 鬧鐘響了就發通知並排明天；開機、App 更新、改時間或時區後鬧鐘會被清掉，全部重排 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_FIRE) {
            val hhmm = intent.data?.lastPathSegment ?: return
            // 響之前就被刪掉的時間不發也不再排
            if (hhmm !in Reminders.times(context)) return
            Reminders.post(context, hhmm)
            Reminders.schedule(context, hhmm)
            return
        }
        Reminders.rescheduleAll(context)
    }

    companion object {
        const val ACTION_FIRE = "com.routina.bite.reminder.FIRE"
    }
}
