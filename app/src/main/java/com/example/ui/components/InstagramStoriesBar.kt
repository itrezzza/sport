package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.InstagramMediaType
import com.example.data.model.InstagramSession
import com.example.data.model.InstagramStoryTrayGroup
import com.example.data.network.InstagramRepository
import com.example.data.network.SportsRepository
import com.example.ui.theme.LiveCrimson
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val InstagramRingGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFEDA75),
        Color(0xFFFA7E1E),
        Color(0xFFD62976),
        Color(0xFF962FBF),
        Color(0xFF4F5BD5)
    )
)

@Composable
fun InstagramStoriesBar(
    storyGroups: List<InstagramStoryTrayGroup>,
    seenStoryItemIds: Set<String>,
    needsLogin: Boolean,
    connectedUsername: String,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenGroup: (Int) -> Unit,
    onRefresh: () -> Unit,
    onManageAccounts: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("instagram_stories_bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                InstagramBadgeGlyph(modifier = Modifier.size(22.dp))
                Text(
                    text = "استوری‌های اینستاگرام",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!needsLogin && connectedUsername.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "@$connectedUsername",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else if (storyGroups.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = SportsRepository.toPersianDigits(storyGroups.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!needsLogin && connectedUsername.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(99.dp))
                            .bouncyClickable(pressedScale = 0.94f, onClick = onLoginClick)
                            .testTag("instagram_header_login_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Login,
                                contentDescription = "اتصال حساب اینستاگرام",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "حساب",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(99.dp))
                        .bouncyClickable(pressedScale = 0.94f, onClick = onManageAccounts)
                        .testTag("instagram_manage_accounts_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "مدیریت حساب و پیج‌های اینستاگرام",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "پیج‌ها",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!needsLogin) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("instagram_stories_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "به‌روزرسانی استوری‌های اینستاگرام",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        when {
            needsLogin -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.2.dp, InstagramRingGradient, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            InstagramBadgeGlyph(modifier = Modifier.size(24.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "اتصال به حساب اینستاگرام",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = errorMessage ?: "برای دیدن استوری‌های واقعی ۲۴ ساعته، وارد حساب اینستاگرامت شو.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onLoginClick,
                            shape = RoundedCornerShape(99.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("instagram_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ورود",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            isLoading && storyGroups.isEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.width(74.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, InstagramRingGradient, CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                StoryShimmerOverlay(modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }

            errorMessage != null && storyGroups.isEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("تلاش مجدد", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            storyGroups.isEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "استوری فعالی یافت نشد؛ می‌توانی پیج‌های ورزشی بیشتری اضافه کنی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onManageAccounts,
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("افزودن پیج", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("instagram_stories_row")
                ) {
                    itemsIndexed(
                        items = storyGroups,
                        key = { _, item -> item.id }
                    ) { index, group ->
                        val allSeen = group.items.isNotEmpty() &&
                            group.items.all { it.id in seenStoryItemIds }
                        InstagramStoryRingAvatar(
                            group = group,
                            isSeen = allSeen,
                            onClick = { onOpenGroup(index) }
                        )
                    }

                    item(key = "ig_add_page_ring") {
                        AddInstagramPageRing(onClick = onManageAccounts)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddInstagramPageRing(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("ig_story_add_account"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "افزودن پیج اینستاگرام",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = "افزودن پیج",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun InstagramStoryRingAvatar(
    group: InstagramStoryTrayGroup,
    isSeen: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val avatarRequest = remember(group.avatarUrl) {
        if (group.avatarUrl.isBlank()) null
        else ImageRequest.Builder(context)
            .data(group.avatarUrl)
            .crossfade(250)
            .memoryCacheKey(group.avatarUrl)
            .diskCacheKey(group.avatarUrl)
            .build()
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .bouncyClickable(pressedScale = 0.92f, onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("ig_story_ring_${group.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSeen) {
                            Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                        } else {
                            Modifier.border(2.6.dp, InstagramRingGradient, CircleShape)
                        }
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (avatarRequest != null) {
                    SubcomposeAsyncImage(
                        model = avatarRequest,
                        contentDescription = group.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { StoryShimmerOverlay(modifier = Modifier.fillMaxSize()) },
                        error = { StoryFallbackMonogram(username = group.username) }
                    )
                } else {
                    StoryFallbackMonogram(username = group.username)
                }
            }

            if (group.badgeLabel.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (!isSeen && group.items.any { it.mediaType == InstagramMediaType.VIDEO }) {
                        LiveCrimson
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.background),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = group.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!isSeen && group.items.any { it.mediaType == InstagramMediaType.VIDEO }) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Text(
            text = "@${group.username}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSeen) FontWeight.Normal else FontWeight.SemiBold,
            color = if (isSeen) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramAccountsSheet(
    accounts: List<String>,
    isAuthenticated: Boolean,
    connectedUsername: String,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAddAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val suggestions = listOf(
        "varzesh3",
        "perspolis",
        "esteghlal.fc.official",
        "footbalbartar",
        "433",
        "realmadrid",
        "fcbarcelona"
    ).filter { it !in accounts }

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
            Text(
                text = "حساب و پیج‌های اینستاگرام",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isAuthenticated) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isAuthenticated) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAuthenticated) {
                                if (connectedUsername.isNotBlank()) "متصل به @$connectedUsername" else "حساب اینستاگرام متصل است"
                            } else {
                                "حساب اینستاگرام متصل نیست"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAuthenticated) {
                                "استوری‌های فالوئینگ‌ها و پیج‌های زیر با نشست حسابت دریافت می‌شوند."
                            } else {
                                "برای دیدن استوری‌های ۲۴ ساعته ابتدا با حسابت وارد شو."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAuthenticated) {
                        TextButton(
                            onClick = onLogoutClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خروج", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onLoginClick()
                            },
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("ورود", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Text(
                text = "پیج‌های ورزشی دلخواه (علاوه بر فالوئینگ‌هایت):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    placeholder = { Text("مثلاً: varzesh3 یا @perspolis", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("instagram_username_input")
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onAddAccount(inputText)
                            inputText = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("instagram_add_username_button")
                ) {
                    Text("افزودن", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "پیج‌های پیشنهادی:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(99.dp))
                                    .bouncyClickable { onAddAccount(suggestion) }
                            ) {
                                Text(
                                    text = "+ @$suggestion",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (accounts.isNotEmpty()) {
                Text(
                    text = "پیج‌های ورزشی سنجاق‌شده (${SportsRepository.toPersianDigits(accounts.size)})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                accounts.forEach { account ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@$account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onRemoveAccount(account) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف @$account",
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginModal(
    onSessionCaptured: (InstagramSession) -> Unit,
    onManualSessionSubmit: (String) -> Boolean = { false },
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var pageProgress by remember { mutableIntStateOf(0) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var sessionCapturedOnce by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showManualTokenBox by remember { mutableStateOf(false) }
    var manualSessionInput by remember { mutableStateOf("") }
    var statusTip by remember { mutableStateOf("با نام کاربری و رمز عبور خودت وارد شو؛ فقط کوکی نشست روی دستگاه ذخیره می‌شود.") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun checkAndExtractSession(checkOnlyFromButton: Boolean = false) {
        if (sessionCapturedOnce) return
        val cm = CookieManager.getInstance()
        val cookiesA = cm.getCookie("https://www.instagram.com").orEmpty()
        val cookiesB = cm.getCookie("https://i.instagram.com").orEmpty()
        val combined = listOf(cookiesA, cookiesB).filter { it.isNotBlank() }.joinToString("; ")
        val ua = webViewRef?.settings?.userAgentString ?: InstagramRepository.DEFAULT_MOBILE_USER_AGENT
        val parsed = InstagramRepository.parseInstagramCookieString(combined, ua)
        if (parsed != null) {
            sessionCapturedOnce = true
            runCatching { cm.flush() }
            onSessionCaptured(parsed)
        } else if (checkOnlyFromButton) {
            statusTip = "هنوز ورود کامل نشده است؛ پس از ورود به صفحهٔ اصلی اینستاگرام دوباره دکمهٔ «بررسی اتصال» را بزن."
        }
    }

    fun reloadLoginPage() {
        loadError = null
        isLoadingPage = true
        pageProgress = 10
        statusTip = "در حال بارگذاری صفحهٔ ورود اینستاگرام…"
        webViewRef?.loadUrl("https://www.instagram.com/accounts/login/")
    }

    LaunchedEffect(Unit) {
        while (!sessionCapturedOnce) {
            delay(1200L)
            checkAndExtractSession(checkOnlyFromButton = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
            }
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("instagram_login_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_close)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ورود به اینستاگرام",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "اتصال امن با صفحهٔ رسمی اینستاگرام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = { checkAndExtractSession(checkOnlyFromButton = true) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("instagram_login_verify_button")
                            ) {
                                Text(
                                    text = "بررسی اتصال",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (loadError == null && isLoadingPage && pageProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { pageProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = loadError ?: statusTip,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadsImagesAutomatically = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportMultipleWindows(false)
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.userAgentString = InstagramRepository.DEFAULT_MOBILE_USER_AGENT

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageProgress = newProgress
                                        isLoadingPage = newProgress < 100
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        if (url != null && url != "about:blank" && !url.startsWith("chrome-error://")) {
                                            isLoadingPage = true
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoadingPage = false
                                        if (url != null && url.startsWith("chrome-error://")) {
                                            loadError = "صفحهٔ ورود اینستاگرام در دسترس نیست؛ اتصال شبکه یا فیلترشکن را بررسی کن."
                                            view?.loadUrl("about:blank")
                                            return
                                        }
                                        checkAndExtractSession(checkOnlyFromButton = false)
                                        if (!sessionCapturedOnce && url != null && url != "about:blank") {
                                            view?.evaluateJavascript(
                                                "(function(){try{return document.body ? document.body.innerText.slice(0,400) : '';}catch(e){return '';}})();"
                                            ) { rawText ->
                                                val text = rawText.orEmpty()
                                                val isSoftError = text.contains("Sorry, something went wrong", ignoreCase = true) ||
                                                    text.contains("page isn't available", ignoreCase = true) ||
                                                    text.contains("Please wait a few minutes", ignoreCase = true) ||
                                                    text.contains("ERR_", ignoreCase = false)
                                                if (isSoftError) {
                                                    loadError = "اینستاگرام صفحهٔ ورود وب را موقتاً محدود کرده است."
                                                    view.loadUrl("about:blank")
                                                }
                                            }
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoadingPage = false
                                            loadError = "دسترسی به صفحهٔ ورود اینستاگرام برقرار نشد؛ اتصال اینترنت یا فیلترشکن را بررسی کن."
                                            view?.stopLoading()
                                            view?.loadUrl("about:blank")
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?
                                    ) {
                                        val code = errorResponse?.statusCode ?: 0
                                        if (request?.isForMainFrame == true && code >= 400) {
                                            isLoadingPage = false
                                            loadError = "صفحهٔ ورود اینستاگرام خطای شبکه داد (کد $code)."
                                            view?.stopLoading()
                                            view?.loadUrl("about:blank")
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        handler?.cancel()
                                        isLoadingPage = false
                                        loadError = "گواهی امنیتی اتصال به اینستاگرام تأیید نشد."
                                        view?.loadUrl("about:blank")
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url?.toString().orEmpty()
                                        if (!(target.startsWith("https://") || target.startsWith("http://"))) {
                                            return true
                                        }
                                        checkAndExtractSession(checkOnlyFromButton = false)
                                        return false
                                    }
                                }
                                webViewRef = this
                                loadUrl("https://www.instagram.com/accounts/login/")
                            }
                        }
                    )

                    if (loadError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = "صفحهٔ ورود اینستاگرام باز نشد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = loadError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { reloadLoginPage() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("instagram_login_retry_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تلاش مجدد", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://www.instagram.com/accounts/login/".toUri()
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مرورگر گوشی", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "اتصال مستقیم با Session ID",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "اگر صفحهٔ وب اینستاگرام در شبکهٔ شما باز نمی‌شود، مقدار کوکی sessionid را اینجا وارد کن:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = manualSessionInput,
                                        onValueChange = { manualSessionInput = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            autoCorrectEnabled = false,
                                            keyboardType = KeyboardType.Ascii
                                        ),
                                        placeholder = { Text("sessionid یا رشتهٔ کوکی", style = MaterialTheme.typography.bodySmall) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("manual_session_id_input")
                                    )
                                    Button(
                                        onClick = {
                                            if (onManualSessionSubmit(manualSessionInput)) {
                                                sessionCapturedOnce = true
                                            } else {
                                                statusTip = "مقدار Session ID معتبر نیست؛ لطفاً کوکی کامل یا sessionid معتبر وارد کن."
                                            }
                                        },
                                        enabled = manualSessionInput.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("manual_session_id_submit")
                                    ) {
                                        Text("اتصال با Session ID", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                if (loadError == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showManualTokenBox = !showManualTokenBox },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = if (showManualTokenBox) "بستن ورود با Session ID" else "ورود جایگزین با Session ID",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                TextButton(
                                    onClick = { reloadLoginPage() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = "بارگذاری مجدد",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (showManualTokenBox) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = manualSessionInput,
                                        onValueChange = { manualSessionInput = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            autoCorrectEnabled = false,
                                            keyboardType = KeyboardType.Ascii
                                        ),
                                        placeholder = { Text("sessionid یا Cookie: ...", style = MaterialTheme.typography.bodySmall) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (onManualSessionSubmit(manualSessionInput)) {
                                                sessionCapturedOnce = true
                                            } else {
                                                statusTip = "مقدار Session ID معتبر نیست؛ لطفاً کوکی کامل یا sessionid معتبر وارد کن."
                                            }
                                        },
                                        enabled = manualSessionInput.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Text("تأیید", style = MaterialTheme.typography.labelLarge)
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
private fun StoryFallbackMonogram(username: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InstagramRingGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.trim().firstOrNull()?.uppercase() ?: "IG",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun InstagramBadgeGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val corner = size.minDimension * 0.28f
        drawRoundRect(
            brush = InstagramRingGradient,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = size.minDimension * 0.11f)
        )
        drawCircle(
            brush = InstagramRingGradient,
            radius = size.minDimension * 0.22f,
            style = Stroke(width = size.minDimension * 0.1f)
        )
        drawCircle(
            color = Color(0xFFD62976),
            radius = size.minDimension * 0.06f,
            center = Offset(size.width * 0.75f, size.height * 0.25f)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun InstagramStoryViewerModal(
    groups: List<InstagramStoryTrayGroup>,
    activeGroupIndex: Int,
    activeSlideIndex: Int,
    onNextSlide: () -> Unit,
    onPrevSlide: () -> Unit,
    onClose: () -> Unit
) {
    val group = groups.getOrNull(activeGroupIndex) ?: return
    val slide = group.items.getOrNull(activeSlideIndex) ?: return
    val context = LocalContext.current

    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var slideProgress by remember(activeGroupIndex, activeSlideIndex) { mutableFloatStateOf(0f) }
    var videoError by remember(activeGroupIndex, activeSlideIndex) { mutableStateOf<String?>(null) }

    val isVideoEntry = slide.mediaType == InstagramMediaType.VIDEO
    val hasDirectVideoStream = isVideoEntry &&
        slide.mediaUrl.isNotBlank() &&
        (slide.mediaUrl.contains(".mp4", ignoreCase = true) || slide.mediaUrl != slide.thumbnailUrl)

    BackHandler { onClose() }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onNextSlide()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                videoError = "پخش مستقیم ویدئو در این شبکه ممکن نشد؛ از دکمهٔ پایین در اینستاگرام ببین."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(slide.id, hasDirectVideoStream) {
        videoError = null
        slideProgress = 0f
        if (hasDirectVideoStream) {
            exoPlayer.setMediaItem(MediaItem.fromUri(slide.mediaUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = !isPaused
            exoPlayer.volume = if (isMuted) 0f else 1f
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(isPaused, isMuted, hasDirectVideoStream) {
        if (hasDirectVideoStream) {
            exoPlayer.playWhenReady = !isPaused
            exoPlayer.volume = if (isMuted) 0f else 1f
        }
    }

    // Progress timer for images / embed video thumbnails and duration sync for direct MP4 videos
    LaunchedEffect(activeGroupIndex, activeSlideIndex, isPaused, hasDirectVideoStream) {
        if (!hasDirectVideoStream) {
            val totalMs = 6000L
            val stepMs = 40L
            while (isActive && slideProgress < 1f) {
                delay(stepMs)
                if (!isPaused) {
                    slideProgress = (slideProgress + stepMs.toFloat() / totalMs).coerceAtMost(1f)
                }
            }
            if (slideProgress >= 1f) {
                onNextSlide()
            }
        } else {
            while (isActive) {
                val dur = exoPlayer.duration
                if (dur > 0L) {
                    slideProgress = (exoPlayer.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
                delay(100L)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("instagram_story_viewer"),
        color = Color(0xFF06070A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeGroupIndex, activeSlideIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width * 0.35f) {
                                onNextSlide()
                            } else if (offset.x > size.width * 0.65f) {
                                onPrevSlide()
                            } else {
                                onNextSlide()
                            }
                        }
                    )
                }
        ) {
            // Media Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                if (hasDirectVideoStream && videoError == null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                keepScreenOn = true
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player !== exoPlayer) {
                                view.player = exoPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val imageToRender = slide.thumbnailUrl.ifBlank { slide.mediaUrl }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageToRender)
                                .crossfade(200)
                                .build(),
                            contentDescription = slide.caption.ifBlank { group.displayName },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = videoError ?: "تصویر استوری بارگذاری نشد.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        )

                        if (isVideoEntry) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.56f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayCircleFilled,
                                    contentDescription = "ریلز ویدئویی",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top Segmented Progress + Author Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        group.items.forEachIndexed { idx, _ ->
                            val fill = when {
                                idx < activeSlideIndex -> 1f
                                idx == activeSlideIndex -> slideProgress
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.White.copy(alpha = 0.32f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fill)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color.White)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, InstagramRingGradient, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1D24)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (group.avatarUrl.isNotBlank()) {
                                SubcomposeAsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = group.username,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                StoryFallbackMonogram(username = group.username)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "@${group.username}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group.displayName} · ${slide.timestampFa.ifBlank { "تازه" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasDirectVideoStream) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                    contentDescription = "صدا",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (isPaused) "ادامه" else "توقف",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("close_story_viewer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Caption + Open in Instagram CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (slide.caption.isNotBlank()) {
                    Text(
                        text = slide.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (slide.permalink.startsWith("https://") || slide.permalink.startsWith("http://")) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, slide.permalink.toUri()))
                            }
                        },
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_instagram_permalink_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاهده در اینستاگرام",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryShimmerOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "storyShimmer")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storyShimmerOffset"
    )
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset(progress * 200f - 80f, 0f),
                end = Offset(progress * 200f + 80f, 160f)
            )
        )
    )
}
