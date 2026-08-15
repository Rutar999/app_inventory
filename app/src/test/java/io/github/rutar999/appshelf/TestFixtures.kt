package io.github.rutar999.appshelf

import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppInfo
import io.github.rutar999.appshelf.model.AppMeta
import io.github.rutar999.appshelf.model.AppSizes
import io.github.rutar999.appshelf.model.AppUsage
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.util.SearchText

/** テスト用に AppEntry を手軽に組み立てるヘルパ。 */
object TestFixtures {

    const val DAY = 24L * 60 * 60 * 1000

    fun entry(
        packageName: String = "com.example.app",
        label: String = "Example",
        firstInstallTime: Long = 0L,
        lastUpdateTime: Long = 0L,
        isSystemApp: Boolean = false,
        sizes: AppSizes? = null,
        usage: AppUsage? = null,
        isFavorite: Boolean = false,
        isHidden: Boolean = false,
        tags: List<Tag> = emptyList()
    ): AppEntry = AppEntry(
        info = AppInfo(
            packageName = packageName,
            label = label,
            searchKey = SearchText.normalize("$label $packageName"),
            versionName = "1.0",
            versionCode = 1L,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            isSystemApp = isSystemApp,
            isUpdatedSystemApp = false,
            categoryTitle = null,
            permissions = emptyList()
        ),
        sizes = sizes,
        usage = usage,
        meta = AppMeta(
            packageName = packageName,
            isFavorite = isFavorite,
            isHidden = isHidden
        ),
        tags = tags
    )

    fun sizes(appMb: Long, dataMb: Long = 0, cacheMb: Long = 0) = AppSizes(
        appBytes = appMb * 1024 * 1024,
        dataBytes = dataMb * 1024 * 1024,
        cacheBytes = cacheMb * 1024 * 1024
    )

    fun usedDaysAgo(days: Int, now: Long, foreground7d: Long = 0L, foreground30d: Long = 0L) = AppUsage(
        lastTimeUsed = now - days * DAY,
        foreground7d = foreground7d,
        foreground30d = foreground30d,
        foregroundTotal = foreground30d
    )

    val neverUsed = AppUsage(
        lastTimeUsed = 0L,
        foreground7d = 0L,
        foreground30d = 0L,
        foregroundTotal = 0L
    )
}
