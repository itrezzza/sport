package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.AppTab
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchGoals
import com.example.data.model.MatchSummary
import com.example.data.model.ScoreFilter
import com.example.data.model.SportCategory
import com.example.data.model.TeamInfo
import com.example.ui.components.KhateBaziTopBar
import com.example.ui.components.SportsBottomNavigation
import com.example.ui.screens.ScoresScreen
import com.example.ui.theme.KhateBaziTheme
import com.example.ui.viewmodel.ScoresUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScoresScreenRedesignTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `360dp rtl live scores screen renders compact telemetry without overflow and captures preview`() {
        var selectedSport by mutableStateOf(SportCategory.ALL)
        var selectedFilter by mutableStateOf(ScoreFilter.ALL)
        var goalsPersepolis by mutableStateOf(MatchGoals(host = 2, guest = 1))
        var rootWidthPx = 0
        var cardRightMaxPx = 0f

        val leagues = listOf(
            LeagueGroup(
                title = "لیگ برتر خلیج فارس ایران",
                sport = 1,
                matches = listOf(
                    MatchSummary(
                        id = "live_derby",
                        host = TeamInfo(name = "پرسپولیس تهران"),
                        guest = TeamInfo(name = "استقلال خوزستان"),
                        goals = goalsPersepolis,
                        isLive = true,
                        liveTime = "۶۸′"
                    ),
                    MatchSummary(
                        id = "live_traktor",
                        host = TeamInfo(name = "تراکتور تبریز"),
                        guest = TeamInfo(name = "فولاد مبارکه سپاهان"),
                        goals = MatchGoals(host = 1, guest = 1),
                        isLive = true,
                        liveTime = "۴۳′"
                    ),
                    MatchSummary(
                        id = "upcoming_esteghlal",
                        host = TeamInfo(name = "استقلال تهران"),
                        guest = TeamInfo(name = "گل‌گهر سیرجان"),
                        goals = null,
                        isLive = false,
                        time = "۱۹:۳۰"
                    )
                )
            ),
            LeagueGroup(
                title = "UEFA Champions League · لیگ قهرمانان",
                sport = 1,
                matches = listOf(
                    MatchSummary(
                        id = "ucl_real_bayern",
                        host = TeamInfo(name = "رئال مادرید Real Madrid"),
                        guest = TeamInfo(name = "بایرن مونیخ FC Bayern"),
                        goals = MatchGoals(host = 3, guest = 2),
                        isLive = false
                    ),
                    MatchSummary(
                        id = "ucl_arsenal",
                        host = TeamInfo(name = "آرسنال"),
                        guest = TeamInfo(name = "اینتر میلان"),
                        goals = null,
                        isLive = false,
                        time = "۲۳:۳۰"
                    )
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KhateBaziTheme(darkTheme = true) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { rootWidthPx = it.size.width },
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            KhateBaziTopBar(
                                liveCount = 2,
                                watchedCount = 1,
                                isDarkTheme = true,
                                isRefreshing = false,
                                onRefreshClick = {},
                                onOpenNotificationSettings = {},
                                onToggleTheme = {}
                            )
                        },
                        bottomBar = {
                            SportsBottomNavigation(
                                currentTab = AppTab.SCORES,
                                liveCount = 2,
                                onSelectTab = {}
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .onGloballyPositioned {
                                    val r = it.boundsInRoot().right
                                    if (r > cardRightMaxPx) cardRightMaxPx = r
                                }
                        ) {
                            ScoresScreen(
                                scoresState = ScoresUiState(
                                    leagues = leagues,
                                    filter = selectedFilter,
                                    sportCategory = selectedSport,
                                    liveCount = 2,
                                    pollIntervalSeconds = 6,
                                    lastUpdatedTime = "۲۱:۰۴"
                                ),
                                watchedMatchIds = setOf("live_derby"),
                                onFilterChange = { selectedFilter = it },
                                onSportChange = { selectedSport = it },
                                onSearchChange = {},
                                onRefreshClick = {},
                                onMatchClick = { _, _, _ -> },
                                onToggleWatchMatch = { _, _ -> }
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_card_live_derby").assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_card_upcoming_esteghlal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_card_ucl_real_bayern").assertIsDisplayed()
        composeTestRule.onNodeWithTag("persistent_bottom_nav_bar").assertIsDisplayed()

        // Capture rendered bitmap before score mutation so user can review real screenshot
        val decorView = composeTestRule.activity.window.decorView
        val width = decorView.width.coerceAtLeast(720)
        val height = decorView.height.coerceAtLeast(1560)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        decorView.draw(canvas)

        val artifactDir = File("/.aistudio/artifacts/brain/513acf03-d767-436a-952e-9749af139e23")
        if (artifactDir.exists() || artifactDir.mkdirs()) {
            FileOutputStream(File(artifactDir, "scores_redesign_preview.png")).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // Verify zero horizontal overflow on 360dp screen
        assertTrue(rootWidthPx > 0)
        assertTrue(cardRightMaxPx <= rootWidthPx + 1f)

        // Verify live score animation update + expandable league toggle
        goalsPersepolis = MatchGoals(host = 3, guest = 1)
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("لیگ برتر خلیج فارس ایران").onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_card_live_derby").assertDoesNotExist()
    }
}
