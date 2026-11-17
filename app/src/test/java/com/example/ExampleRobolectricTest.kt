package com.example

import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppTab
import com.example.data.model.BroadcastProgram
import com.example.data.model.BroadcastSource
import com.example.data.model.ChatMessage
import com.example.data.model.FavoriteTeam
import com.example.data.model.InstagramMediaType
import com.example.data.model.LeagueGroup
import com.example.data.model.LineupPlayer
import com.example.data.model.LineupSide
import com.example.data.model.MatchDetailInfo
import com.example.data.model.MatchEntry
import com.example.data.model.MatchEvent
import com.example.data.model.MatchGoals
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.model.NewsCategory
import com.example.data.model.NotificationSettings
import com.example.data.model.PlayerMatchContribution
import com.example.data.model.PlayerProfileData
import com.example.data.model.ScoreFilter
import com.example.data.model.StatEntry
import com.example.data.model.TeamInfo
import com.example.data.model.TeamProfileData
import com.example.data.model.WatchedMatch
import com.example.data.network.InstagramRepository
import com.example.data.network.SportsRepository
import com.example.ui.components.AnimatedScoreboard
import com.example.ui.components.BroadcastPlayerStage
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.FavoriteTeamButton
import com.example.ui.components.FootballPitchVisualization
import com.example.ui.components.FullscreenBroadcastPlayerOverlay
import com.example.ui.components.GoalEventAnimation
import com.example.ui.components.KeepActivityScreenOnEffect
import com.example.ui.components.KhateBaziBottomBar
import com.example.ui.components.LeagueHeader
import com.example.ui.components.LiveBeaconLottie
import com.example.ui.components.LiveChatFeedState
import com.example.ui.components.LiveMatchChatSlideOutOverlay
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.LiveScoreTicker
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.LiveStreamingPlayer
import com.example.ui.components.LoadingPitchLottie
import com.example.ui.components.MatchSkeletonLoader
import com.example.ui.components.MatchSkeletonVariant
import com.example.ui.components.MatchStatisticsPanel
import com.example.ui.components.MatchStatsVicoChart
import com.example.ui.components.PremiumLiveMatchCard
import com.example.ui.components.ScoreboardSize
import com.example.ui.components.SportsBottomNavigation
import com.example.ui.components.StartupSplashOverlay
import com.example.ui.components.TacticalPitchView
import com.example.ui.components.TeamFormVicoChart
import com.example.ui.components.TeamLogo
import com.example.ui.components.rememberBroadcastPlayerController
import com.example.ui.screens.BroadcastScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FeaturedNewsCard
import com.example.ui.screens.MatchDetailModal
import com.example.ui.screens.MatchTimeline
import com.example.ui.screens.NewsReaderModal
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.PlayerProfileSheet
import com.example.ui.screens.ScoresScreen
import com.example.ui.screens.TeamProfileSheet
import com.example.ui.screens.TrendingNewsCarousel
import com.example.ui.theme.KhateBaziTheme
import com.example.ui.viewmodel.BroadcastUiState
import com.example.ui.viewmodel.DetailTab
import com.example.ui.viewmodel.MatchDetailUiState
import com.example.ui.viewmodel.NewsUiState
import com.example.ui.viewmodel.ScoresUiState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("خط بازی", appName)
    }

    @Test
    fun `extractPosterUrl and team logos handle nested and relative formats`() {
        val obj = JSONObject().apply {
            put("posters", JSONArray().put(JSONObject().put("mobile", "uploads/big.jpg")))
            put("mobilePoster", "uploads/match_poster.webp")
            put("team1", JSONObject().put("logo", "http://match-cdn.varzesh3.ir/t1.png"))
            put("team2_logo", "/logos/t2.png")
        }

        val poster = SportsRepository.extractPosterUrl(obj, "https://www.anten.ir")
        val (hLogo, gLogo) = SportsRepository.extractTeamLogos(obj, "https://www.aparatsport.com")

        assertEquals("https://www.anten.ir/uploads/match_poster.webp", poster)
        assertEquals("https://match-cdn.varzesh3.ir/t1.png", hLogo)
        assertEquals("https://www.aparatsport.com/logos/t2.png", gLogo)
    }

    @Test
    fun `parseVarzesh3RssFeed extracts enclosure and media thumbnail images`() {
        val rss = """
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <item>
                  <title>پیروزی حساس در هفته پایانی لیگ برتر</title>
                  <link>http://www.varzesh3.ir/news/2105001/match-win</link>
                  <description><![CDATA[<p>خلاصه مسابقه و حواشی</p>]]></description>
                  <media:thumbnail url="http://news-cdn.varzesh3.ir/pictures/2025/thumb.jpg" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val items = SportsRepository.parseVarzesh3RssFeed(rss)
        assertEquals(1, items.size)
        assertEquals("https://news-cdn.varzesh3.ir/pictures/2025/thumb.jpg", items[0].imageUrl)
        assertEquals("https://www.varzesh3.ir/news/2105001/match-win", items[0].link)
    }

    @Test
    fun `instagram normalizeUsername handles handles links and whitespace`() {
        assertEquals("varzesh3", InstagramRepository.normalizeUsername("  @Varzesh3 "))
        assertEquals(
            "esteghlal.fc.official",
            InstagramRepository.normalizeUsername("https://www.instagram.com/esteghlal.fc.official/?hl=en")
        )
        assertEquals(
            "perspolis",
            InstagramRepository.normalizeUsername("https://instagram.com/perspolis/reels/")
        )
    }

    @Test
    fun `parseWebProfileInfoJson extracts avatar timeline videos and sidecar carousels`() {
        val webProfileJson = """
            {
              "data": {
                "user": {
                  "username": "varzesh3",
                  "full_name": "ورزش سه",
                  "profile_pic_url_hd": "https:\/\/cdn.example.com\/avatar.jpg?a=1\u0026b=2",
                  "edge_owner_to_timeline_media": {
                    "edges": [
                      {
                        "node": {
                          "id": "p_1",
                          "shortcode": "ABC123xyz",
                          "is_video": true,
                          "product_type": "clips",
                          "display_url": "https:\/\/cdn.example.com\/reel_cover.jpg",
                          "video_url": "https:\/\/cdn.example.com\/reel.mp4",
                          "taken_at_timestamp": 1763200000,
                          "edge_media_to_caption": {
                            "edges": [
                              { "node": { "text": "گل تماشایی در دقیقه ۹۰" } }
                            ]
                          }
                        }
                      },
                      {
                        "node": {
                          "id": "p_2",
                          "shortcode": "CAR789xyz",
                          "is_video": false,
                          "edge_media_to_caption": {
                            "edges": [
                              { "node": { "text": "گزارش تصویری" } }
                            ]
                          },
                          "edge_sidecar_to_children": {
                            "edges": [
                              {
                                "node": {
                                  "id": "c_1",
                                  "is_video": false,
                                  "display_url": "https://cdn.example.com/slide1.jpg"
                                }
                              },
                              {
                                "node": {
                                  "id": "c_2",
                                  "is_video": true,
                                  "display_url": "https://cdn.example.com/slide2.jpg",
                                  "video_url": "https://cdn.example.com/slide2.mp4"
                                }
                              }
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val group = InstagramRepository.parseWebProfileInfoJson(webProfileJson, "varzesh3")
        assertNotNull(group)
        assertEquals("varzesh3", group!!.username)
        assertEquals("ورزش سه", group.displayName)
        assertEquals("https://cdn.example.com/avatar.jpg?a=1&b=2", group.avatarUrl)
        assertEquals("ریلز و پست", group.badgeLabel)
        assertEquals(3, group.items.size)
        assertEquals(InstagramMediaType.VIDEO, group.items[0].mediaType)
        assertEquals("https://cdn.example.com/reel.mp4", group.items[0].mediaUrl)
        assertEquals("https://www.instagram.com/reel/ABC123xyz/", group.items[0].permalink)
        assertEquals(InstagramMediaType.IMAGE, group.items[1].mediaType)
        assertEquals(InstagramMediaType.VIDEO, group.items[2].mediaType)
    }

    @Test
    fun `parseInstagramCookieString validates sessionid and extracts csrf and userId`() {
        assertNull(InstagramRepository.parseInstagramCookieString("csrftoken=abc; mid=xyz"))
        assertNull(InstagramRepository.parseInstagramCookieString("sessionid=deleted; csrftoken=abc"))

        val session = InstagramRepository.parseInstagramCookieString(
            rawCookie = "mid=123; csrftoken=csrf_999; ds_user_id=556677; sessionid=real_sess_token",
            userAgent = "CustomUA"
        )
        assertNotNull(session)
        assertTrue(session!!.isAuthenticated)
        assertEquals("csrf_999", session.csrfToken)
        assertEquals("556677", session.userId)
        assertEquals("CustomUA", session.userAgent)
        assertTrue(session.cookieHeader.contains("sessionid=real_sess_token"))
    }

    @Test
    fun `parseReelsTrayJson and parseReelsMediaJson parse live 24h photo and video stories`() {
        val trayJson = """
            {
              "tray": [
                {
                  "id": "1001",
                  "user": {
                    "pk": "1001",
                    "username": "varzesh3",
                    "full_name": "ورزش سه",
                    "profile_pic_url": "https:\/\/cdn.example.com\/v3_avatar.jpg"
                  },
                  "items": [
                    {
                      "id": "st_img_1",
                      "pk": "9001",
                      "media_type": 1,
                      "taken_at": 1763200000,
                      "image_versions2": {
                        "candidates": [
                          { "width": 720, "height": 1280, "url": "https:\/\/cdn.example.com\/story_low.jpg" },
                          { "width": 1080, "height": 1920, "url": "https:\/\/cdn.example.com\/story_hd.jpg" }
                        ]
                      }
                    }
                  ]
                },
                {
                  "id": "2002",
                  "user": {
                    "pk": "2002",
                    "username": "perspolis",
                    "full_name": "پرسپولیس",
                    "profile_pic_url": "https://cdn.example.com/pp_avatar.jpg"
                  }
                }
              ]
            }
        """.trimIndent()

        val trayEntries = InstagramRepository.parseReelsTrayJson(trayJson)
        assertEquals(2, trayEntries.size)
        assertEquals("1001", trayEntries[0].reelId)
        assertEquals("varzesh3", trayEntries[0].group.username)
        assertEquals(1, trayEntries[0].group.items.size)
        assertEquals(InstagramMediaType.IMAGE, trayEntries[0].group.items[0].mediaType)
        assertEquals("https://cdn.example.com/story_hd.jpg", trayEntries[0].group.items[0].mediaUrl)
        assertTrue(trayEntries[0].group.items[0].isLiveStory)
        assertEquals(0, trayEntries[1].group.items.size)

        val reelsMediaJson = """
            {
              "reels": {
                "2002": {
                  "user": { "pk": "2002", "username": "perspolis" },
                  "items": [
                    {
                      "id": "st_vid_1",
                      "pk": "9002",
                      "media_type": 2,
                      "taken_at": 1763200100,
                      "image_versions2": {
                        "candidates": [
                          { "width": 1080, "height": 1920, "url": "https://cdn.example.com/vid_cover.jpg" }
                        ]
                      },
                      "video_versions": [
                        { "width": 720, "height": 1280, "url": "https:\/\/cdn.example.com\/story_video.mp4" }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val hydrated = InstagramRepository.parseReelsMediaJson(reelsMediaJson)
        assertEquals(1, hydrated["2002"]?.size)
        assertEquals(InstagramMediaType.VIDEO, hydrated["2002"]?.get(0)?.mediaType)
        assertEquals("https://cdn.example.com/story_video.mp4", hydrated["2002"]?.get(0)?.mediaUrl)
        assertEquals("https://cdn.example.com/vid_cover.jpg", hydrated["2002"]?.get(0)?.thumbnailUrl)
    }

    @Test
    fun `persistent bottom bar shows dedicated Live Scores tab with live badge and navigates on click`() {
        var selectedTab = AppTab.BROADCAST
        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                KhateBaziBottomBar(
                    currentTab = selectedTab,
                    liveCount = 4,
                    onSelectTab = { selectedTab = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("persistent_bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_scores").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_scores_live_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_scores").performClick()
        assertEquals(AppTab.SCORES, selectedTab)
    }

    @Test
    fun `buildSessionFromManualToken parses bare sessionid cookie header and rejects invalid inputs`() {
        assertNull(InstagramRepository.buildSessionFromManualToken("short"))
        assertNull(InstagramRepository.buildSessionFromManualToken("invalid token with spaces"))

        val encodedSession = InstagramRepository.buildSessionFromManualToken("123456789%3AabcDefGhi%3A27")
        assertNotNull(encodedSession)
        assertTrue(encodedSession!!.isAuthenticated)
        assertEquals("123456789", encodedSession.userId)
        assertTrue(encodedSession.cookieHeader.contains("sessionid=123456789%3AabcDefGhi%3A27"))
        assertTrue(encodedSession.cookieHeader.contains("ds_user_id=123456789"))

        val colonSession = InstagramRepository.buildSessionFromManualToken("987654321:xyzToken:11")
        assertNotNull(colonSession)
        assertEquals("987654321", colonSession!!.userId)

        val fullCookie = InstagramRepository.buildSessionFromManualToken(
            "Cookie: csrftoken = tok_1 ; ds_user_id=445566; SessionId = full_cookie_token_value"
        )
        assertNotNull(fullCookie)
        assertEquals("445566", fullCookie!!.userId)
        assertEquals("tok_1", fullCookie.csrfToken)
        assertTrue(fullCookie.cookieHeader.contains("sessionid=full_cookie_token_value"))

        val tsvRow = InstagramRepository.buildSessionFromManualToken(
            "sessionid\t11223344%3AtokenFromDevTools%3A27\t.instagram.com\t/\t2027-01-01"
        )
        assertNotNull(tsvRow)
        assertEquals("11223344", tsvRow!!.userId)
        assertTrue(tsvRow.cookieHeader.contains("sessionid=11223344%3AtokenFromDevTools%3A27"))

        val jsonExport = InstagramRepository.buildSessionFromManualToken(
            """[{"name":"csrftoken","value":"csrf_json"},{"name":"sessionid","value":"778899%3AjsonTok"}]"""
        )
        assertNotNull(jsonExport)
        assertEquals("778899", jsonExport!!.userId)
        assertEquals("csrf_json", jsonExport.csrfToken)

        val quotedAndRtlMarked = InstagramRepository.buildSessionFromManualToken(
            "\"\u200F554433%3ArtlCleanToken\u200E\";"
        )
        assertNotNull(quotedAndRtlMarked)
        assertEquals("554433", quotedAndRtlMarked!!.userId)
        assertTrue(quotedAndRtlMarked.cookieHeader.contains("sessionid=554433%3ArtlCleanToken"))
    }

    @Test
    fun `live score ticker renders ongoing matches horizontally and handles match and scores clicks`() {
        val liveMatch = MatchSummary(
            id = "m_live_1",
            host = TeamInfo(name = "پرسپولیس"),
            guest = TeamInfo(name = "استقلال"),
            goals = MatchGoals(host = 2, guest = 1),
            isLive = true,
            liveTime = "۶۷'"
        )
        val finishedMatch = MatchSummary(
            id = "m_done_2",
            host = TeamInfo(name = "سپاهان"),
            guest = TeamInfo(name = "تراکتور"),
            goals = MatchGoals(host = 0, guest = 0),
            isLive = false,
            statusTitle = "پایان بازی"
        )
        var clickedMatchId: String? = null
        var openedScoresTab = false

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                LiveScoreTicker(
                    leagues = listOf(
                        LeagueGroup(
                            title = "لیگ برتر ایران",
                            sport = 1,
                            matches = listOf(liveMatch, finishedMatch)
                        )
                    ),
                    isLoading = false,
                    lastUpdatedTime = "۱۸:۴۵:۰۰",
                    onMatchClick = { match, _, _ -> clickedMatchId = match.id },
                    onOpenScoresTab = { openedScoresTab = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_score_ticker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_score_ticker_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ticker_match_m_live_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ticker_match_m_done_2").assertDoesNotExist()

        composeTestRule.onNodeWithTag("ticker_match_m_live_1").performClick()
        assertEquals("m_live_1", clickedMatchId)

        composeTestRule.onNodeWithTag("ticker_view_all_scores").performClick()
        assertTrue(openedScoresTab)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `live score ticker supports pull to refresh and manual rotating sync button`() {
        val liveMatch = MatchSummary(
            id = "m_sync_1",
            host = TeamInfo(name = "تیم میزبان"),
            guest = TeamInfo(name = "تیم میهمان"),
            goals = MatchGoals(host = 1, guest = 0),
            isLive = true,
            liveTime = "۳۲'"
        )
        var refreshCount = 0
        var isSyncing by mutableStateOf(false)

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = {
                        refreshCount += 1
                        isSyncing = true
                    },
                    state = pullState,
                    indicator = {
                        LiveScoresPullRefreshIndicator(
                            state = pullState,
                            isRefreshing = isSyncing,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("test_ticker_pull_container")
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            LiveScoreTicker(
                                leagues = listOf(
                                    LeagueGroup(
                                        title = "لیگ نمونه",
                                        sport = 1,
                                        matches = listOf(liveMatch)
                                    )
                                ),
                                isLoading = isSyncing,
                                lastUpdatedTime = "۱۹:۰۰:۰۰",
                                onMatchClick = { _, _, _ -> },
                                onOpenScoresTab = {},
                                onRefreshScores = {
                                    refreshCount += 1
                                    isSyncing = true
                                }
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_score_ticker_sync_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_score_ticker_sync_icon", useUnmergedTree = true).assertIsDisplayed()

        // Pull down gesture triggers manual refresh and displays rotating pull indicator
        composeTestRule.onNodeWithTag("test_ticker_pull_container").performTouchInput {
            swipeDown(startY = 120f, endY = 900f, durationMillis = 350)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, refreshCount)
        composeTestRule.onNodeWithTag("live_scores_pull_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_scores_pull_refresh_icon", useUnmergedTree = true).assertIsDisplayed()

        // Reset refreshing and test direct sync button tap
        isSyncing = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_score_ticker_sync_button").performClick()
        assertEquals(2, refreshCount)
    }

    @Test
    fun `broadcast player keeps screen awake with FLAG_KEEP_SCREEN_ON and exposes pro controls`() {
        var activeUrl by mutableStateOf<String?>("https://example.com/live/stream.m3u8")

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                BroadcastPlayerStage(
                    activeProgramId = if (activeUrl != null) "prog_1" else null,
                    programTitle = "پخش زنده لیگ قهرمانان",
                    sourceLabel = "آنتن",
                    playerStatus = "پخش مستقیم",
                    streamUrl = activeUrl,
                    qualities = emptyList(),
                    activeQualityIndex = 0,
                    isFullscreen = false,
                    onSelectQuality = {},
                    onToggleFullscreen = {},
                    onClosePlayer = { activeUrl = null },
                    onPlayerError = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        val windowFlagsWhilePlaying = composeTestRule.activity.window.attributes.flags
        assertTrue((windowFlagsWhilePlaying and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        composeTestRule.onNodeWithTag("player_keep_screen_on_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_resize_mode_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("تناسب").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_resize_mode_button").performClick()
        composeTestRule.onNodeWithText("بزرگ‌نمایی").assertIsDisplayed()

        // Closing the player releases FLAG_KEEP_SCREEN_ON cleanly
        composeTestRule.onNodeWithTag("player_close_button").performClick()
        composeTestRule.waitForIdle()
        val windowFlagsAfterClose = composeTestRule.activity.window.attributes.flags
        assertEquals(0, windowFlagsAfterClose and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Test
    fun `hoisted player controller preserves ExoPlayer across layout switches and fullscreen overlay`() {
        var isFullscreen by mutableStateOf(false)
        var simulateWideSwitch by mutableStateOf(false)
        val observedPlayers = mutableListOf<ExoPlayer>()

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                val controller = rememberBroadcastPlayerController(
                    streamUrl = "https://example.com/live/derby.m3u8",
                    onPlayerError = {}
                )
                observedPlayers.add(controller.exoPlayer)
                KeepActivityScreenOnEffect(enabled = true)

                Box(modifier = Modifier.fillMaxSize()) {
                    if (simulateWideSwitch) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            BroadcastPlayerStage(
                                activeProgramId = "prog_derby",
                                programTitle = "دربی پایتخت",
                                sourceLabel = "آپارات اسپرت",
                                playerStatus = "در حال پخش",
                                streamUrl = "https://example.com/live/derby.m3u8",
                                qualities = emptyList(),
                                activeQualityIndex = 0,
                                isFullscreen = isFullscreen,
                                onSelectQuality = {},
                                onToggleFullscreen = { isFullscreen = it },
                                onClosePlayer = {},
                                onPlayerError = {},
                                controller = controller
                            )
                        }
                    } else {
                        BroadcastPlayerStage(
                            activeProgramId = "prog_derby",
                            programTitle = "دربی پایتخت",
                            sourceLabel = "آپارات اسپرت",
                            playerStatus = "در حال پخش",
                            streamUrl = "https://example.com/live/derby.m3u8",
                            qualities = emptyList(),
                            activeQualityIndex = 0,
                            isFullscreen = isFullscreen,
                            onSelectQuality = {},
                            onToggleFullscreen = { isFullscreen = it },
                            onClosePlayer = {},
                            onPlayerError = {},
                            controller = controller
                        )
                    }

                    if (isFullscreen) {
                        FullscreenBroadcastPlayerOverlay(
                            controller = controller,
                            programTitle = "دربی پایتخت",
                            onExitFullscreen = { isFullscreen = false }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialPlayer = observedPlayers.first()
        composeTestRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertTrue(isFullscreen)
        composeTestRule.onNodeWithTag("fullscreen_player_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exit_fullscreen_button").assertIsDisplayed()

        // Simulate horizontal/wide branch re-composition while fullscreen overlay is active
        simulateWideSwitch = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fullscreen_player_container").assertIsDisplayed()
        assertTrue(observedPlayers.all { it === initialPlayer })

        // Exiting fullscreen restores inline player and keeps FLAG_KEEP_SCREEN_ON intact
        composeTestRule.onNodeWithTag("exit_fullscreen_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(!isFullscreen)
        composeTestRule.onNodeWithTag("fullscreen_player_container").assertDoesNotExist()
        composeTestRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed()
        assertTrue(observedPlayers.all { it === initialPlayer })
        val flagsAfterExit = composeTestRule.activity.window.attributes.flags
        assertTrue((flagsAfterExit and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `startup splash overlay displays loading UI when visible and hides after dismiss`() {
        var splashVisible by mutableStateOf(true)

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                StartupSplashOverlay(visible = splashVisible)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("app_startup_loading").assertIsDisplayed()
        composeTestRule.onNodeWithText("خط بازی").assertIsDisplayed()

        splashVisible = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("app_startup_loading").assertDoesNotExist()
    }

    @Test
    fun `broadcast screen hides ticker, news section, and poster header while allowing chat toggle`() {
        val sampleGame = MergedBroadcastGame(
            key = "derby_live",
            title = "پرسپولیس - استقلال",
            time = "۱۷:۳۰",
            isLive = true,
            leagueTitle = "لیگ برتر",
            sources = mapOf(
                BroadcastSource.ANTEN to BroadcastProgram(
                    id = "prog_1",
                    title = "پرسپولیس - استقلال",
                    time = "۱۷:۳۰",
                    isLive = true,
                    source = BroadcastSource.ANTEN
                )
            )
        )
        val liveMatch = MatchSummary(
            id = "m_1",
            host = TeamInfo(name = "سپاهان"),
            guest = TeamInfo(name = "تراکتور"),
            goals = MatchGoals(host = 1, guest = 1),
            isLive = true,
            liveTime = "۵۴'"
        )
        val article = NewsArticle(
            id = "news_home_1",
            title = "خبر نمونه اول",
            summary = "خلاصه خبر",
            link = "https://www.varzesh3.ir/news/1",
            imageUrl = "",
            publishedAtFa = "",
            category = NewsCategory.IRAN_FOOTBALL
        )
        var chatToggledCount = 0

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                BroadcastScreen(
                    broadcastState = BroadcastUiState(
                        mergedGames = listOf(sampleGame),
                        activeProgramId = "prog_1",
                        activeProgramTitle = "پرسپولیس - استقلال",
                        streamUrl = "https://example.com/live/stream.m3u8"
                    ),
                    scoresState = ScoresUiState(
                        leagues = listOf(
                            LeagueGroup(
                                title = "لیگ برتر",
                                sport = 1,
                                matches = listOf(liveMatch)
                            )
                        )
                    ),
                    featuredNews = listOf(article),
                    onFilterChange = {},
                    onMergedGameClick = {},
                    onSelectQuality = {},
                    onToggleFullscreen = {},
                    onClosePlayer = {},
                    onPlayerError = {},
                    onSearchChange = {},
                    onRefreshPrograms = {},
                    isChatOpen = false,
                    onToggleChat = { chatToggledCount += 1 }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("featured_match_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_score_ticker").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_sports_news_section").assertDoesNotExist()
        assertEquals(0, composeTestRule.onAllNodesWithText("پوستر بازی‌های روز").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("گزارش تصویری روز").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithTag("player_header_chat_button").assertIsDisplayed().performClick()
        assertEquals(1, chatToggledCount)
    }

    @Test
    fun `news screen renders pro hero slider without fresh badges and reader modal hides website button`() {
        val articles = listOf(
            NewsArticle(
                id = "n_1",
                title = "پیروزی ارزشمند در لیگ قهرمانان",
                summary = "توضیحات مسابقه اول",
                link = "https://www.varzesh3.ir/news/101",
                imageUrl = "",
                publishedAtFa = "",
                category = NewsCategory.WORLD_FOOTBALL
            ),
            NewsArticle(
                id = "n_2",
                title = "گزارش تمرین پیش از دربی",
                summary = "توضیحات تمرین",
                link = "https://www.varzesh3.ir/news/102",
                imageUrl = "",
                publishedAtFa = "۱۸:۲۰",
                category = NewsCategory.IRAN_FOOTBALL
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize()) {
                    NewsScreen(
                        newsState = NewsUiState(articles = articles),
                        onCategoryChange = {},
                        onSearchChange = {},
                        onRefreshClick = {},
                        onArticleClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("news_hero_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_hero_pager").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("تازه").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("تازه‌ترین‌های تصویری").fetchSemanticsNodes().size)
    }

    @Test
    fun `news reader modal hides external website button`() {
        val article = NewsArticle(
            id = "n_1",
            title = "پیروزی ارزشمند در لیگ قهرمانان",
            summary = "توضیحات مسابقه اول",
            link = "https://www.varzesh3.ir/news/101",
            imageUrl = "",
            publishedAtFa = "",
            category = NewsCategory.WORLD_FOOTBALL
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                NewsReaderModal(
                    article = article,
                    articleBody = "متن کامل گزارش ورزشی",
                    isLoadingBody = false,
                    onClose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("پیروزی ارزشمند در لیگ قهرمانان").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_news_external_button").assertDoesNotExist()
        assertEquals(0, composeTestRule.onAllNodesWithText("مشاهدهٔ خبر در وب‌سایت").fetchSemanticsNodes().size)
    }

    @Test
    fun `match detail modal omits external watch and full match page buttons`() {
        val match = MatchSummary(
            id = "m_detail_1",
            host = TeamInfo(name = "تیم الف"),
            guest = TeamInfo(name = "تیم ب"),
            goals = MatchGoals(host = 2, guest = 0),
            isLive = true,
            liveTime = "۸۱'",
            link = "https://www.varzesh3.ir/match/m_detail_1",
            liveStreamLink = "https://www.anten.ir/live/1"
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                MatchDetailModal(
                    state = MatchDetailUiState(
                        isOpen = true,
                        match = match,
                        leagueTitle = "لیگ برتر"
                    ),
                    isWatched = false,
                    onToggleWatch = {},
                    onSelectTab = {},
                    onToggleHighlight = {},
                    onCloseHighlight = {},
                    onClose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("تیم الف").assertIsDisplayed()
        composeTestRule.onNodeWithText("تیم ب").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("تماشای پخش").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("صفحهٔ کامل بازی").fetchSemanticsNodes().size)
    }

    @Test
    fun `live chat overlay shows google sign in gate when signed out and displays messages when signed in`() {
        var userId by mutableStateOf<String?>(null)
        var signInClicks = 0
        val messages = listOf(
            ChatMessage(
                id = "msg_1",
                roomId = "room_1",
                userId = "uid_alice",
                senderName = "کاربر",
                text = "عجب گلی زد!"
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiveMatchChatSlideOutOverlay(
                        isOpen = true,
                        isCompactInline = false,
                        programTitle = "پخش زنده ویژه",
                        currentUserId = userId,
                        currentUserName = if (userId != null) "کاربر" else null,
                        feedState = if (userId != null) LiveChatFeedState.Ready(messages) else LiveChatFeedState.Idle,
                        isSending = false,
                        isSigningIn = false,
                        authError = null,
                        onGoogleSignIn = { signInClicks += 1 },
                        onSignOut = {},
                        onSendMessage = {},
                        onDeleteMessage = {},
                        onClose = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_chat_slide_out_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_google_sign_in_button").assertIsDisplayed().performClick()
        assertEquals(1, signInClicks)

        userId = "uid_alice"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_google_sign_in_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("live_chat_message_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_message_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_send_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("عجب گلی زد!").assertIsDisplayed()
    }

    @Test
    fun `bottom navigation bar displays all 4 tabs including favorites`() {
        var selectedTab by mutableStateOf(AppTab.BROADCAST)
        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                KhateBaziBottomBar(
                    currentTab = selectedTab,
                    liveCount = 3,
                    onSelectTab = { selectedTab = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_broadcast").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_scores").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_news").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_favorites").assertIsDisplayed().performClick()
        assertEquals(AppTab.FAVORITES, selectedTab)
    }

    @Test
    fun `lottie vector animations render without composition errors`() {
        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                Column {
                    LiveBeaconLottie(modifier = Modifier.testTag("lottie_beacon"))
                    LoadingPitchLottie(modifier = Modifier.testTag("lottie_pitch"))
                    EmptyCrestLottie(modifier = Modifier.testTag("lottie_empty"))
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lottie_beacon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lottie_pitch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lottie_empty").assertIsDisplayed()
    }

    @Test
    fun `vico charts and tactical pitch view render and handle player selection`() {
        var clickedPlayer: String? = null
        val stats = listOf(
            StatEntry(title = "مالکیت توپ", hostValue = "۵۸٪", guestValue = "۴۲٪", hostPercent = 58f),
            StatEntry(title = "شوت در چارچوب", hostValue = "۷", guestValue = "۳", hostPercent = 70f)
        )
        val matches = listOf(
            MatchEntry(
                match = MatchSummary(
                    id = "m_form_1",
                    host = TeamInfo(name = "پرسپولیس"),
                    guest = TeamInfo(name = "سپاهان"),
                    goals = MatchGoals(host = 2, guest = 1)
                ),
                leagueTitle = "لیگ برتر",
                sport = 1
            )
        )
        val lineup = LineupSide(
            formation = "4-3-3",
            coach = "سرمربی نمونه",
            starters = listOf(
                LineupPlayer(name = "علیرضا بیرانوند", shirtNumber = "1", isCaptain = true),
                LineupPlayer(name = "مهدی ترابی", shirtNumber = "9", isManOfTheMatch = true),
                LineupPlayer(name = "وحید امیری", shirtNumber = "19")
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    MatchStatsVicoChart(
                        stats = stats,
                        hostName = "پرسپولیس",
                        guestName = "سپاهان"
                    )
                    TeamFormVicoChart(
                        matches = matches,
                        teamName = "پرسپولیس"
                    )
                    TacticalPitchView(
                        teamName = "پرسپولیس",
                        side = lineup,
                        onPlayerClick = { clickedPlayer = it.name }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("match_stats_vico_chart").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("team_form_vico_chart").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tactical_pitch_view").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pitch_player_مهدی ترابی").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("مهدی ترابی", clickedPlayer)
    }

    @Test
    fun `match detail modal supports team drilldown and stats or lineup visualizations`() {
        var openedTeam: String? = null
        var activeTab by mutableStateOf(DetailTab.STATS)
        val match = MatchSummary(
            id = "m_drill_1",
            host = TeamInfo(name = "تراکتور"),
            guest = TeamInfo(name = "استقلال"),
            goals = MatchGoals(host = 2, guest = 1),
            isLive = true,
            liveTime = "۷۴'"
        )
        val detailInfo = MatchDetailInfo(
            stats = listOf(
                StatEntry(title = "شوت", hostValue = "۱۲", guestValue = "۸", hostPercent = 60f)
            ),
            hostLineup = LineupSide(
                formation = "4-2-3-1",
                starters = listOf(LineupPlayer(name = "امیرحسین حسین‌زاده", shirtNumber = "10"))
            ),
            guestLineup = LineupSide(
                formation = "3-5-2",
                starters = listOf(LineupPlayer(name = "حسین حسینی", shirtNumber = "1"))
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                MatchDetailModal(
                    state = MatchDetailUiState(
                        isOpen = true,
                        match = match,
                        leagueTitle = "لیگ برتر",
                        activeTab = activeTab,
                        detailInfo = detailInfo
                    ),
                    isWatched = false,
                    onToggleWatch = {},
                    onSelectTab = { activeTab = it },
                    onToggleHighlight = {},
                    onCloseHighlight = {},
                    onClose = {},
                    onTeamClick = { team, _, _ -> openedTeam = team.name }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("detail_host_team").assertIsDisplayed().performClick()
        assertEquals("تراکتور", openedTeam)

        composeTestRule.onNodeWithTag("match_detail_list").performScrollToKey("detail_body")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("match_stats_vico_chart").assertIsDisplayed()

        composeTestRule.onNodeWithTag("match_detail_list").performScrollToKey("detail_tabs")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("detail_tab_lineup").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("match_detail_list").performScrollToKey("detail_body")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tactical_pitch_view").assertIsDisplayed()
    }

    @Test
    fun `team profile sheet shows club stats, favorite toggle, and form chart`() {
        var favoriteToggled = false
        val profile = TeamProfileData(
            teamName = "سپاهان",
            leagueTitle = "لیگ برتر ایران",
            formation = "4-3-3",
            coach = "پاتریس کارترون",
            matches = listOf(
                MatchEntry(
                    match = MatchSummary(
                        id = "m_team_1",
                        host = TeamInfo(name = "سپاهان"),
                        guest = TeamInfo(name = "ذوب‌آهن"),
                        goals = MatchGoals(host = 3, guest = 1)
                    ),
                    leagueTitle = "لیگ برتر ایران",
                    sport = 1
                )
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                TeamProfileSheet(
                    profile = profile,
                    isFavorite = false,
                    onToggleFavorite = { favoriteToggled = true },
                    onMatchClick = { _, _, _ -> },
                    onClose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_profile_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("سپاهان").assertIsDisplayed()
        composeTestRule.onNodeWithTag("team_profile_favorite_button").performClick()
        assertTrue(favoriteToggled)
        composeTestRule.onNodeWithTag("team_profile_list").performScrollToKey("team_form_chart")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_form_vico_chart").assertIsDisplayed()
    }

    @Test
    fun `player profile sheet displays bio metrics and clickable team chip`() {
        var clickedTeam: String? = null
        val profile = PlayerProfileData(
            player = LineupPlayer(
                name = "سردار آزمون",
                shirtNumber = "20",
                isCaptain = true,
                isManOfTheMatch = true,
                score = "8.7"
            ),
            teamName = "تیم ملی ایران",
            leagueTitle = "مقدماتی جام جهانی",
            matchLabel = "ایران ـ قطر",
            goalsCount = 2,
            assistsCount = 1,
            contributions = listOf(
                PlayerMatchContribution(
                    minute = "۴۱′",
                    title = "گل",
                    detail = "سردار آزمون (پاس گل: مهدی طارمی)",
                    eventType = 1
                )
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                PlayerProfileSheet(
                    profile = profile,
                    onTeamClick = { teamName, _ -> clickedTeam = teamName },
                    onClose = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_profile_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("سردار آزمون").assertIsDisplayed()
        composeTestRule.onNodeWithText("بهترین بازیکن زمین").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_profile_team_chip").performClick()
        assertEquals("تیم ملی ایران", clickedTeam)
    }

    @Test
    fun `favorites screen displays followed teams, search discovery, and notifications panel`() {
        var toggledTeam: String? = null
        val leagues = listOf(
            LeagueGroup(
                title = "لیگ برتر",
                sport = 1,
                matches = listOf(
                    MatchSummary(
                        id = "fav_m_1",
                        host = TeamInfo(name = "پرسپولیس"),
                        guest = TeamInfo(name = "تراکتور"),
                        goals = MatchGoals(host = 1, guest = 0),
                        isLive = true,
                        liveTime = "۶۱'"
                    )
                )
            )
        )
        val favorites = listOf(
            FavoriteTeam(name = "پرسپولیس", leagueTitle = "لیگ برتر", sport = 1)
        )
        val watched = listOf(
            WatchedMatch(
                id = "fav_m_1",
                hostName = "پرسپولیس",
                guestName = "تراکتور",
                leagueTitle = "لیگ برتر"
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                FavoritesScreen(
                    favoriteTeams = favorites,
                    leagues = leagues,
                    watchedMatches = watched,
                    notificationSettings = NotificationSettings(),
                    onToggleFavoriteTeam = { team, _, _ -> toggledTeam = team.name },
                    onTeamClick = { _, _, _ -> },
                    onMatchClick = { _, _, _ -> },
                    onToggleWatchMatch = { _, _ -> },
                    onUnwatchMatch = {},
                    onUpdateNotificationSettings = {},
                    onSendTestNotification = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("favorites_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("favorites_followed_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("favorite_team_card_پرسپولیس").assertIsDisplayed()
        composeTestRule.onNodeWithTag("favorites_team_matches").assertIsDisplayed()

        composeTestRule.onNodeWithTag("favorites_screen").performScrollToKey("team_discovery_section")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("favorites_team_search_input").performTextInput("تراکتور")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discover_team_تراکتور").assertIsDisplayed().performClick()
        assertEquals("تراکتور", toggledTeam)

        composeTestRule.onNodeWithTag("favorites_screen").performScrollToKey("favorites_notification_prefs")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("favorites_notifications_panel").assertIsDisplayed()
    }

    @Test
    fun `all 15 reusable broadcast components render and handle interactive states`() {
        var favoriteState by mutableStateOf(false)
        var leagueExpanded by mutableStateOf(true)
        var matchClicks = 0
        var retryClicks = 0
        var navTab by mutableStateOf(AppTab.BROADCAST)
        var newsClicks = 0

        val sampleMatch = MatchSummary(
            id = "comp_m_1",
            host = TeamInfo(name = "پرسپولیس"),
            guest = TeamInfo(name = "استقلال"),
            goals = MatchGoals(host = 2, guest = 1),
            isLive = true,
            liveTime = "۶۸′"
        )
        val sampleEntry = MatchEntry(match = sampleMatch, leagueTitle = "لیگ برتر ایران", sport = 1)
        val sampleEvents = listOf(
            MatchEvent(
                id = "ev_goal_1",
                time = "64",
                rawTime = 64,
                eventType = 1,
                side = 0,
                description = "ضربه سر در محوطه جریمه",
                strikerName = "علی علیپور",
                hostScore = 2,
                guestScore = 1
            )
        )
        val sampleStats = listOf(
            StatEntry(title = "مالکیت توپ", hostValue = "۵۶٪", guestValue = "۴۴٪", hostPercent = 56f)
        )
        val sampleLineup = LineupSide(
            formation = "4-3-3",
            starters = listOf(
                LineupPlayer(name = "الکسیس گندوز", shirtNumber = "1", isCaptain = true),
                LineupPlayer(name = "اوستون اورونوف", shirtNumber = "70", isManOfTheMatch = true)
            )
        )
        val sampleArticles = listOf(
            NewsArticle(
                id = "comp_art_1",
                title = "تحلیل فنی دربی پایتخت",
                summary = "نگاهی به آمار مالکیت و موقعیت‌های گل دو تیم",
                link = "https://example.com/news/1",
                category = NewsCategory.IRAN_FOOTBALL,
                publishedAtFa = "۱۰ دقیقه پیش"
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    LivePulseIndicator(label = "زنده", modifier = Modifier.testTag("comp_live_pulse"))
                    TeamLogo(logoUrl = "", teamName = "پرسپولیس", modifier = Modifier.testTag("comp_team_logo"))
                    AnimatedScoreboard(
                        goals = sampleMatch.goals,
                        isLive = true,
                        size = ScoreboardSize.SPOTLIGHT,
                        modifier = Modifier.testTag("comp_animated_scoreboard")
                    )
                    LeagueHeader(
                        title = "لیگ برتر ایران",
                        matchCount = 4,
                        hasLive = true,
                        expanded = leagueExpanded,
                        onToggleExpand = { leagueExpanded = !leagueExpanded },
                        modifier = Modifier.testTag("comp_league_header")
                    )
                    PremiumLiveMatchCard(
                        entry = sampleEntry,
                        onClick = { matchClicks += 1 },
                        isWatched = true,
                        onToggleWatch = {}
                    )
                    PremiumLiveMatchCard(
                        entry = sampleEntry.copy(match = sampleMatch.copy(id = "err_1")),
                        onClick = {},
                        errorMessage = "خطا در بارگذاری",
                        onRetry = { retryClicks += 1 }
                    )
                    FavoriteTeamButton(
                        isFavorite = favoriteState,
                        teamName = "پرسپولیس",
                        onToggle = { favoriteState = !favoriteState },
                        modifier = Modifier.testTag("comp_fav_button")
                    )
                    MatchSkeletonLoader(variant = MatchSkeletonVariant.LIST_ROWS)
                    GoalEventAnimation(size = 24.dp)
                    MatchTimeline(events = sampleEvents, match = sampleMatch)
                    MatchStatisticsPanel(
                        stats = sampleStats,
                        hostName = "پرسپولیس",
                        guestName = "استقلال"
                    )
                    FootballPitchVisualization(
                        teamName = "پرسپولیس",
                        side = sampleLineup,
                        onPlayerClick = {}
                    )
                    TrendingNewsCarousel(
                        articles = sampleArticles,
                        onArticleClick = { newsClicks += 1 }
                    )
                    FeaturedNewsCard(
                        article = sampleArticles.first().copy(id = "single_card"),
                        onClick = { newsClicks += 1 }
                    )
                    LiveStreamingPlayer(
                        activeProgramId = null,
                        programTitle = "پخش آزمایشی",
                        sourceLabel = "آنتن",
                        playerStatus = "",
                        streamUrl = null,
                        qualities = emptyList(),
                        activeQualityIndex = 0,
                        isFullscreen = false,
                        onSelectQuality = {},
                        onToggleFullscreen = {},
                        onClosePlayer = {},
                        onPlayerError = {}
                    )
                    SportsBottomNavigation(
                        currentTab = navTab,
                        liveCount = 2,
                        onSelectTab = { navTab = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("comp_live_pulse").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("comp_team_logo").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("comp_animated_scoreboard").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("comp_league_header").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(false, leagueExpanded)

        composeTestRule.onNodeWithTag("premium_match_card_comp_m_1").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, matchClicks)
        composeTestRule.onNodeWithText("تلاش دوباره").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, retryClicks)

        composeTestRule.onNodeWithTag("comp_fav_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(favoriteState)

        composeTestRule.onNodeWithTag("match_skeleton_loader").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("match_timeline").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("goal_event_animation").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("goal_event_animation")[0].performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("match_statistics_panel").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tactical_pitch_view").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_hero_slider").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("news_hero_card_single_card").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, newsClicks)
        composeTestRule.onNodeWithTag("persistent_bottom_nav_bar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `home screen renders league and favorite club shortcuts while preserving minimal live layout`() {
        var clickedLeague: String? = null
        var clickedTeam: String? = null
        val leagues = listOf(
            LeagueGroup(
                title = "لیگ برتر ایران",
                sport = 1,
                matches = listOf(
                    MatchSummary(
                        id = "hm_1",
                        host = TeamInfo(name = "تراکتور"),
                        guest = TeamInfo(name = "سپاهان"),
                        isLive = true,
                        liveTime = "۳۳′"
                    )
                )
            )
        )
        val favorites = listOf(FavoriteTeam(name = "تراکتور", leagueTitle = "لیگ برتر ایران", sport = 1))

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                BroadcastScreen(
                    broadcastState = BroadcastUiState(
                        isLoadingPrograms = false,
                        mergedGames = listOf(
                            MergedBroadcastGame(
                                key = "g_shortcut_1",
                                title = "تراکتور - سپاهان",
                                time = "۱۹:۳۰",
                                isLive = true,
                                sources = emptyMap()
                            )
                        )
                    ),
                    scoresState = ScoresUiState(leagues = leagues, liveCount = 1),
                    favoriteTeams = favorites,
                    onFilterChange = {},
                    onMergedGameClick = {},
                    onTeamClick = { team, _, _ -> clickedTeam = team.name },
                    onLeagueShortcutClick = { clickedLeague = it },
                    onSelectQuality = {},
                    onToggleFullscreen = {},
                    onClosePlayer = {},
                    onPlayerError = {},
                    onSearchChange = {},
                    onRefreshPrograms = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_score_ticker").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_sports_news_section").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_shortcuts_strip").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_fav_team_تراکتور").performScrollTo().performClick()
        assertEquals("تراکتور", clickedTeam)
        composeTestRule.onNodeWithTag("home_league_shortcut_لیگ برتر ایران").performScrollTo().performClick()
        assertEquals("لیگ برتر ایران", clickedLeague)
    }

    @Test
    fun `scores screen supports upcoming and finished fixture subfilters`() {
        val leagues = listOf(
            LeagueGroup(
                title = "لیگ قهرمانان",
                sport = 1,
                matches = listOf(
                    MatchSummary(
                        id = "up_1",
                        host = TeamInfo(name = "رئال مادرید"),
                        guest = TeamInfo(name = "بایرن مونیخ"),
                        time = "۲۲:۳۰",
                        goals = null,
                        isLive = false
                    ),
                    MatchSummary(
                        id = "fin_1",
                        host = TeamInfo(name = "آرسنال"),
                        guest = TeamInfo(name = "اینتر"),
                        goals = MatchGoals(host = 1, guest = 0),
                        isLive = false
                    )
                )
            )
        )

        composeTestRule.setContent {
            KhateBaziTheme(darkTheme = true) {
                ScoresScreen(
                    scoresState = ScoresUiState(
                        leagues = leagues,
                        filter = ScoreFilter.ALL
                    ),
                    watchedMatchIds = emptySet(),
                    onFilterChange = {},
                    onSportChange = {},
                    onSearchChange = {},
                    onRefreshClick = {},
                    onMatchClick = { _, _, _ -> },
                    onToggleWatchMatch = { _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_card_up_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_card_fin_1").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("score_schedule_upcoming").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_card_up_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_card_fin_1").assertDoesNotExist()

        composeTestRule.onNodeWithTag("score_schedule_finished").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_card_fin_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_card_up_1").assertDoesNotExist()
    }
}
