package io.github.rutar999.appshelf.ui.home

import io.github.rutar999.appshelf.model.AppEntry

/** ダッシュボードに出す集計結果（F-10 / F-11 / F-12 / F-13 / F-15）。 */
data class DashboardStats(
    val totalApps: Int,
    /** 容量が 1 件も取れていない（使用状況アクセス未許可）なら null */
    val totalBytes: Long?,
    val unusedCount: Int,
    /** 未使用アプリを全部消したときに回収できる推定容量 */
    val reclaimableBytes: Long?,
    val topBySize: List<AppEntry>,
    val topByUsage: List<AppEntry>,
    /** 使用状況が取れないときの代替: インストールが古い順（F-15） */
    val oldestInstalls: List<AppEntry>
)

object DashboardLogic {

    fun calculate(
        entries: List<AppEntry>,
        unusedDays: Int,
        usageAvailable: Boolean,
        topCount: Int = 5,
        now: Long = System.currentTimeMillis()
    ): DashboardStats {
        val withSizes = entries.filter { it.sizes != null }
        val totalBytes = if (withSizes.isEmpty()) null else withSizes.sumOf { it.sizes!!.total }

        val unused = if (usageAvailable) entries.filter { it.isUnused(unusedDays, now) } else emptyList()
        val unusedWithSizes = unused.filter { it.sizes != null }
        val reclaimable = when {
            !usageAvailable -> null
            unused.isEmpty() -> 0L
            unusedWithSizes.isEmpty() -> null
            else -> unusedWithSizes.sumOf { it.sizes!!.total }
        }

        return DashboardStats(
            totalApps = entries.size,
            totalBytes = totalBytes,
            unusedCount = unused.size,
            reclaimableBytes = reclaimable,
            topBySize = entries
                .filter { it.sizes != null }
                .sortedByDescending { it.sizes!!.total }
                .take(topCount),
            topByUsage = entries
                .filter { (it.usage?.foreground7d ?: 0L) > 0L }
                .sortedByDescending { it.usage!!.foreground7d }
                .take(topCount),
            oldestInstalls = entries
                .sortedBy { it.info.firstInstallTime }
                .take(topCount)
        )
    }
}
