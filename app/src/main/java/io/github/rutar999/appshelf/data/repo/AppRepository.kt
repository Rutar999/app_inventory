package io.github.rutar999.appshelf.data.repo

import io.github.rutar999.appshelf.data.db.AppMetaDao
import io.github.rutar999.appshelf.data.db.AppMetaEntity
import io.github.rutar999.appshelf.data.db.AppTagCrossRef
import io.github.rutar999.appshelf.data.db.TagDao
import io.github.rutar999.appshelf.data.db.TagEntity
import io.github.rutar999.appshelf.data.db.UsageSnapshotDao
import io.github.rutar999.appshelf.data.db.UsageSnapshotEntity
import io.github.rutar999.appshelf.data.prefs.SettingsRepository
import io.github.rutar999.appshelf.data.system.PackageScanner
import io.github.rutar999.appshelf.data.system.StorageStatsSource
import io.github.rutar999.appshelf.data.system.UsageStatsSource
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppInfo
import io.github.rutar999.appshelf.model.AppMeta
import io.github.rutar999.appshelf.model.AppSizes
import io.github.rutar999.appshelf.model.AppUsage
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.model.TrendPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * アプリ情報の単一の窓口。
 *
 * 設計の要点:
 * - OS から取る情報（一覧・使用状況・容量）は StateFlow に持ち、DB の情報と combine して UI に流す
 * - 重い処理（容量取得）は取れたぶんから順に流し込むので、一覧は先に描ける
 * - 使用状況アクセスが未許可でも、usage / sizes が null になるだけでアプリは成立する
 */
class AppRepository(
    private val scanner: PackageScanner,
    private val usageSource: UsageStatsSource,
    private val storageSource: StorageStatsSource,
    private val appMetaDao: AppMetaDao,
    private val tagDao: TagDao,
    private val snapshotDao: UsageSnapshotDao,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope
) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _sizes = MutableStateFlow<Map<String, AppSizes>>(emptyMap())
    private val _usage = MutableStateFlow<Map<String, AppUsage>>(emptyMap())
    private val _isLoading = MutableStateFlow(true)
    private val _isEnriching = MutableStateFlow(false)
    private val _usageAccessGranted = MutableStateFlow(false)

    /** 一覧の初回読み込み中か（アプリ名とアイコンがまだ出ていない状態） */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 容量など時間のかかる情報を裏で取得中か */
    val isEnriching: StateFlow<Boolean> = _isEnriching.asStateFlow()

    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    val tags: Flow<List<Tag>> = tagDao.observeTags().map { list -> list.map { it.toDomain() } }

    private val tagsByPackage: Flow<Map<String, List<Tag>>> =
        combine(tagDao.observeTags(), tagDao.observeCrossRefs()) { tags, refs ->
            val byId = tags.associate { it.id to it.toDomain() }
            refs.groupBy { it.packageName }
                .mapValues { (_, list) -> list.mapNotNull { byId[it.tagId] } }
        }

    private val metaByPackage: Flow<Map<String, AppMeta>> =
        appMetaDao.observeAll().map { list -> list.associate { it.packageName to it.toDomain() } }

    /** 画面に流す一覧。OS 由来の情報とユーザー由来の情報を結合したもの。 */
    val entries: Flow<List<AppEntry>> =
        combine(_apps, _sizes, _usage, metaByPackage, tagsByPackage) { apps, sizes, usage, meta, tags ->
            apps.map { info ->
                AppEntry(
                    info = info,
                    sizes = sizes[info.packageName],
                    usage = usage[info.packageName],
                    meta = meta[info.packageName] ?: AppMeta(info.packageName),
                    tags = tags[info.packageName].orEmpty()
                )
            }
        }

    private var refreshJob: Job? = null

    /** 一覧を読み直す。多重実行は抑止する。 */
    fun refresh(force: Boolean = false) {
        if (!force && refreshJob?.isActive == true) return
        refreshJob?.cancel()
        refreshJob = scope.launch { performRefresh() }
    }

    private suspend fun performRefresh() {
        val apps = scanner.scan()
        _apps.value = apps
        _isLoading.value = false

        val now = System.currentTimeMillis()
        appMetaDao.insertIfAbsent(
            apps.map { AppMetaEntity(packageName = it.packageName, firstSeenAt = now, updatedAt = now) }
        )
        pruneRemovedPackages(apps.map { it.packageName })

        val granted = usageSource.hasAccess()
        _usageAccessGranted.value = granted
        if (!granted) {
            _usage.value = emptyMap()
            _sizes.value = emptyMap()
            return
        }

        _isEnriching.value = true
        try {
            _usage.value = loadUsageMergedWithHistory()
            storageSource.query(apps.map { it.packageName }) { chunk ->
                _sizes.update { current -> current + chunk }
            }
            saveDailySnapshotIfNeeded()
        } finally {
            _isEnriching.value = false
        }
    }

    /**
     * OS から取れる使用状況と、自前で貯めたスナップショットを突き合わせる。
     * OS 側は保持期間を過ぎると値を返さなくなるため、より古い記録はこちらが持っている（要件定義書 §2.2）。
     */
    private suspend fun loadUsageMergedWithHistory(): Map<String, AppUsage> {
        val fromOs = usageSource.query()
        val stored = runCatching { snapshotDao.lastUsedByPackage() }.getOrDefault(emptyList())
            .associate { it.packageName to it.lastTimeUsed }
        if (stored.isEmpty()) return fromOs

        val packages = fromOs.keys + stored.keys
        return packages.associateWith { pkg ->
            val os = fromOs[pkg]
            val storedLast = stored[pkg] ?: 0L
            AppUsage(
                lastTimeUsed = maxOf(os?.lastTimeUsed ?: 0L, storedLast),
                foreground7d = os?.foreground7d ?: 0L,
                foreground30d = os?.foreground30d ?: 0L,
                foregroundTotal = os?.foregroundTotal ?: 0L
            )
        }
    }

    /**
     * 1日1回だけ、その日の使用状況と容量を保存する（F-16 の土台）。
     * WorkManager を使わず「アプリを開いたときに保存」する簡易方式。
     * 依存を増やさない代わりに、アプリを開かない日は記録が飛ぶ。
     */
    private suspend fun saveDailySnapshotIfNeeded() {
        val today = todayAsInt()
        if (settings.settings.first().lastSnapshotDate == today) return

        val usage = _usage.value
        val sizes = _sizes.value
        val rows = _apps.value.mapNotNull { info ->
            val u = usage[info.packageName]
            val s = sizes[info.packageName]
            if (u == null && s == null) return@mapNotNull null
            UsageSnapshotEntity(
                packageName = info.packageName,
                date = today,
                // 直近30日の使用時間を記録する。
                // 365日ローリング合計（foregroundTotal）だと、日付順に並べても
                // ほぼ単調増加するだけで「最近使わなくなった」が読み取れない。
                // 30日窓なら、値が下がっていく＝使わなくなった、と素直に読める。
                foregroundMs = u?.foreground30d ?: 0L,
                lastTimeUsed = u?.lastTimeUsed ?: 0L,
                appBytes = s?.appBytes ?: 0L,
                dataBytes = s?.dataBytes ?: 0L,
                cacheBytes = s?.cacheBytes ?: 0L
            )
        }
        if (rows.isEmpty()) return

        snapshotDao.upsertAll(rows)
        snapshotDao.deleteOlderThan(twoYearsAgoAsInt())
        settings.setLastSnapshotDate(today)
    }

    /**
     * アンインストール済みパッケージの行を掃除する。
     * SQLite の変数上限（約999）を超えると失敗するため、多すぎる場合は見送る。
     */
    private suspend fun pruneRemovedPackages(existing: List<String>) {
        if (existing.isEmpty() || existing.size > 900) return
        runCatching {
            appMetaDao.deleteMissing(existing)
            tagDao.deleteMissing(existing)
        }
    }

    // ---- ユーザー操作 -------------------------------------------------------

    suspend fun setFavorite(packageName: String, value: Boolean) =
        updateMeta(packageName) { it.copy(isFavorite = value) }

    suspend fun setHidden(packageName: String, value: Boolean) =
        updateMeta(packageName) { it.copy(isHidden = value) }

    suspend fun setHidden(packageNames: List<String>, value: Boolean) {
        val now = System.currentTimeMillis()
        appMetaDao.insertIfAbsent(
            packageNames.map { AppMetaEntity(packageName = it, firstSeenAt = now, updatedAt = now) }
        )
        appMetaDao.setHidden(packageNames, value, now)
    }

    suspend fun setNote(packageName: String, note: String?) =
        updateMeta(packageName) { it.copy(note = note?.takeIf { text -> text.isNotBlank() }) }

    private suspend fun updateMeta(
        packageName: String,
        transform: (AppMetaEntity) -> AppMetaEntity
    ) {
        val now = System.currentTimeMillis()
        val current = appMetaDao.find(packageName)
            ?: AppMetaEntity(packageName = packageName, firstSeenAt = now, updatedAt = now)
        appMetaDao.upsert(transform(current).copy(updatedAt = now))
    }

    // ---- タグ ---------------------------------------------------------------

    /** @return 作成できたら id、同名タグがあれば null */
    suspend fun createTag(name: String, colorArgb: Int): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (tagDao.findByName(trimmed) != null) return null
        return runCatching {
            tagDao.insert(TagEntity(name = trimmed, colorArgb = colorArgb))
        }.getOrNull()
    }

    suspend fun updateTag(tag: Tag): Boolean {
        val trimmed = tag.name.trim()
        if (trimmed.isEmpty()) return false
        val duplicate = tagDao.findByName(trimmed)
        if (duplicate != null && duplicate.id != tag.id) return false
        tagDao.update(
            TagEntity(
                id = tag.id,
                name = trimmed,
                colorArgb = tag.colorArgb,
                sortOrder = tag.sortOrder
            )
        )
        return true
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.delete(TagEntity(tag.id, tag.name, tag.colorArgb, tag.sortOrder))
    }

    suspend fun assignTag(packageNames: List<String>, tagId: Long) {
        tagDao.addCrossRefs(packageNames.map { AppTagCrossRef(it, tagId) })
    }

    suspend fun unassignTag(packageNames: List<String>, tagId: Long) {
        tagDao.removeCrossRefs(packageNames, tagId)
    }

    // ---- ユーティリティ -----------------------------------------------------

    /**
     * 長期トレンド（F-16）。日次スナップショットを日付順に返す。
     * 記録が 1 件以下ならグラフにならないので、呼び出し側で件数を見て扱いを分けること。
     */
    suspend fun trendFor(packageName: String): List<TrendPoint> =
        runCatching {
            snapshotDao.forPackage(packageName).map { row ->
                TrendPoint(
                    date = row.date,
                    // AppSizes.total と同じ定義（dataBytes はキャッシュを含むので cache は足さない）
                    totalBytes = row.appBytes + row.dataBytes,
                    foregroundMs = row.foregroundMs
                )
            }
        }.getOrDefault(emptyList())

    fun permissionLabel(permissionName: String): String = scanner.permissionLabel(permissionName)

    fun usageAccessIntent() = usageSource.settingsIntent()

    /** Activity の onResume 等から呼んで、許可状態の変化を拾う。 */
    fun refreshUsageAccessState(): Boolean {
        val granted = usageSource.hasAccess()
        val changed = granted != _usageAccessGranted.value
        _usageAccessGranted.value = granted
        return changed
    }

    private fun todayAsInt(): Int = LocalDate.now().toDateInt()

    private fun twoYearsAgoAsInt(): Int = LocalDate.now().minusYears(2).toDateInt()

    private fun LocalDate.toDateInt(): Int = year * 10000 + monthValue * 100 + dayOfMonth
}

private fun TagEntity.toDomain() = Tag(id, name, colorArgb, sortOrder)

private fun AppMetaEntity.toDomain() = AppMeta(packageName, isFavorite, isHidden, note, firstSeenAt)
