package com.example

import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppTab
import com.example.data.model.InstagramMediaType
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchGoals
import com.example.data.model.MatchSummary
import com.example.data.model.TeamInfo
import com.example.data.network.InstagramRepository
import com.example.data.network.SportsRepository
import com.example.ui.components.BroadcastPlayerStage
import com.example.ui.components.FullscreenBroadcastPlayerOverlay
import com.example.ui.components.KeepActivityScreenOnEffect
import com.example.ui.components.KhateBaziBottomBar
import com.example.ui.components.LiveScoreTicker
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.rememberBroadcastPlayerController
import com.example.ui.theme.KhateBaziTheme
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
}
