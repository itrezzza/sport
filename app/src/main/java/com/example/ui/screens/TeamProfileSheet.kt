package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LineupPlayer
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.model.TeamProfileData
import com.example.data.network.SportsRepository
import com.example.ui.components.AnimatedScoreboard
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.FavoriteTeamButton
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.RollingScoreText
import com.example.ui.components.ScoreboardSize
import com.example.ui.components.TeamFormVicoChart
import com.example.ui.components.TeamLogo
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.LiveCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamProfileSheet(
    profile: TeamProfileData,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onMatchClick: (MatchSummary, String, Int) -> Unit,
    onBroadcastClick: (MergedBroadcastGame) -> Unit = {},
    onPlayerClick: (LineupPlayer, String, String) -> Unit = { _, _, _ -> },
    onArticleClick: (NewsArticle) -> Unit = {},
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val record = remember(profile.matches, profile.teamName) {
        var wins = 0
        var draws = 0
        var losses = 0
        var goalsFor = 0
        var goalsAgainst = 0
        profile.matches.forEach { entry ->
            val g = entry.match.goals ?: return@forEach
            val h = g.host ?: return@forEach
            val a = g.guest ?: return@forEach
            val isHost = entry.match.host.name.contains(profile.teamName, ignoreCase = true)
            val gf = if (isHost) h else a
            val ga = if (isHost) a else h
            goalsFor += gf
            goalsAgainst += ga
            when {
                gf > ga -> wins += 1
                gf == ga -> draws += 1
                else -> losses += 1
            }
        }
        TeamAggregateRecord(
            played = wins + draws + losses,
            wins = wins,
            draws = draws,
            losses = losses,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .testTag("team_profile_sheet"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .testTag("team_profile_list"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "team_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("close_team_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_close)
                                    )
                                }
                            }

                            Text(
                                text = "پروفایل باشگاه",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            FavoriteTeamButton(
                                isFavorite = isFavorite,
                                teamName = profile.teamName,
                                onToggle = onToggleFavorite,
                                modifier = Modifier.testTag("team_profile_favorite_button")
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TeamLogo(
                                    logoUrl = profile.logoUrl,
                                    teamName = profile.teamName,
                                    size = 72.dp,
                                    highlightBorder = true
                                )
                                Text(
                                    text = profile.teamName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (profile.leagueTitle.isNotBlank()) {
                                        ProfileMetaChip(
                                            text = profile.leagueTitle,
                                            accent = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (profile.formation.isNotBlank()) {
                                        ProfileMetaChip(
                                            text = "آرایش ${profile.formation}",
                                            accent = BroadcastCyan
                                        )
                                    }
                                    if (profile.coach.isNotBlank()) {
                                        ProfileMetaChip(
                                            text = "سرمربی: ${profile.coach}",
                                            accent = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (record.played > 0) {
                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        TeamKpiCell(
                                            label = "مسابقه",
                                            value = SportsRepository.toPersianDigits(record.played),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TeamKpiCell(
                                            label = "برد / مساوی / باخت",
                                            value = "${SportsRepository.toPersianDigits(record.wins)}-${SportsRepository.toPersianDigits(record.draws)}-${SportsRepository.toPersianDigits(record.losses)}",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TeamKpiCell(
                                            label = "گل زده / خورده",
                                            value = "${SportsRepository.toPersianDigits(record.goalsFor)} : ${SportsRepository.toPersianDigits(record.goalsAgainst)}",
                                            color = BroadcastCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (record.played > 0) {
                item(key = "team_form_chart") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TeamFormVicoChart(
                            matches = profile.matches,
                            teamName = profile.teamName
                        )
                    }
                }
            }

            if (profile.broadcastGames.isNotEmpty()) {
                item(key = "team_broadcasts") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileSectionTitle("پخش زندهٔ مرتبط")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Column {
                                profile.broadcastGames.forEachIndexed { idx, game ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onBroadcastClick(game) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LiveTv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = game.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (game.time.isNotBlank()) {
                                                Text(
                                                    text = game.time,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (idx < profile.broadcastGames.lastIndex) {
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

            if (profile.matches.isNotEmpty()) {
                item(key = "team_fixtures") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileSectionTitle("مسابقات این تیم")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column {
                                profile.matches.forEachIndexed { idx, entry ->
                                    val m = entry.match
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onMatchClick(m, entry.leagueTitle, entry.sport) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (m.isLive) {
                                            LivePulseIndicator(size = 6.dp)
                                        }
                                        Text(
                                            text = when {
                                                m.isLive -> m.liveTime.ifBlank { "زنده" }
                                                m.goals != null -> "پایان"
                                                else -> m.time.ifBlank { "–" }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (m.isLive) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(44.dp),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = m.host.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        AnimatedScoreboard(
                                            goals = m.goals,
                                            isLive = m.isLive,
                                            size = ScoreboardSize.COMPACT
                                        )
                                        Text(
                                            text = m.guest.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (idx < profile.matches.lastIndex) {
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

            if (profile.squadPlayers.isNotEmpty()) {
                item(key = "team_squad") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileSectionTitle("بازیکنان و امتیاز عملکرد")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column {
                                profile.squadPlayers.forEachIndexed { idx, player ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onPlayerClick(player, profile.teamName, profile.logoUrl)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (player.shirtNumber.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = SportsRepository.toPersianDigits(player.shirtNumber),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (player.score.isNotBlank()) {
                                                Text(
                                                    text = SportsRepository.toPersianDigits(player.score),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = BroadcastCyan
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronLeft,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (idx < profile.squadPlayers.lastIndex) {
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

            if (profile.relatedNews.isNotEmpty()) {
                item(key = "team_news") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileSectionTitle("اخبار مرتبط")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column {
                                profile.relatedNews.forEachIndexed { idx, article ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onArticleClick(article) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = article.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (article.publishedAtFa.isNotBlank()) {
                                            Text(
                                                text = article.publishedAtFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (idx < profile.relatedNews.lastIndex) {
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

            if (profile.matches.isEmpty() && profile.squadPlayers.isEmpty() && profile.relatedNews.isEmpty()) {
                item(key = "team_empty") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EmptyCrestLottie(size = 64.dp)
                            Text(
                                text = "اطلاعات مسابقه‌ای بیشتری برای این تیم در لیست امروز ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TeamAggregateRecord(
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int
)

@Composable
private fun ProfileMetaChip(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(0.7.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TeamKpiCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RollingScoreText(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
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
}
