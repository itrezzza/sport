package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.BroadcastSource
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightBorder
import com.example.ui.theme.MidnightSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class FeaturedSlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val leagueLabel: String = "",
    val isLive: Boolean,
    val imageUrl: String,
    val hostName: String = "",
    val guestName: String = "",
    val hostLogoUrl: String = "",
    val guestLogoUrl: String = "",
    val sources: List<BroadcastSource> = emptyList(),
    val onClick: (() -> Unit)? = null
)

@Composable
fun FeaturedMatchSlider(
    games: List<MergedBroadcastGame>,
    featuredNews: List<NewsArticle> = emptyList(),
    isLoading: Boolean = false,
    onGameClick: (MergedBroadcastGame) -> Unit,
    onNewsClick: (NewsArticle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val slides = remember(games, featuredNews) {
        if (games.isNotEmpty()) {
            val rankedGames = games.sortedByDescending { game ->
                var score = 0
                if (game.isLive) score += 8
                if (game.posterUrl.isNotBlank()) score += 6
                if (game.newsPhotoUrl.isNotBlank()) score += 3
                if (game.hostLogoUrl.isNotBlank() || game.guestLogoUrl.isNotBlank()) score += 2
                score
            }
            rankedGames.take(6).map { game ->
                val parts = game.title.split('-', '–', '—')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val hostName = parts.getOrNull(0).orEmpty()
                val guestName = parts.getOrNull(1).orEmpty()
                val realPhoto = game.posterUrl.ifBlank { game.newsPhotoUrl }

                FeaturedSlideItem(
                    id = game.key,
                    title = game.title,
                    subtitle = game.time.ifBlank { "پخش امروز" },
                    leagueLabel = game.leagueTitle,
                    isLive = game.isLive,
                    imageUrl = realPhoto,
                    hostName = hostName,
                    guestName = guestName,
                    hostLogoUrl = game.hostLogoUrl,
                    guestLogoUrl = game.guestLogoUrl,
                    sources = game.sources.keys.toList(),
                    onClick = { onGameClick(game) }
                )
            }
        } else {
            featuredNews
                .filter { it.imageUrl.isNotBlank() }
                .take(5)
                .map { article ->
                    FeaturedSlideItem(
                        id = "news_${article.id}",
                        title = article.title,
                        subtitle = if (article.publishedAtFa.isNotBlank()) {
                            "${article.sourceName} · ${article.publishedAtFa}"
                        } else {
                            article.sourceName
                        },
                        leagueLabel = article.category.labelFa,
                        isLive = false,
                        imageUrl = article.imageUrl,
                        onClick = { onNewsClick(article) }
                    )
                }
        }
    }

    if (slides.isEmpty()) {
        if (isLoading) {
            MatchSkeletonLoader(
                variant = MatchSkeletonVariant.HERO_CARD,
                modifier = modifier.padding(horizontal = 16.dp)
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(slides.size, isDragged) {
        if (slides.size <= 1 || isDragged) return@LaunchedEffect
        while (isActive) {
            delay(4600L)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("featured_match_slider"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("todays_poster_pager")
        ) { page ->
            val slide = slides[page]
            val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = rawOffset.absoluteValue
            val fraction = 1f - absOffset.coerceIn(0f, 1f)

            FeaturedPosterCard(
                slide = slide,
                rawOffset = rawOffset,
                fraction = fraction,
                onClick = {
                    if (pagerState.currentPage != page) {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = page,
                                animationSpec = tween(460, easing = FastOutSlowInEasing)
                            )
                        }
                    } else {
                        slide.onClick?.invoke()
                    }
                }
            )
        }

        if (slides.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 26.dp else 6.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "posterDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        animationSpec = tween(250),
                        label = "posterDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = tween(460, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedPosterCard(
    slide: FeaturedSlideItem,
    rawOffset: Float,
    fraction: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(slide.imageUrl) {
        if (slide.imageUrl.isBlank()) null
        else ImageRequest.Builder(context)
            .data(slide.imageUrl)
            .crossfade(350)
            .memoryCacheKey(slide.imageUrl)
            .diskCacheKey(slide.imageUrl)
            .build()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer {
                    val scale = lerp(0.94f, 1f, fraction)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(0.75f, 1f, fraction)
                }
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .testTag("poster_slide_${slide.id}"),
            shape = RoundedCornerShape(18.dp),
            color = MidnightBg,
            shadowElevation = if (fraction > 0.7f) 8.dp else 2.dp,
            border = BorderStroke(
                width = if (slide.isLive && fraction > 0.7f) 1.2.dp else 1.dp,
                color = if (slide.isLive && fraction > 0.7f) LiveCrimson.copy(alpha = 0.75f)
                else MidnightBorder
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val parallaxModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.09f
                        scaleY = 1.09f
                        translationX = rawOffset.coerceIn(-1f, 1f) * -32f
                    }

                if (imageRequest != null) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = slide.title,
                        contentScale = ContentScale.Crop,
                        modifier = parallaxModifier,
                        loading = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                MatchupStudioBackdrop(slide = slide, modifier = Modifier.fillMaxSize())
                                PosterShimmerOverlay(modifier = Modifier.fillMaxSize())
                            }
                        },
                        error = {
                            MatchupStudioBackdrop(slide = slide, modifier = Modifier.fillMaxSize())
                        }
                    )
                } else {
                    MatchupStudioBackdrop(
                        slide = slide,
                        modifier = parallaxModifier
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MidnightBg.copy(alpha = 0.94f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (slide.isLive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LiveCrimson
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LivePulseIndicator(color = Color.White, size = 6.dp)
                                Text(
                                    text = "زنده",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    slide.sources.forEach { src ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MidnightBg.copy(alpha = 0.78f),
                            border = BorderStroke(0.8.dp, MidnightBorder)
                        ) {
                            Text(
                                text = src.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (src == BroadcastSource.ANTEN) ElectricLime else BroadcastCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (slide.leagueLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MidnightBg.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = slide.leagueLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLime.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchupStudioBackdrop(
    slide: FeaturedSlideItem,
    modifier: Modifier = Modifier
) {
    val hasTeams = slide.hostLogoUrl.isNotBlank() || slide.guestLogoUrl.isNotBlank() ||
        (slide.hostName.isNotBlank() && slide.guestName.isNotBlank())

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A2812),
                    MidnightSurface,
                    MidnightBg
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.White.copy(alpha = 0.06f)
            drawLine(
                color = lineColor,
                start = Offset(size.width * 0.5f, 0f),
                end = Offset(size.width * 0.5f, size.height),
                strokeWidth = 2f
            )
            drawCircle(
                color = lineColor,
                radius = size.minDimension * 0.25f,
                center = Offset(size.width * 0.5f, size.height * 0.44f),
                style = Stroke(width = 2f)
            )
        }

        if (hasTeams) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp)
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(
                    logoUrl = slide.hostLogoUrl,
                    teamName = slide.hostName.ifBlank { slide.title },
                    size = 56.dp,
                    highlightBorder = true
                )
                Surface(
                    shape = CircleShape,
                    color = ElectricLime.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = ElectricLime,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
                TeamLogo(
                    logoUrl = slide.guestLogoUrl,
                    teamName = slide.guestName.ifBlank { slide.title },
                    size = 56.dp,
                    highlightBorder = true
                )
            }
        } else {
            Surface(
                shape = CircleShape,
                color = ElectricLime.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 26.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PosterShimmerOverlay(modifier: Modifier = Modifier) {
    val brush = rememberBroadcastShimmerBrush()
    Box(modifier = modifier.background(brush))
}
