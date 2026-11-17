package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FavoriteTeam
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchEntry
import com.example.data.model.MatchSummary
import com.example.data.model.NotificationSettings
import com.example.data.model.TeamInfo
import com.example.data.model.WatchedMatch
import com.example.data.network.SportsRepository
import com.example.ui.components.AnimatedScoreboard
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.ScoreboardSize
import com.example.ui.components.TeamLogo
import com.example.ui.components.bouncyClickable
import com.example.ui.theme.LiveCrimson

private data class DiscoverableTeam(
    val team: TeamInfo,
    val leagueTitle: String,
    val sport: Int
)

@Composable
fun FavoritesScreen(
    favoriteTeams: List<FavoriteTeam>,
    leagues: List<LeagueGroup>,
    watchedMatches: List<WatchedMatch>,
    notificationSettings: NotificationSettings,
    onToggleFavoriteTeam: (TeamInfo, String, Int) -> Unit,
    onTeamClick: (TeamInfo, String, Int) -> Unit,
    onMatchClick: (MatchSummary, String, Int) -> Unit,
    onToggleWatchMatch: (MatchSummary, String) -> Unit,
    onUnwatchMatch: (String) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    var teamQuery by rememberSaveable { mutableStateOf("") }
    val favoriteNames = remember(favoriteTeams) {
        favoriteTeams.map { it.name.lowercase() }.toSet()
    }
    val watchedIds = remember(watchedMatches) {
        watchedMatches.map { it.id }.toSet()
    }

    val discoverableTeams = remember(leagues, teamQuery) {
        val seen = linkedMapOf<String, DiscoverableTeam>()
        leagues.forEach { league ->
            league.matches.forEach { match ->
                listOf(match.host, match.guest).forEach { team ->
                    val key = team.name.trim().lowercase()
                    if (key.isNotBlank() && !seen.containsKey(key)) {
                        seen[key] = DiscoverableTeam(team, league.title, league.sport)
                    }
                }
            }
        }
        val cleanQ = teamQuery.trim()
        seen.values
            .filter { cleanQ.isBlank() || it.team.name.contains(cleanQ, ignoreCase = true) || it.leagueTitle.contains(cleanQ, ignoreCase = true) }
            .take(18)
    }

    val favoriteMatches = remember(leagues, favoriteNames) {
        if (favoriteNames.isEmpty()) {
            emptyList()
        } else {
            leagues.flatMap { league ->
                league.matches.filter { m ->
                    favoriteNames.any { fav ->
                        m.host.name.equals(fav, ignoreCase = true) ||
                            m.guest.name.equals(fav, ignoreCase = true)
                    }
                }.map { m ->
                    MatchEntry(match = m, leagueTitle = league.title, sport = league.sport)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen"),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Followed Teams Rail
        item(key = "followed_teams_section") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FavoritesSectionHeader(
                    title = "تیم‌های محبوب من",
                    badgeText = if (favoriteTeams.isNotEmpty()) "${SportsRepository.toPersianDigits(favoriteTeams.size)} تیم" else null,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (favoriteTeams.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmptyCrestLottie(size = 56.dp)
                            Text(
                                text = "هنوز باشگاهی دنبال نکرده‌ای! از لیست زیر تیم محبوبت را انتخاب کن.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("favorites_followed_rail")
                    ) {
                        items(favoriteTeams, key = { "fav_${it.name}" }) { fav ->
                            Surface(
                                modifier = Modifier
                                    .width(126.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .bouncyClickable {
                                        onTeamClick(
                                            TeamInfo(name = fav.name, logoUrl = fav.logoUrl),
                                            fav.leagueTitle,
                                            fav.sport
                                        )
                                    }
                                    .testTag("favorite_team_card_${fav.name}"),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        TeamLogo(
                                            logoUrl = fav.logoUrl,
                                            teamName = fav.name,
                                            size = 44.dp,
                                            highlightBorder = true,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = "حذف تیم محبوب",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(18.dp)
                                                .clickable {
                                                    onToggleFavoriteTeam(
                                                        TeamInfo(name = fav.name, logoUrl = fav.logoUrl),
                                                        fav.leagueTitle,
                                                        fav.sport
                                                    )
                                                }
                                                .testTag("remove_fav_team_${fav.name}")
                                        )
                                    }
                                    Text(
                                        text = fav.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (fav.leagueTitle.isNotBlank()) {
                                        Text(
                                            text = fav.leagueTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Favorite Teams' Matches Feed
        if (favoriteMatches.isNotEmpty()) {
            item(key = "favorites_team_matches") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("favorites_team_matches"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FavoritesSectionHeader(
                        title = "بازی‌های تیم‌های محبوب",
                        badgeText = "${SportsRepository.toPersianDigits(favoriteMatches.size)} مسابقه"
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column {
                            favoriteMatches.forEachIndexed { idx, entry ->
                                val match = entry.match
                                val isWatched = match.id in watchedIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMatchClick(match, entry.leagueTitle, entry.sport) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (match.isLive) {
                                        LivePulseIndicator(size = 6.dp)
                                    }
                                    Text(
                                        text = when {
                                            match.isLive -> match.liveTime.ifBlank { "زنده" }
                                            match.goals != null -> "پایان"
                                            else -> match.time.ifBlank { "–" }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (match.isLive) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(42.dp),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = match.host.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AnimatedScoreboard(
                                        goals = match.goals,
                                        isLive = match.isLive,
                                        size = ScoreboardSize.COMPACT
                                    )
                                    Text(
                                        text = match.guest.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (match.id.isNotBlank()) {
                                        IconButton(
                                            onClick = { onToggleWatchMatch(match, entry.leagueTitle) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isWatched) Icons.Rounded.NotificationsActive else Icons.Outlined.NotificationsNone,
                                                contentDescription = stringResource(R.string.cd_match_notifications),
                                                tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (idx < favoriteMatches.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Team Discovery Picker
        item(key = "team_discovery_section") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FavoritesSectionHeader(title = "افزودن باشگاه از لیگ‌های امروز")

                OutlinedTextField(
                    value = teamQuery,
                    onValueChange = { teamQuery = it },
                    singleLine = true,
                    placeholder = { Text("جستجوی نام باشگاه یا لیگ…", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (teamQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { teamQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorites_team_search_input")
                )

                if (discoverableTeams.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoverableTeams, key = { "disc_${it.team.name}" }) { item ->
                            val isFav = item.team.name.lowercase() in favoriteNames
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFav) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isFav) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onToggleFavoriteTeam(item.team, item.leagueTitle, item.sport)
                                    }
                                    .testTag("discover_team_${item.team.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TeamLogo(logoUrl = item.team.logoUrl, teamName = item.team.name, size = 24.dp)
                                    Text(
                                        text = item.team.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Watched Matches Alerts
        if (watchedMatches.isNotEmpty()) {
            item(key = "favorites_watched_section") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("favorites_watched_matches"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FavoritesSectionHeader(
                        title = "هشدارهای فعال مسابقات",
                        badgeText = "${SportsRepository.toPersianDigits(watchedMatches.size)} بازی"
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column {
                            watchedMatches.forEachIndexed { idx, watched ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${watched.hostName} ـ ${watched.guestName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = watched.leagueTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onUnwatchMatch(watched.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsOff,
                                            contentDescription = stringResource(R.string.cd_unwatch_match),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (idx < watchedMatches.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Smart Notification Preferences Panel
        item(key = "favorites_notification_prefs") {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("favorites_notifications_panel"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoritesSectionHeader(title = "تنظیمات اعلان و یادآور")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("فعال‌سازی هشدارهای ورزشی", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "دریافت یادآور شروع بازی و اعلان گل‌ها",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationSettings.enabled,
                                onCheckedChange = {
                                    onUpdateNotificationSettings(notificationSettings.copy(enabled = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("fav_notifications_master_switch")
                            )
                        }

                        if (notificationSettings.enabled) {
                            HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("یادآور پیش از سوت آغاز", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = notificationSettings.notifyOnKickoff,
                                    onCheckedChange = {
                                        onUpdateNotificationSettings(notificationSettings.copy(notifyOnKickoff = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            if (notificationSettings.notifyOnKickoff) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        0 to "لحظهٔ شروع",
                                        5 to "۵ دقیقه قبل",
                                        15 to "۱۵ دقیقه قبل"
                                    ).forEach { (minutes, label) ->
                                        FilterChip(
                                            selected = notificationSettings.kickoffLeadMinutes == minutes,
                                            onClick = {
                                                onUpdateNotificationSettings(
                                                    notificationSettings.copy(kickoffLeadMinutes = minutes)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("اعلان تغییر نتیجه و گل زنده", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = notificationSettings.notifyOnScoreChange,
                                    onCheckedChange = {
                                        onUpdateNotificationSettings(notificationSettings.copy(notifyOnScoreChange = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = onSendTestNotification,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("fav_test_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ارسال اعلان آزمایشی", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        item(key = "favorites_bottom_pad") {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun FavoritesSectionHeader(
    title: String,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (!badgeText.isNullOrBlank()) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
