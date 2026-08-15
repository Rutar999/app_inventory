package io.github.rutar999.appshelf.ui.applist

import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppFilter
import io.github.rutar999.appshelf.model.SortOrder
import io.github.rutar999.appshelf.util.SearchText

/**
 * 一覧の検索・絞り込み・並び替え（F-02 / F-03 / F-05）。
 *
 * Compose や Android に依存しない純粋な関数にしてあるので、
 * そのまま JUnit で単体テストできる（app/src/test 配下にテストあり）。
 */
object AppListLogic {

    fun apply(
        entries: List<AppEntry>,
        query: String,
        filter: AppFilter,
        sort: SortOrder,
        unusedDays: Int,
        largeThresholdBytes: Long,
        now: Long = System.currentTimeMillis()
    ): List<AppEntry> {
        val filtered = entries.filter { entry ->
            matchesQuery(entry, query) && matchesFilter(entry, filter, unusedDays, largeThresholdBytes, now)
        }
        return filtered.sortedWith(comparator(sort, now))
    }

    private fun matchesQuery(entry: AppEntry, query: String): Boolean {
        if (query.isBlank()) return true
        if (SearchText.matches(entry.info.searchKey, query)) return true
        // タグ名でも引けるようにする
        return entry.tags.any { SearchText.matches(SearchText.normalize(it.name), query) }
    }

    private fun matchesFilter(
        entry: AppEntry,
        filter: AppFilter,
        unusedDays: Int,
        largeThresholdBytes: Long,
        now: Long
    ): Boolean {
        if (filter.favoriteOnly && !entry.meta.isFavorite) return false
        if (filter.unusedOnly && !entry.isUnused(unusedDays, now)) return false
        if (filter.largeOnly && (entry.sizes?.total ?: 0L) < largeThresholdBytes) return false
        if (filter.untaggedOnly && entry.tags.isNotEmpty()) return false
        val tagId = filter.tagId
        if (tagId != null && entry.tags.none { it.id == tagId }) return false
        return true
    }

    /**
     * お気に入りは常に最上部（F-33）。その中で選ばれた並び順を適用する。
     */
    private fun comparator(sort: SortOrder, now: Long): Comparator<AppEntry> {
        val base: Comparator<AppEntry> = when (sort) {
            SortOrder.NAME ->
                compareBy { it.info.searchKey }

            SortOrder.INSTALLED_DESC ->
                compareByDescending { it.info.firstInstallTime }

            SortOrder.UPDATED_DESC ->
                compareByDescending { it.info.lastUpdateTime }

            SortOrder.SIZE_DESC ->
                compareByDescending { it.sizes?.total ?: -1L }

            // 「最終起動が古い順」= 棚卸しで最初に見るべき順。
            // 起動記録なし → 一番古い扱いで先頭に出す。容量未取得のものは末尾へ。
            SortOrder.LAST_USED_ASC ->
                compareBy { entry ->
                    val usage = entry.usage ?: return@compareBy Long.MAX_VALUE
                    if (usage.lastTimeUsed <= 0L) Long.MIN_VALUE else usage.lastTimeUsed
                }

            SortOrder.USAGE_DESC ->
                compareByDescending { it.usage?.foreground30d ?: -1L }
        }
        return compareByDescending<AppEntry> { it.meta.isFavorite }
            .then(base)
            .thenBy { it.info.searchKey }
    }
}
