package io.github.rutar999.appshelf

import io.github.rutar999.appshelf.TestFixtures.DAY
import io.github.rutar999.appshelf.ui.home.DashboardLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardLogicTest {

    private val now = 1_700_000_000_000L
    private val mb = 1024L * 1024L

    @Test
    fun `合計容量は本体とデータの合計になる`() {
        // dataBytes にはキャッシュが含まれているので、cache を足すと二重計上になる
        val entries = listOf(
            TestFixtures.entry(packageName = "a", sizes = TestFixtures.sizes(appMb = 100, dataMb = 50, cacheMb = 20))
        )
        val stats = DashboardLogic.calculate(entries, unusedDays = 90, usageAvailable = true, now = now)
        assertEquals(150 * mb, stats.totalBytes)
    }

    @Test
    fun `使用状況が使えないときは未使用件数を出さない`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "a", sizes = TestFixtures.sizes(appMb = 10))
        )
        val stats = DashboardLogic.calculate(entries, unusedDays = 90, usageAvailable = false, now = now)
        assertEquals(0, stats.unusedCount)
        assertNull(stats.reclaimableBytes)
    }

    @Test
    fun `回収可能容量は未使用アプリの合計`() {
        val entries = listOf(
            TestFixtures.entry(
                packageName = "unused",
                sizes = TestFixtures.sizes(appMb = 300),
                usage = TestFixtures.usedDaysAgo(200, now)
            ),
            TestFixtures.entry(
                packageName = "active",
                sizes = TestFixtures.sizes(appMb = 100),
                usage = TestFixtures.usedDaysAgo(2, now)
            )
        )
        val stats = DashboardLogic.calculate(entries, unusedDays = 90, usageAvailable = true, now = now)
        assertEquals(1, stats.unusedCount)
        assertEquals(300 * mb, stats.reclaimableBytes)
    }

    @Test
    fun `使用状況がなくてもインストールが古い順は出せる`() {
        val entries = listOf(
            TestFixtures.entry(packageName = "new", firstInstallTime = now - 5 * DAY),
            TestFixtures.entry(packageName = "old", firstInstallTime = now - 900 * DAY)
        )
        val stats = DashboardLogic.calculate(entries, unusedDays = 90, usageAvailable = false, now = now)
        assertEquals("old", stats.oldestInstalls.first().packageName)
    }

    @Test
    fun `容量が1件も取れていないときは合計を null にする`() {
        val entries = listOf(TestFixtures.entry(packageName = "a", sizes = null))
        val stats = DashboardLogic.calculate(entries, unusedDays = 90, usageAvailable = false, now = now)
        assertNull(stats.totalBytes)
    }
}
