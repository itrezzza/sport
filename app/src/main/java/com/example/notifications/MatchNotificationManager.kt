package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.model.WatchedMatch
import com.example.data.network.SportsRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object MatchNotificationManager {

    const val CHANNEL_LIVE_SCORES = "khatebazi_live_scores"
    const val CHANNEL_KICKOFF = "khatebazi_kickoff_reminders"
    const val EXTRA_MATCH_ID = "extra_match_id"
    private const val MONITOR_WORK_NAME = "khatebazi_live_score_monitor"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_SCORES,
            "تغییر نتیجه و گل‌های زنده",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان فوری هنگام تغییر نتیجه، شروع یا پایان مسابقات دنبال‌شده"
            enableVibration(true)
        }

        val kickoffChannel = NotificationChannel(
            CHANNEL_KICKOFF,
            "یادآور شروع بازی",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "یادآور پیش از آغاز مسابقات انتخاب‌شده"
            enableVibration(true)
        }

        manager.createNotificationChannel(liveChannel)
        manager.createNotificationChannel(kickoffChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showScoreChangeNotification(
        context: Context,
        matchId: String,
        leagueTitle: String,
        hostName: String,
        guestName: String,
        hostGoals: Int,
        guestGoals: Int
    ) {
        val h = SportsRepository.toPersianDigits(hostGoals)
        val g = SportsRepository.toPersianDigits(guestGoals)
        postNotification(
            context = context,
            channelId = CHANNEL_LIVE_SCORES,
            notificationId = ("score_$matchId").hashCode(),
            matchId = matchId,
            title = "⚽ تغییر نتیجه · $leagueTitle",
            body = "$hostName  $h : $g  $guestName"
        )
    }

    fun showMatchStartedNotification(
        context: Context,
        matchId: String,
        leagueTitle: String,
        hostName: String,
        guestName: String
    ) {
        postNotification(
            context = context,
            channelId = CHANNEL_LIVE_SCORES,
            notificationId = ("start_$matchId").hashCode(),
            matchId = matchId,
            title = "🔴 شروع مسابقه · $leagueTitle",
            body = "$hostName مقابل $guestName هم‌اکنون آغاز شد!"
        )
    }

    fun showMatchFinishedNotification(
        context: Context,
        matchId: String,
        leagueTitle: String,
        hostName: String,
        guestName: String,
        hostGoals: Int?,
        guestGoals: Int?
    ) {
        val scorePart = if (hostGoals != null && guestGoals != null) {
            "نتیجهٔ نهایی: ${SportsRepository.toPersianDigits(hostGoals)} : ${SportsRepository.toPersianDigits(guestGoals)}"
        } else {
            "مسابقه به پایان رسید"
        }
        postNotification(
            context = context,
            channelId = CHANNEL_LIVE_SCORES,
            notificationId = ("end_$matchId").hashCode(),
            matchId = matchId,
            title = "🏁 پایان بازی · $leagueTitle",
            body = "$hostName - $guestName ($scorePart)"
        )
    }

    fun showKickoffReminderNotification(
        context: Context,
        matchId: String,
        leagueTitle: String,
        hostName: String,
        guestName: String,
        leadMinutes: Int
    ) {
        val timing = if (leadMinutes <= 0) {
            "هم‌اکنون در آستانهٔ شروع است"
        } else {
            "تا ${SportsRepository.toPersianDigits(leadMinutes)} دقیقهٔ دیگر آغاز می‌شود"
        }
        postNotification(
            context = context,
            channelId = CHANNEL_KICKOFF,
            notificationId = ("kickoff_$matchId").hashCode(),
            matchId = matchId,
            title = "⏰ یادآور شروع بازی · $leagueTitle",
            body = "$hostName مقابل $guestName $timing"
        )
    }

    fun showTestNotification(context: Context) {
        postNotification(
            context = context,
            channelId = CHANNEL_LIVE_SCORES,
            notificationId = 90090,
            matchId = "",
            title = "⚽ اعلان آزمایشی خط بازی",
            body = "اعلان‌های تغییر نتیجه و یادآور شروع مسابقات فعال هستند."
        )
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        matchId: String,
        title: String,
        body: String
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (matchId.isNotBlank()) {
                putExtra(EXTRA_MATCH_ID, matchId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun scheduleKickoffReminder(
        context: Context,
        match: WatchedMatch,
        leadMinutes: Int
    ) {
        val kickoffMillis = parseIsoUtcMillis(match.startOnUtc) ?: return
        val triggerAt = kickoffMillis - TimeUnit.MINUTES.toMillis(leadMinutes.toLong())
        val delayMillis = triggerAt - System.currentTimeMillis()
        if (delayMillis <= 0L) return

        val request = OneTimeWorkRequestBuilder<MatchKickoffWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    MatchKickoffWorker.KEY_MATCH_ID to match.id,
                    MatchKickoffWorker.KEY_LEAD_MINUTES to leadMinutes
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "kickoff_${match.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelKickoffReminder(context: Context, matchId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("kickoff_$matchId")
    }

    fun syncBackgroundScoreMonitor(context: Context, active: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!active) {
            wm.cancelUniqueWork(MONITOR_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<LiveScoreMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(
            MONITOR_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun parseIsoUtcMillis(raw: String): Long? {
        if (raw.isBlank()) return null
        val cleaned = raw.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(cleaned)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
