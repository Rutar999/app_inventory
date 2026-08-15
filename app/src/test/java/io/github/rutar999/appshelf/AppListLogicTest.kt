package io.github.rutar999.appshelf

import io.github.rutar999.appshelf.TestFixtures.DAY
import io.github.rutar999.appshelf.model.AppFilter
import io.github.rutar999.appshelf.model.SortOrder
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.ui.applist.AppListLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListLogicTest {

    private val now = 1_700_000_000_000L
    private val mb = 1024L * 1024L

    @Test
    fun `お気に入りは並び順にかかわらず先頭に来る`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "a", label = "Apple"),
            TestFixtures.entry(packageName = "z", label = "Zebra", isFavorite = true)
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(),
            sort = SortOrder.NAME,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals("z", result.first().packageName)
    }

    @Test
    fun `未使用フィルタはしきい値を超えたものだけ残す`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "recent", usage = TestFixtures.usedDaysAgo(10, now)),
            TestFixtures.entry(packageName = "old", usage = TestFixtures.usedDaysAgo(120, now)),
            TestFixtures.entry(packageName = "never", usage = TestFixtures.neverUsed)
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(unusedOnly = true),
            sort = SortOrder.NAME,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals(setOf("old", "never"), result.map { it.packageName }.toSet())
    }

    @Test
    fun `使用状況が取れないアプリは未使用扱いにしない`() {
        // 使用状況アクセスが未許可のときに「全部未使用」と誤判定しないこと
        val entries = listOf(TestFixtures.entry(packageName = "unknown", usage = null))
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(unusedOnly = true),
            sort = SortOrder.NAME,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `最終起動が古い順では起動記録なしが先頭に来る`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "recent", usage = TestFixtures.usedDaysAgo(1, now)),
            TestFixtures.entry(packageName = "old", usage = TestFixtures.usedDaysAgo(300, now)),
            TestFixtures.entry(packageName = "never", usage = TestFixtures.neverUsed)
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(),
            sort = SortOrder.LAST_USED_ASC,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals(listOf("never", "old", "recent"), result.map { it.packageName })
    }

    @Test
    fun `大容量フィルタはしきい値以上だけ残す`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "small", sizes = TestFixtures.sizes(appMb = 50)),
            TestFixtures.entry(packageName = "big", sizes = TestFixtures.sizes(appMb = 400)),
            TestFixtures.entry(packageName = "unknown", sizes = null)
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(largeOnly = true),
            sort = SortOrder.SIZE_DESC,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals(listOf("big"), result.map { it.packageName })
    }

    @Test
    fun `タグ名でも検索できる`() {
        val tag = Tag(id = 1L, name = "仕事", colorArgb = 0, sortOrder = 0)
        val entries = listOf(
            TestFixtures.entry(packageName = "tagged", label = "Slack", tags = listOf(tag)),
            TestFixtures.entry(packageName = "other", label = "Game")
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "仕事",
            filter = AppFilter(),
            sort = SortOrder.NAME,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals(listOf("tagged"), result.map { it.packageName })
    }

    @Test
    fun `インストール日が新しい順に並ぶ`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "old", firstInstallTime = now - 100 * DAY),
            TestFixtures.entry(packageName = "new", firstInstallTime = now - 1 * DAY)
        )
        val result = AppListLogic.apply(
            entries = entries,
            query = "",
            filter = AppFilter(),
            sort = SortOrder.INSTALLED_DESC,
            unusedDays = 90,
            largeThresholdBytes = 200 * mb,
            now = now
        )
        assertEquals(listOf("new", "old"), result.map { it.packageName })
    }
}
