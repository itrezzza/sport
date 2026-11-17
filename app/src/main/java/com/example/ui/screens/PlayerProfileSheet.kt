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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PlayerProfileData
import com.example.data.network.SportsRepository
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.RollingScoreText
import com.example.ui.components.TeamBadge
import com.example.ui.theme.AmberCard
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GoldMotm

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileSheet(
    profile: PlayerProfileData,
    onTeamClick: (String, String) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .testTag("player_profile_sheet"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "player_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
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
                                        .testTag("close_player_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_close)
                                    )
                                }
                            }

                            Text(
                                text = "پروفایل بازیکن",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Box(modifier = Modifier.size(48.dp))
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
                                Surface(
                                    shape = CircleShape,
                                    color = if (profile.player.isManOfTheMatch) GoldMotm
                                    else MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = profile.player.shirtNumber.ifBlank { "★" }
                                                .let { SportsRepository.toPersianDigits(it) },
                                            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Black,
                                            color = if (profile.player.isManOfTheMatch) Color(0xFF091203)
                                            else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = profile.player.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )

                                if (profile.teamName.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                onTeamClick(profile.teamName, profile.teamLogoUrl)
                                            }
                                            .testTag("player_profile_team_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            TeamBadge(
                                                logoUrl = profile.teamLogoUrl,
                                                teamName = profile.teamName,
                                                size = 20.dp
                                            )
                                            Text(
                                                text = profile.teamName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (profile.player.isCaptain) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MilitaryTech,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "کاپیتان تیم",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    if (profile.player.isManOfTheMatch) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldMotm.copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = GoldMotm,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "بهترین بازیکن زمین",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldMotm
                                                )
                                            }
                                        }
                                    }
                                }

                                if (profile.matchLabel.isNotBlank()) {
                                    Text(
                                        text = if (profile.leagueTitle.isNotBlank()) "${profile.leagueTitle} · ${profile.matchLabel}" else profile.matchLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "player_kpis") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (profile.player.score.isNotBlank()) {
                            PlayerKpiBox(
                                label = "امتیاز بازی",
                                value = SportsRepository.toPersianDigits(profile.player.score),
                                accent = ElectricLime
                            )
                        }
                        PlayerKpiBox(
                            label = "گل",
                            value = SportsRepository.toPersianDigits(profile.goalsCount),
                            accent = MaterialTheme.colorScheme.primary
                        )
                        PlayerKpiBox(
                            label = "پاس گل",
                            value = SportsRepository.toPersianDigits(profile.assistsCount),
                            accent = BroadcastCyan
                        )
                        PlayerKpiBox(
                            label = "کارت",
                            value = SportsRepository.toPersianDigits(profile.cardsCount),
                            accent = AmberCard
                        )
                    }
                }
            }

            item(key = "player_contributions") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "رخدادهای ثبت‌شده در مسابقه",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (profile.contributions.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EmptyCrestLottie(size = 56.dp)
                                Text(
                                    text = "رخداد مستقیمی برای این بازیکن در تایم‌لاین مسابقه ثبت نشده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column {
                                profile.contributions.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = item.minute,
                                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (item.detail.isNotBlank()) {
                                                Text(
                                                    text = item.detail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (idx < profile.contributions.lastIndex) {
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
        }
    }
}

@Composable
private fun PlayerKpiBox(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RollingScoreText(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.Black,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
