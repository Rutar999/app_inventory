package io.github.rutar999.appshelf

import io.github.rutar999.appshelf.util.SearchText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {

    @Test
    fun `カタカナはひらがなに正規化される`() {
        assertEquals("かめら", SearchText.normalize("カメラ"))
    }

    @Test
    fun `全角英数と半角カナは NFKC で揃う`() {
        assertEquals("youtube", SearchText.normalize("ＹｏｕＴｕｂｅ"))
        assertEquals("かめら", SearchText.normalize("ｶﾒﾗ"))
    }

    @Test
    fun `ひらがなで打ってもカタカナのアプリ名にあたる`() {
        val target = SearchText.normalize("カメラ com.example.camera")
        assertTrue(SearchText.matches(target, "かめら"))
        assertTrue(SearchText.matches(target, "カメラ"))
    }

    @Test
    fun `空白区切りは AND 検索になる`() {
        val target = SearchText.normalize("Google Maps com.google.android.apps.maps")
        assertTrue(SearchText.matches(target, "google maps"))
        assertTrue(SearchText.matches(target, "maps google"))
        assertFalse(SearchText.matches(target, "google chrome"))
    }

    @Test
    fun `検索語が空なら常にマッチする`() {
        assertTrue(SearchText.matches("なんでも", ""))
        assertTrue(SearchText.matches("なんでも", "   "))
    }

    @Test
    fun `パッケージ名でも引ける`() {
        val target = SearchText.normalize("マップ com.google.android.apps.maps")
        assertTrue(SearchText.matches(target, "com.google"))
    }
}
