package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.HlsTrackOption
import com.example.data.model.StreamQuality
import com.example.data.network.SportsRepository
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun KeepActivityScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val window = context.findActivity()?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@OptIn(UnstableApi::class)
enum class PlayerResizeMode(val resizeMode: Int, val labelFa: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "تناسب"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "بزرگ‌نمایی"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "تمام‌عرض")
}

@Stable
@OptIn(UnstableApi::class)
class BroadcastPlayerController(
    val exoPlayer: ExoPlayer
) {
    var activeStreamUrl by mutableStateOf<String?>(null)
    var hlsTracks by mutableStateOf<List<HlsTrackOption>>(emptyList())
    var selectedTrackId by mutableStateOf("auto")
    var isMuted by mutableStateOf(false)
    var currentVolume by mutableFloatStateOf(1f)
    var currentTracksSnapshot by mutableStateOf<Tracks?>(null)

    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var playbackError by mutableStateOf<String?>(null)
    var isLiveStream by mutableStateOf(true)
    var isSeekable by mutableStateOf(false)
    var isAtLiveEdge by mutableStateOf(true)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)

    var controlsVisible by mutableStateOf(true)
    var showFullscreenQualityMenu by mutableStateOf(false)
    var resizeMode by mutableStateOf(PlayerResizeMode.FIT)
    var seekFeedbackLabel by mutableStateOf<String?>(null)
    var hudInteractionTick by mutableIntStateOf(0)

    fun prepareAndPlayStream(url: String) {
        activeStreamUrl = url
        playbackError = null
        isBuffering = true
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
            )
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        val isHls = url.contains(".m3u8", ignoreCase = true)
        isLiveStream = isHls
        if (isHls) {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
            exoPlayer.setMediaSource(hlsSource)
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun clearStream() {
        activeStreamUrl = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        isPlaying = false
        isBuffering = false
        playbackError = null
    }

    fun retry() {
        activeStreamUrl?.let { prepareAndPlayStream(it) }
    }

    fun applyHlsTrackSelection(option: HlsTrackOption) {
        selectedTrackId = option.id
        if (option.id == "auto") {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
        } else {
            val tracks = currentTracksSnapshot ?: return
            val group = tracks.groups.getOrNull(option.groupIndex) ?: return
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
                )
                .build()
        }
    }

    fun togglePlayPause() {
        hudInteractionTick += 1
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            val url = activeStreamUrl
            if (exoPlayer.playbackState == Player.STATE_IDLE && !url.isNullOrBlank()) {
                prepareAndPlayStream(url)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun toggleMute() {
        hudInteractionTick += 1
        isMuted = !isMuted
        if (isMuted) {
            currentVolume = exoPlayer.volume.takeIf { it > 0f } ?: 1f
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = currentVolume
        }
    }

    fun cycleResizeMode() {
        hudInteractionTick += 1
        val entries = PlayerResizeMode.entries
        resizeMode = entries[(resizeMode.ordinal + 1) % entries.size]
    }

    fun seekRelative(deltaMs: Long) {
        hudInteractionTick += 1
        val target = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
        val clamped = if (durationMs > 0L) target.coerceAtMost(durationMs) else target
        exoPlayer.seekTo(clamped)
        seekFeedbackLabel = if (deltaMs > 0) "+۱۰ ثانیه" else "-۱۰ ثانیه"
    }

    fun jumpToLiveEdge() {
        hudInteractionTick += 1
        exoPlayer.seekToDefaultPosition()
        exoPlayer.play()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberBroadcastPlayerController(
    streamUrl: String?,
    onPlayerError: (String) -> Unit
): BroadcastPlayerController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestErrorCallback by rememberUpdatedState(onPlayerError)

    val controller = remember(context) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1_500, 3_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                playWhenReady = true
            }
        BroadcastPlayerController(player)
    }

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                controller.exoPlayer.playWhenReady = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                controller.isPlaying = playing
                if (playing) {
                    controller.playbackError = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                controller.isBuffering = playbackState == Player.STATE_BUFFERING
                controller.isLiveStream = controller.exoPlayer.isCurrentMediaItemLive ||
                    controller.activeStreamUrl.orEmpty().contains(".m3u8", ignoreCase = true)
                controller.isSeekable = controller.exoPlayer.isCurrentMediaItemSeekable &&
                    !controller.exoPlayer.isCurrentMediaItemLive
                if (playbackState == Player.STATE_READY) {
                    controller.playbackError = null
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                controller.currentTracksSnapshot = tracks
                val options = mutableListOf(HlsTrackOption(id = "auto", label = "کیفیت خودکار"))
                val seenHeights = mutableSetOf<Int>()
                tracks.groups.forEachIndexed { gIdx, group ->
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (tIdx in 0 until group.length) {
                            val format = group.getTrackFormat(tIdx)
                            val h = format.height
                            if (h > 0 && seenHeights.add(h)) {
                                options.add(
                                    HlsTrackOption(
                                        id = "${gIdx}_$tIdx",
                                        label = "${h}p",
                                        groupIndex = gIdx,
                                        trackIndex = tIdx
                                    )
                                )
                            } else if (h <= 0 && format.bitrate > 0) {
                                val kbps = format.bitrate / 1000
                                options.add(
                                    HlsTrackOption(
                                        id = "${gIdx}_$tIdx",
                                        label = "${kbps}k",
                                        groupIndex = gIdx,
                                        trackIndex = tIdx
                                    )
                                )
                            }
                        }
                    }
                }
                controller.hlsTracks = if (options.size > 2) options else emptyList()
            }

            override fun onPlayerError(error: PlaybackException) {
                controller.isBuffering = false
                controller.isPlaying = false
                val msg = error.localizedMessage ?: "خطای پخش رسانه"
                controller.playbackError = msg
                controller.controlsVisible = true
                latestErrorCallback(msg)
            }
        }
        controller.exoPlayer.addListener(listener)
        onDispose {
            controller.exoPlayer.removeListener(listener)
            controller.exoPlayer.release()
        }
    }

    LaunchedEffect(streamUrl) {
        controller.selectedTrackId = "auto"
        controller.hlsTracks = emptyList()
        controller.playbackError = null
        controller.controlsVisible = true
        controller.hudInteractionTick += 1
        if (streamUrl.isNullOrBlank()) {
            controller.clearStream()
        } else if (controller.activeStreamUrl != streamUrl) {
            controller.prepareAndPlayStream(streamUrl)
        }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNullOrBlank()) return@LaunchedEffect
        while (isActive) {
            controller.positionMs = controller.exoPlayer.currentPosition.coerceAtLeast(0L)
            controller.durationMs = controller.exoPlayer.duration
                .takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
            controller.isLiveStream = controller.exoPlayer.isCurrentMediaItemLive ||
                streamUrl.contains(".m3u8", ignoreCase = true)
            controller.isSeekable = controller.exoPlayer.isCurrentMediaItemSeekable &&
                !controller.exoPlayer.isCurrentMediaItemLive
            controller.isAtLiveEdge = if (controller.exoPlayer.isCurrentMediaItemLive) {
                controller.exoPlayer.currentLiveOffset <= 10_000L
            } else {
                true
            }
            delay(500L)
        }
    }

    LaunchedEffect(
        controller.controlsVisible,
        controller.isPlaying,
        controller.isBuffering,
        controller.playbackError,
        controller.hudInteractionTick,
        controller.showFullscreenQualityMenu
    ) {
        if (controller.controlsVisible &&
            controller.isPlaying &&
            !controller.isBuffering &&
            controller.playbackError == null &&
            !controller.showFullscreenQualityMenu
        ) {
            delay(4000L)
            controller.controlsVisible = false
        }
    }

    LaunchedEffect(controller.seekFeedbackLabel) {
        if (controller.seekFeedbackLabel != null) {
            delay(750L)
            controller.seekFeedbackLabel = null
        }
    }

    return controller
}

@OptIn(UnstableApi::class)
@Composable
fun LiveStreamingPlayer(
    activeProgramId: String?,
    programTitle: String,
    sourceLabel: String,
    playerStatus: String,
    streamUrl: String?,
    qualities: List<StreamQuality>,
    activeQualityIndex: Int,
    isFullscreen: Boolean,
    onSelectQuality: (Int) -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onClosePlayer: () -> Unit,
    onPlayerError: (String) -> Unit,
    modifier: Modifier = Modifier,
    controller: BroadcastPlayerController? = null,
    isChatOpen: Boolean = false,
    onToggleChat: (() -> Unit)? = null,
    chatOverlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    BroadcastPlayerStage(
        activeProgramId = activeProgramId,
        programTitle = programTitle,
        sourceLabel = sourceLabel,
        playerStatus = playerStatus,
        streamUrl = streamUrl,
        qualities = qualities,
        activeQualityIndex = activeQualityIndex,
        isFullscreen = isFullscreen,
        onSelectQuality = onSelectQuality,
        onToggleFullscreen = onToggleFullscreen,
        onClosePlayer = onClosePlayer,
        onPlayerError = onPlayerError,
        modifier = modifier,
        controller = controller,
        isChatOpen = isChatOpen,
        onToggleChat = onToggleChat,
        chatOverlayContent = chatOverlayContent
    )
}

@OptIn(UnstableApi::class)
@Composable
fun BroadcastPlayerStage(
    activeProgramId: String?,
    programTitle: String,
    sourceLabel: String,
    playerStatus: String,
    streamUrl: String?,
    qualities: List<StreamQuality>,
    activeQualityIndex: Int,
    isFullscreen: Boolean,
    onSelectQuality: (Int) -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onClosePlayer: () -> Unit,
    onPlayerError: (String) -> Unit,
    modifier: Modifier = Modifier,
    controller: BroadcastPlayerController? = null,
    isChatOpen: Boolean = false,
    onToggleChat: (() -> Unit)? = null,
    chatOverlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val playerController = controller ?: rememberBroadcastPlayerController(
        streamUrl = streamUrl,
        onPlayerError = onPlayerError
    )

    val isVisible = activeProgramId != null || !streamUrl.isNullOrBlank()
    val keepScreenAwake = isVisible &&
        (!streamUrl.isNullOrBlank() || playerController.isPlaying || playerController.isBuffering || isFullscreen)

    if (controller == null) {
        KeepActivityScreenOnEffect(enabled = keepScreenAwake)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("broadcast_player_stage"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiveBeaconLottie(size = 13.dp)
                            Text(
                                text = sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("player_keep_screen_on_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BrightnessHigh,
                                        contentDescription = "صفحه در حین پخش روشن می‌ماند",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "صفحه روشن",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = programTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onToggleChat != null) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (isChatOpen) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { onToggleChat() }
                                    .testTag("player_header_chat_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forum,
                                        contentDescription = "گفتگوی زنده",
                                        tint = if (isChatOpen) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "گفتگو",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChatOpen) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onClosePlayer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "بستن پخش‌کننده",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MidnightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !streamUrl.isNullOrBlank() && !isFullscreen -> {
                                ProVideoSurfaceWithHud(
                                    controller = playerController,
                                    keepScreenOn = keepScreenAwake,
                                    isFullscreen = false,
                                    programTitle = programTitle,
                                    onToggleFullscreen = { onToggleFullscreen(true) },
                                    isChatOpen = isChatOpen,
                                    onToggleChat = onToggleChat,
                                    chatOverlayContent = chatOverlayContent
                                )
                            }

                            isFullscreen -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Text(
                                        text = "در حال پخش در حالت تمام‌صفحه",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD5E0F5),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            else -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = playerStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD5E0F5),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = playerStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (qualities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        qualities.forEachIndexed { index, q ->
                            FilterChip(
                                selected = index == activeQualityIndex,
                                onClick = { onSelectQuality(index) },
                                label = { Text(q.label, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = if (index == activeQualityIndex) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("quality_chip_$index")
                            )
                        }
                    }
                }

                if (playerController.hlsTracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = "کیفیت تصویر",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        playerController.hlsTracks.forEach { option ->
                            FilterChip(
                                selected = option.id == playerController.selectedTrackId,
                                onClick = { playerController.applyHlsTrackSelection(option) },
                                label = {
                                    Text(
                                        text = option.label,
                                        fontFamily = VazirmatnFontFamily,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("hls_track_${option.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun FullscreenBroadcastPlayerOverlay(
    controller: BroadcastPlayerController,
    programTitle: String,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    isChatOpen: Boolean = false,
    onToggleChat: (() -> Unit)? = null,
    chatOverlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    BackHandler {
        controller.showFullscreenQualityMenu = false
        onExitFullscreen()
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previousOrientation
            controller.showFullscreenQualityMenu = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_player_container")
    ) {
        ProVideoSurfaceWithHud(
            controller = controller,
            keepScreenOn = true,
            isFullscreen = true,
            programTitle = programTitle,
            onToggleFullscreen = {
                controller.showFullscreenQualityMenu = false
                onExitFullscreen()
            },
            isChatOpen = isChatOpen,
            onToggleChat = onToggleChat,
            chatOverlayContent = chatOverlayContent
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ProVideoSurfaceWithHud(
    controller: BroadcastPlayerController,
    keepScreenOn: Boolean,
    isFullscreen: Boolean,
    programTitle: String,
    onToggleFullscreen: () -> Unit,
    isChatOpen: Boolean = false,
    onToggleChat: (() -> Unit)? = null,
    chatOverlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val isSeekable = controller.isSeekable
    val playbackError = controller.playbackError
    val controlsVisible = controller.controlsVisible
    val isBuffering = controller.isBuffering
    val isPlaying = controller.isPlaying

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isSeekable) {
                detectTapGestures(
                    onTap = {
                        if (isFullscreen && controller.showFullscreenQualityMenu) {
                            controller.showFullscreenQualityMenu = false
                        } else {
                            controller.controlsVisible = !controller.controlsVisible
                        }
                        controller.hudInteractionTick += 1
                    },
                    onDoubleTap = { offset ->
                        if (isSeekable) {
                            if (offset.x > size.width * 0.55f) {
                                controller.seekRelative(10_000L)
                            } else if (offset.x < size.width * 0.45f) {
                                controller.seekRelative(-10_000L)
                            } else {
                                controller.controlsVisible = !controller.controlsVisible
                            }
                        } else {
                            controller.controlsVisible = !controller.controlsVisible
                        }
                    }
                )
            }
    ) {
        ExoPlayerViewBox(
            player = controller.exoPlayer,
            useController = false,
            keepScreenOn = keepScreenOn,
            resizeMode = controller.resizeMode.resizeMode,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controller.seekFeedbackLabel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(
                    text = controller.seekFeedbackLabel.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = LiveCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = playbackError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = { controller.retry() },
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("player_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تلاش مجدد", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = (controlsVisible || isBuffering) && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (controlsVisible) 0.30f else 0.12f))
            ) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                                )
                            )
                            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (controller.isLiveStream) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (controller.isAtLiveEdge) LiveCrimson else MidnightBg.copy(alpha = 0.78f),
                                    border = if (!controller.isAtLiveEdge) {
                                        BorderStroke(1.dp, LiveCrimson)
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !controller.isAtLiveEdge) {
                                            controller.jumpToLiveEdge()
                                        }
                                        .testTag("player_live_edge_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        LiveBeaconLottie(size = 12.dp)
                                        Text(
                                            text = if (controller.isAtLiveEdge) "پخش زنده" else "بازگشت به زنده",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            if (isFullscreen) {
                                Text(
                                    text = programTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isFullscreen && controller.hlsTracks.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color.Black.copy(alpha = 0.56f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .clickable {
                                            controller.showFullscreenQualityMenu = !controller.showFullscreenQualityMenu
                                            controller.hudInteractionTick += 1
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.HighQuality,
                                            contentDescription = "انتخاب کیفیت",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = controller.hlsTracks.firstOrNull { it.id == controller.selectedTrackId }?.label ?: "کیفیت",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (onToggleChat != null) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (isChatOpen) MaterialTheme.colorScheme.primary
                                    else Color.Black.copy(alpha = 0.56f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .clickable {
                                            controller.hudInteractionTick += 1
                                            onToggleChat()
                                        }
                                        .testTag("player_chat_toggle_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Forum,
                                            contentDescription = "گفتگوی زنده",
                                            tint = if (isChatOpen) MaterialTheme.colorScheme.onPrimary else Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isFullscreen) "گفتگوی زنده" else "چت",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChatOpen) MaterialTheme.colorScheme.onPrimary else Color.White
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color.Black.copy(alpha = 0.56f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { controller.cycleResizeMode() }
                                    .testTag("player_resize_mode_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = "تغییر نسبت تصویر",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = controller.resizeMode.labelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.68f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    } else if (controlsVisible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            if (isSeekable) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.58f)
                                ) {
                                    IconButton(
                                        onClick = { controller.seekRelative(-10_000L) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("player_replay_10s_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Replay10,
                                            contentDescription = "۱۰ ثانیه عقب",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                shadowElevation = 6.dp
                            ) {
                                IconButton(
                                    onClick = { controller.togglePlayPause() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("player_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "توقف پخش" else "ادامه پخش",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            if (isSeekable) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.58f)
                                ) {
                                    IconButton(
                                        onClick = { controller.seekRelative(10_000L) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("player_forward_10s_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Forward10,
                                            contentDescription = "۱۰ ثانیه جلو",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFullscreen && controller.showFullscreenQualityMenu && controller.hlsTracks.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 56.dp, end = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MidnightSurface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, MidnightBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            controller.hlsTracks.forEach { option ->
                                val selected = option.id == controller.selectedTrackId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            controller.applyHlsTrackSelection(option)
                                            controller.showFullscreenQualityMenu = false
                                        }
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f))
                                )
                            )
                            .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isSeekable && controller.durationMs > 0L) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Slider(
                                    value = (controller.positionMs.toFloat() / controller.durationMs.toFloat()).coerceIn(0f, 1f),
                                    onValueChange = { fraction ->
                                        controller.hudInteractionTick += 1
                                        controller.exoPlayer.seekTo((fraction * controller.durationMs).toLong())
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { controller.toggleMute() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("player_mute_button")
                                ) {
                                    Icon(
                                        imageVector = if (controller.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                        contentDescription = if (controller.isMuted) "وصل صدا" else "قطع صدا",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                val timeDisplay = if (isSeekable && controller.durationMs > 0L) {
                                    "${formatPlayerDuration(controller.positionMs)} / ${formatPlayerDuration(controller.durationMs)}"
                                } else if (controller.isLiveStream) {
                                    "زنده · کیفیت ${controller.hlsTracks.firstOrNull { it.id == controller.selectedTrackId }?.label ?: "خودکار"}"
                                } else {
                                    formatPlayerDuration(controller.positionMs)
                                }
                                Text(
                                    text = timeDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag(if (isFullscreen) "exit_fullscreen_button" else "player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = stringResource(
                                        if (isFullscreen) R.string.cd_exit_fullscreen else R.string.cd_fullscreen
                                    ),
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (onToggleChat != null && !controlsVisible && !isChatOpen && playbackError == null) {
            Surface(
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
                color = Color.Black.copy(alpha = 0.44f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onToggleChat() }
                    .testTag("player_chat_edge_tab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = "باز کردن گفتگوی زنده",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        chatOverlayContent?.invoke(this)
    }
}

private fun formatPlayerDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    val raw = if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
    return SportsRepository.toPersianDigits(raw)
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerViewBox(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    useController: Boolean = true,
    keepScreenOn: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                this.useController = useController
                this.keepScreenOn = keepScreenOn
                this.resizeMode = resizeMode
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.useController != useController) {
                view.useController = useController
            }
            if (view.keepScreenOn != keepScreenOn) {
                view.keepScreenOn = keepScreenOn
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
        },
        modifier = modifier
    )
}

@Composable
fun InlineHighlightPlayer(
    title: String,
    mp4Url: String?,
    statusText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val clipPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    KeepActivityScreenOnEffect(enabled = !mp4Url.isNullOrBlank() && errorMsg == null)

    DisposableEffect(clipPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorMsg = "بارگذاری ویدئو ناموفق بود."
            }
        }
        clipPlayer.addListener(listener)
        onDispose {
            clipPlayer.removeListener(listener)
            clipPlayer.release()
        }
    }

    LaunchedEffect(mp4Url) {
        errorMsg = null
        if (!mp4Url.isNullOrBlank()) {
            clipPlayer.setMediaItem(MediaItem.fromUri(mp4Url))
            clipPlayer.prepare()
            clipPlayer.playWhenReady = true
        } else {
            clipPlayer.stop()
            clipPlayer.clearMediaItems()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        tonalElevation = 1.dp,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A1220)),
                contentAlignment = Alignment.Center
            ) {
                if (!mp4Url.isNullOrBlank()) {
                    ExoPlayerViewBox(
                        player = clipPlayer,
                        keepScreenOn = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = errorMsg ?: statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (errorMsg != null || (statusText.isNotBlank() && !mp4Url.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMsg ?: statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
