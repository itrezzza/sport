package com.example

import com.example.data.model.BroadcastSource
import com.example.data.network.SportsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File

class ExampleUnitTest {
    @Test
    fun vazirmatnFontsSupportPersianGlyphs() {
        val weights = listOf(
            "vazirmatn.ttf",
            "vazirmatn_medium.ttf",
            "vazirmatn_semibold.ttf",
            "vazirmatn_bold.ttf",
            "vazirmatn_extrabold.ttf"
        )
        for (name in weights) {
            val file = File("src/main/res/font/$name")
            assertTrue("Missing font file: $name", file.exists())
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            val ok = font.canDisplay('\u06CC') && font.canDisplay('\u0627') && font.canDisplay('\u06F1')
            assertTrue("$name is missing Persian glyphs", ok)
        }
    }

    @Test
    fun broadcastSourcesContainOnlyAntenAndAparat() {
        assertEquals(
            listOf(BroadcastSource.ANTEN, BroadcastSource.APARAT),
            BroadcastSource.entries
        )
    }

    @Test
    fun normalizeRemoteImageUrlHandlesHttpProtocolRelativeAndPaths() {
        assertEquals(
            "https://news-cdn.varzesh3.ir/pictures/2025/photo.jpg",
            SportsRepository.normalizeRemoteImageUrl("http://news-cdn.varzesh3.ir/pictures/2025/photo.jpg")
        )
        assertEquals(
            "https://www.anten.ir/uploads/poster.webp",
            SportsRepository.normalizeRemoteImageUrl("uploads/poster.webp", "https://www.anten.ir")
        )
        assertEquals(
            "https://www.aparatsport.com/images/match.png",
            SportsRepository.normalizeRemoteImageUrl("//www.aparatsport.com/images/match.png")
        )
    }

    @Test
    fun extractOgImageAndNewsHtmlMapResolveRealImages() {
        val articleHtml = """
            <html><head>
            <meta property="og:image" content="http://news-cdn.varzesh3.ir/pictures/2025/05/10/hero.jpg" />
            </head></html>
        """.trimIndent()
        assertEquals(
            "https://news-cdn.varzesh3.ir/pictures/2025/05/10/hero.jpg",
            SportsRepository.extractOgImageUrl(articleHtml)
        )

        val newsListHtml = """
            <div class="news-item">
              <a href="/news/2104567/derby-report"><img data-src="http://news-cdn.varzesh3.ir/pictures/2025/05/10/derby.jpg" /></a>
              <div class="title">
                <a href="/news/2104567/derby-report">گزارش ویژه از آمادگی دو تیم پیش از دربی بزرگ تهران</a>
              </div>
            </div>
        """.trimIndent()

        val articles = SportsRepository.parseVarzesh3NewsPageHtml(newsListHtml)
        assertEquals(1, articles.size)
        assertEquals("2104567", articles[0].id)
        assertEquals(
            "https://news-cdn.varzesh3.ir/pictures/2025/05/10/derby.jpg",
            articles[0].imageUrl
        )
    }
}
