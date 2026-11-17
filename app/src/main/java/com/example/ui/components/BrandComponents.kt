package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Scoreboard
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Scoreboard
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.AppTab
import com.example.data.model.BroadcastSource
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NotificationSettings
import com.example.data.model.WatchedMatch
import com.example.data.network.SportsRepository
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.VazirmatnFontFamily

@Composable
fun LivePulseDot(
    color: Color = LiveCrimson,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    LivePulseIndicator(
        color = color,
        size = size,
        modifier = modifier
    )
}

@Composable
fun KhateBaziTopBar(
    liveCount: Int,
    watchedCount: Int,
    isDarkTheme: Boolean,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val spinAngle: Float = if (isRefreshing && !reducedMotion) {
        val refreshTransition = rememberInfiniteTransition(label = "refreshSpin")
        val angle by refreshTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "spinAngle"
        )
        angle
    } else {
        0f
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
                        cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
                    )
                    val cx = w * 0.52f
                    val cy = h * 0.50f
                    val playPath = Path().apply {
                        moveTo(cx - w * 0.14f, cy - h * 0.20f)
                        lineTo(cx + w * 0.20f, cy)
                        lineTo(cx - w * 0.14f, cy + h * 0.20f)
                        close()
                    }
                    drawPath(playPath, color = Color(0xFF091203))
                }

                Text(
                    text = "خط بازی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (liveCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LiveCrimson.copy(alpha = 0.14f),
                        border = BorderStroke(0.6.dp, LiveCrimson.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LivePulseIndicator(color = LiveCrimson, size = 5.dp)
                            RollingScoreText(
                                text = "${SportsRepository.toPersianDigits(liveCount)} زنده",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LiveCrimson
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (watchedCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = SportsRepository.toPersianDigits(watchedCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (watchedCount > 0) Icons.Rounded.NotificationsActive
                            else Icons.Rounded.Notifications,
                            contentDescription = stringResource(R.string.cd_notification_settings),
                            tint = if (watchedCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRefreshClick()
                    },
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        tint = if (isRefreshing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = spinAngle
                            }
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_theme_button")
                ) {
                    AnimatedContent(
                        targetState = isDarkTheme,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)))
                                .togetherWith(scaleOut(tween(150)) + fadeOut(tween(150)))
                        },
                        label = "themeIcon"
                    ) { dark ->
                        Icon(
                            imageVector = if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = stringResource(R.string.cd_toggle_theme),
                            tint = if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun KhateBaziBottomBar(
    currentTab: AppTab,
    liveCount: Int = 0,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    SportsBottomNavigation(
        currentTab = currentTab,
        liveCount = liveCount,
        onSelectTab = onSelectTab,
        modifier = modifier
    )
}

@Composable
fun SportsBottomNavigation(
    currentTab: AppTab,
    liveCount: Int = 0,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("persistent_bottom_nav_bar"),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    val activeAccent = MaterialTheme.colorScheme.primary
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) {
                            activeAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        },
                        animationSpec = tween(if (reducedMotion) 0 else 180),
                        label = "navIconTint"
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 22.dp else 0.dp,
                        animationSpec = if (reducedMotion) tween(0) else spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navIndicatorWidth"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = if (reducedMotion) tween(0) else spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navIconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .minimumInteractiveComponentSize()
                            .bouncyClickable(pressedScale = 0.95f) {
                                onSelectTab(tab)
                            }
                            .testTag("nav_tab_${tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle top broadcast hairline indicator instead of oversized pill
                        if (indicatorWidth > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(indicatorWidth)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(activeAccent)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            TabIconWithLiveBadge(
                                tab = tab,
                                selected = selected,
                                liveCount = liveCount,
                                tint = iconTint,
                                iconModifier = Modifier
                                    .size(21.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Text(
                                text = tab.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = iconTint,
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

@Composable
fun TabIconWithLiveBadge(
    tab: AppTab,
    selected: Boolean,
    liveCount: Int,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconModifier: Modifier = Modifier.size(21.dp)
) {
    if (tab == AppTab.SCORES && liveCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = LiveCrimson,
                    contentColor = Color.White,
                    modifier = Modifier
                        .offset(x = 7.dp, y = (-4).dp)
                        .testTag("nav_scores_live_badge")
                ) {
                    Text(
                        text = SportsRepository.toPersianDigits(liveCount.coerceAtMost(99)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            Icon(
                imageVector = tabNavIcon(tab, selected),
                contentDescription = tab.labelFa,
                tint = tint,
                modifier = iconModifier
            )
        }
    } else {
        Icon(
            imageVector = tabNavIcon(tab, selected),
            contentDescription = tab.labelFa,
            tint = tint,
            modifier = iconModifier
        )
    }
}

fun tabNavIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.BROADCAST -> if (selected) Icons.Rounded.LiveTv else Icons.Outlined.LiveTv
    AppTab.SCORES -> if (selected) Icons.Rounded.Scoreboard else Icons.Outlined.Scoreboard
    AppTab.NEWS -> if (selected) Icons.Rounded.Newspaper else Icons.Outlined.Newspaper
    AppTab.FAVORITES -> if (selected) Icons.Rounded.Star else Icons.Outlined.StarOutline
}

@Composable
fun TeamBadge(
    logoUrl: String,
    teamName: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    TeamLogo(
        logoUrl = logoUrl,
        teamName = teamName,
        size = size,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSourceChooserSheet(
    game: MergedBroadcastGame,
    onSelectSource: (BroadcastSource) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "انتخاب سرویس پخش",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "از کدام سرویس تماشا می‌کنی؟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            listOf(BroadcastSource.ANTEN, BroadcastSource.APARAT).forEach { source ->
                val available = game.sources.containsKey(source)
                val accentColor = if (source == BroadcastSource.ANTEN) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .bouncyClickable(enabled = available) { onSelectSource(source) }
                        .testTag("watch_source_${source.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (available) {
                        accentColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (available) accentColor.copy(alpha = 0.50f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (available) accentColor
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = source.badgeText,
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (available) Color(0xFF091203) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = source.titleFa,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (available) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = if (available) "آمادهٔ پخش با کیفیت مستقیم" else "در این سرویس در دسترس نیست",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (available) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
    notificationSettings: NotificationSettings,
    watchedMatches: List<WatchedMatch>,
    onUpdateSettings: (NotificationSettings) -> Unit,
    onUnwatchMatch: (String) -> Unit,
    onSendTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "هشدارهای هوشمند",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "اعلان‌ها و یادآور بازی‌ها",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Switch(
                    checked = notificationSettings.enabled,
                    onCheckedChange = { checked ->
                        onUpdateSettings(notificationSettings.copy(enabled = checked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("notifications_master_switch")
                )
            }

            Text(
                text = "زنگولهٔ هر بازی را در نتایج یا جزئیات بزن تا یادآور شروع و گل‌های زنده برایت ارسال شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (notificationSettings.enabled) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("یادآور شروع بازی", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "اعلان پیش از سوت آغاز مسابقه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationSettings.notifyOnKickoff,
                        onCheckedChange = {
                            onUpdateSettings(notificationSettings.copy(notifyOnKickoff = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("notifications_kickoff_switch")
                    )
                }

                if (notificationSettings.notifyOnKickoff) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "زمان ارسال یادآور:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                        onUpdateSettings(
                                            notificationSettings.copy(kickoffLeadMinutes = minutes)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("lead_minutes_$minutes")
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تغییر نتیجه و گل‌های زنده", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "اعلان فوری هنگام ثبت گل یا پایان بازی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationSettings.notifyOnScoreChange,
                        onCheckedChange = {
                            onUpdateSettings(notificationSettings.copy(notifyOnScoreChange = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("notifications_score_switch")
                    )
                }

                OutlinedButton(
                    onClick = onSendTestNotification,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_notification_button")
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

            if (watchedMatches.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "مسابقات دنبال‌شده (${SportsRepository.toPersianDigits(watchedMatches.size)})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                watchedMatches.forEach { watched ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${watched.hostName} ـ ${watched.guestName}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = watched.leagueTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onUnwatchMatch(watched.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("unwatch_match_${watched.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = stringResource(R.string.cd_unwatch_match),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
