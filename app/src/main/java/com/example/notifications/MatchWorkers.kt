package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.network.SportsRepository
import com.example.data.preferences.SettingsStore
import kotlinx.coroutines.flow.first

class MatchKickoffWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val matchId = inputData.getString(KEY_MATCH_ID) ?: return Result.success()
        val leadMinutes = inputData.getInt(KEY_LEAD_MINUTES, 5)
        val store = SettingsStore(applicationContext)
        val settings = store.notificationSettingsFlow.first()
        if (!settings.enabled || !settings.notifyOnKickoff) return Result.success()

        val watched = store.watchedMatchesFlow.first()[matchId] ?: return Result.success()
        MatchNotificationManager.showKickoffReminderNotification(
            context = applicationContext,
            matchId = watched.id,
            leagueTitle = watched.leagueTitle,
            hostName = watched.hostName,
            guestName = watched.guestName,
            leadMinutes = leadMinutes
        )
        return Result.success()
    }

    companion object {
        const val KEY_MATCH_ID = "match_id"
        const val KEY_LEAD_MINUTES = "lead_minutes"
    }
}

class LiveScoreMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val settings = store.notificationSettingsFlow.first()
        val watchedMap = store.watchedMatchesFlow.first().toMutableMap()
        if (!settings.enabled || watchedMap.isEmpty()) {
            return Result.success()
        }

        val repository = SportsRepository()
        val leagues = runCatching { repository.fetchVarzesh3LiveScores() }.getOrElse {
            return Result.retry()
        }

        var changed = false
        leagues.forEach { league ->
            league.matches.forEach { match ->
                val prev = watchedMap[match.id] ?: return@forEach
                val newHost = match.goals?.host
                val newGuest = match.goals?.guest
                val isFinished = !match.isLive && match.goals != null &&
                    (match.statusTitle.contains("پایان") || prev.wasLive)

                if (settings.notifyOnKickoff && !prev.wasLive && match.isLive) {
                    MatchNotificationManager.showMatchStartedNotification(
                        context = applicationContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name
                    )
                }

                if (settings.notifyOnScoreChange &&
                    newHost != null && newGuest != null &&
                    (prev.lastHostGoals != null && prev.lastGuestGoals != null) &&
                    (newHost != prev.lastHostGoals || newGuest != prev.lastGuestGoals)
                ) {
                    MatchNotificationManager.showScoreChangeNotification(
                        context = applicationContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name,
                        hostGoals = newHost,
                        guestGoals = newGuest
                    )
                }

                if (settings.notifyOnScoreChange && !prev.wasFinished && isFinished) {
                    MatchNotificationManager.showMatchFinishedNotification(
                        context = applicationContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name,
                        hostGoals = newHost,
                        guestGoals = newGuest
                    )
                }

                val updated = prev.copy(
                    lastHostGoals = newHost ?: prev.lastHostGoals,
                    lastGuestGoals = newGuest ?: prev.lastGuestGoals,
                    wasLive = match.isLive,
                    wasFinished = prev.wasFinished || isFinished
                )
                if (updated != prev) {
                    watchedMap[match.id] = updated
                    changed = true
                }
            }
        }

        if (changed) {
            store.saveWatchedMatches(watchedMap)
        }
        return Result.success()
    }
}
