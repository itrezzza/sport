package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.NewsArticle
import com.example.data.model.NewsCategory
import com.example.data.network.SportsRepository
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.MatchSkeletonLoader
import com.example.ui.components.MatchSkeletonVariant
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.bouncyClickable
import com.example.ui.components.rememberBroadcastShimmerBrush
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightBorder
import com.example.ui.viewmodel.NewsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun NewsScreen(
    newsState: NewsUiState,
    onCategoryChange: (NewsCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchOpen by rememberSaveable { mutableStateOf(newsState.searchQuery.isNotBlank()) }
    val query = newsState.searchQuery.trim()
    val filtered = remember(newsState.articles, newsState.selectedCategory, query) {
        newsState.articles.filter { article ->
            val categoryOk = newsState.selectedCategory == NewsCategory.ALL ||
                article.category == newsState.selectedCategory
            val queryOk = query.isBlank() ||
                article.title.contains(query, ignoreCase = true) ||
                article.summary.contains(query, ignoreCase = true)
            categoryOk && queryOk
        }
    }
    val (sliderArticles, wireArticles) = remember(filtered) {
        val photoFirst = filtered.filter { it.imageUrl.isNotBlank() }
        val withoutPhoto = filtered.filter { it.imageUrl.isBlank() }
        val slider = (photoFirst + withoutPhoto).take(5)
        val sliderIds = slider.map { it.id }.toSet()
        slider to filtered.filter { it.id !in sliderIds }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "news_compact_toolbar") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NewsCategory.entries.forEach { category ->
                                val selected = newsState.selectedCategory == category
                                val chipBg by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                    animationSpec = tween(220),
                                    label = "newsCategoryBg"
                                )
                                val chipText by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(220),
                                    label = "newsCategoryText"
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = chipBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .bouncyClickable(pressedScale = 0.94f) {
                                            onCategoryChange(category)
                                        }
                                        .testTag("news_category_${category.name.lowercase()}")
                                ) {
                                    Text(
                                        text = category.labelFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = chipText,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (searchOpen) {
                                    onSearchChange("")
                                    searchOpen = false
                                } else {
                                    searchOpen = true
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("news_toggle_search")
                        ) {
                            Icon(
                                imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (searchOpen) "بستن جستجو" else "جستجوی خبر",
                                tint = if (searchOpen) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !newsState.isLoading,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("news_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "به‌روزرسانی اخبار",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = searchOpen || newsState.searchQuery.isNotEmpty(),
                        enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                    ) {
                        OutlinedTextField(
                            value = newsState.searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            placeholder = {
                                Text("جستجو در خبرهای ورزشی…", style = MaterialTheme.typography.bodySmall)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = if (newsState.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onSearchChange("") }) {
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
                                .testTag("news_search_input")
                        )
                    }
                }
            }

            if (newsState.isLoading && newsState.articles.isEmpty()) {
                item(key = "news_loading_skeleton") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MatchSkeletonLoader(variant = MatchSkeletonVariant.HERO_CARD)
                        MatchSkeletonLoader(variant = MatchSkeletonVariant.LIST_ROWS)
                    }
                }
            } else if (filtered.isEmpty()) {
                item(key = "news_empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = newsState.errorMessage
                                    ?: "خبری با این جستجو یا دسته‌بندی پیدا نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (newsState.errorMessage != null) {
                                OutlinedButton(
                                    onClick = onRefreshClick,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تلاش دوباره", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "news_hero_slider") {
                    StaggeredEntrance(index = 0) {
                        TrendingNewsCarousel(
                            articles = sliderArticles,
                            onArticleClick = onArticleClick
                        )
                    }
                }

                if (wireArticles.isNotEmpty()) {
                    if (isWide) {
                        val rows = wireArticles.chunked(2)
                        items(rows.size, key = { "row_${rows[it].first().id}" }) { rowIdx ->
                            val pair = rows[rowIdx]
                            StaggeredEntrance(index = rowIdx + 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { article ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            NewsEditorialRow(
                                                article = article,
                                                onClick = { onArticleClick(article) }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "news_wire_board") {
                            StaggeredEntrance(index = 2) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
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
                                                text = "پیشخوان خبرها",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                        Text(
                                            text = "${SportsRepository.toPersianDigits(wireArticles.size)} خبر",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Column {
                                            wireArticles.forEachIndexed { idx, article ->
                                                NewsEditorialRow(
                                                    article = article,
                                                    onClick = { onArticleClick(article) }
                                                )
                                                if (idx < wireArticles.lastIndex) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 14.dp),
                                                        thickness = 0.8.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant
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

            item(key = "news_bottom_pad") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun TrendingNewsCarousel(
    articles: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    if (articles.isEmpty()) return
    val reducedMotion = rememberReducedMotionEnabled()
    val pagerState = rememberPagerState(pageCount = { articles.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(articles.size, isDragged, reducedMotion) {
        if (articles.size <= 1 || isDragged || reducedMotion) return@LaunchedEffect
        while (isActive) {
            delay(4800L)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % articles.size
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("news_hero_slider"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("news_hero_pager")
        ) { page ->
            val article = articles[page]
            val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val fraction = 1f - rawOffset.absoluteValue.coerceIn(0f, 1f)

            FeaturedNewsCard(
                article = article,
                rawOffset = rawOffset,
                fraction = fraction,
                onClick = {
                    if (pagerState.currentPage != page) {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = page,
                                animationSpec = tween(
                                    if (reducedMotion) 0 else 460,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    } else {
                        onArticleClick(article)
                    }
                }
            )
        }

        if (articles.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(articles.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 24.dp else 6.dp,
                        animationSpec = if (reducedMotion) tween(0) else spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "newsDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        animationSpec = tween(if (reducedMotion) 0 else 250),
                        label = "newsDotColor"
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
                                        animationSpec = tween(
                                            if (reducedMotion) 0 else 460,
                                            easing = FastOutSlowInEasing
                                        )
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
fun FeaturedNewsCard(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rawOffset: Float = 0f,
    fraction: Float = 1f
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val scale = lerp(0.95f, 1f, fraction)
                scaleX = scale
                scaleY = scale
                alpha = lerp(0.78f, 1f, fraction)
            }
            .clip(RoundedCornerShape(18.dp))
            .bouncyClickable(onClick = onClick)
            .testTag("news_hero_card_${article.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MidnightBg,
        border = BorderStroke(1.dp, MidnightBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
        ) {
            NewsCoverImage(
                imageUrl = article.imageUrl,
                categoryLabel = article.category.labelFa,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.07f
                        scaleY = 1.07f
                        translationX = rawOffset.coerceIn(-1f, 1f) * -28f
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.12f),
                                0.48f to MidnightBg.copy(alpha = 0.56f),
                                1f to MidnightBg.copy(alpha = 0.96f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricLime
                ) {
                    Text(
                        text = article.category.labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF091203),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (article.publishedAtFa.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MidnightBg.copy(alpha = 0.72f),
                        border = BorderStroke(0.7.dp, MidnightBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = article.publishedAtFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD5E0F2),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsEditorialRow(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("news_card_${article.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NewsCoverImage(
            imageUrl = article.imageUrl,
            categoryLabel = article.category.labelFa,
            contentDescription = article.title,
            modifier = Modifier
                .width(104.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = article.category.labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                if (article.publishedAtFa.isNotBlank()) {
                    Text(
                        text = article.publishedAtFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (article.summary.isNotBlank()) {
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewsCoverImage(
    imageUrl: String,
    categoryLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(imageUrl) {
        if (imageUrl.isBlank()) null
        else ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(300)
            .memoryCacheKey(imageUrl)
            .diskCacheKey(imageUrl)
            .build()
    }

    if (request != null) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    NewsShimmerOverlay(modifier = Modifier.fillMaxSize())
                }
            },
            error = {
                NewsMinimalPlaceholder(
                    categoryLabel = categoryLabel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        NewsMinimalPlaceholder(
            categoryLabel = categoryLabel,
            modifier = modifier
        )
    }
}

@Composable
private fun NewsMinimalPlaceholder(
    categoryLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Newspaper,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NewsShimmerOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(rememberBroadcastShimmerBrush())
    )
}

@Composable
fun NewsReaderModal(
    article: NewsArticle,
    articleBody: String,
    isLoadingBody: Boolean,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(276.dp)
            ) {
                NewsCoverImage(
                    imageUrl = article.imageUrl,
                    categoryLabel = article.category.labelFa,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MidnightBg.copy(alpha = 0.72f),
                        border = BorderStroke(0.8.dp, MidnightBorder)
                    ) {
                        Text(
                            text = "${article.sourceName} · ${article.category.labelFa}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricLime,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MidnightBg.copy(alpha = 0.72f),
                        border = BorderStroke(0.8.dp, MidnightBorder)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("close_news_reader_button")
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (article.publishedAtFa.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = article.publishedAtFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (article.summary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(74.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = article.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                if (isLoadingBody) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (articleBody.isNotBlank()) {
                    Text(
                        text = SportsRepository.toPersianDigits(articleBody),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 29.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
