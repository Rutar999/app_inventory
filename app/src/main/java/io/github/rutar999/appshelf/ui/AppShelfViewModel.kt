package io.github.rutar999.appshelf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.rutar999.appshelf.AppShelfApp
import io.github.rutar999.appshelf.R
import io.github.rutar999.appshelf.data.prefs.SettingsRepository
import io.github.rutar999.appshelf.data.prefs.UserSettings
import io.github.rutar999.appshelf.data.repo.AppRepository
import io.github.rutar999.appshelf.model.AppEntry
import io.github.rutar999.appshelf.model.AppFilter
import io.github.rutar999.appshelf.model.SortOrder
import io.github.rutar999.appshelf.model.Tag
import io.github.rutar999.appshelf.model.ThemeMode
import io.github.rutar999.appshelf.model.TrendPoint
import io.github.rutar999.appshelf.model.ViewMode
import io.github.rutar999.appshelf.util.IconLoader
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 画面をまたいで共有する ViewModel。
 *
 * なぜ画面ごとに分けないのか:
 * どの画面も同じ「アプリ一覧 + 設定」を見ており、しかも複数選択やアンインストール待ち行列は
 * 画面遷移をまたいで残る必要がある。v1 の規模なら 1 つにまとめたほうが
 * 状態の流れを追いやすく、同じ Flow を何度も組み直す無駄も避けられる。
 */
class AppShelfViewModel(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository,
    val iconLoader: IconLoader
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, started, UserSettings())

    /**
     * DataStore の読み込みは非同期なので、最初の 1 フレームは既定値が流れてくる。
     * これを見て「オンボーディング未完了」と誤判定しないよう、読み込み完了を別に持つ。
     */
    val settingsLoaded: StateFlow<Boolean> =
        settingsRepository.settings.map { true }.stateIn(viewModelScope, started, false)

    /** 非表示のものも含む全アプリ */
    val allEntries: StateFlow<List<AppEntry>> =
        repository.entries.stateIn(viewModelScope, started, emptyList())

    /** 一覧・ダッシュボードで使う、非表示とシステムアプリ設定を反映済みの一覧 */
    val visibleEntries: StateFlow<List<AppEntry>> =
        combine(repository.entries, settingsRepository.settings) { entries, config ->
            entries.filter { entry ->
                !entry.meta.isHidden && (config.showSystemApps || !entry.info.isSystemApp)
            }
        }.stateIn(viewModelScope, started, emptyList())

    val tags: StateFlow<List<Tag>> =
        repository.tags.stateIn(viewModelScope, started, emptyList())

    val isLoading: StateFlow<Boolean> = repository.isLoading
    val isEnriching: StateFlow<Boolean> = repository.isEnriching
    val usageAccessGranted: StateFlow<Boolean> = repository.usageAccessGranted

    // ---- 一覧の絞り込み条件（画面遷移しても保つ） -----------------------------

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(AppFilter())
    val filter: StateFlow<AppFilter> = _filter.asStateFlow()

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setFilter(value: AppFilter) {
        _filter.value = value
    }

    fun clearFilter() {
        _filter.value = AppFilter()
        _searchQuery.value = ""
    }

    /** ホームの「棚卸しを始める」から未使用アプリだけを表示する導線 */
    fun showUnusedOnly() {
        _searchQuery.value = ""
        _filter.value = AppFilter(unusedOnly = true)
    }

    // ---- 複数選択モード -----------------------------------------------------

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    fun toggleSelection(packageName: String) {
        _selection.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun selectAll(packageNames: List<String>) {
        _selection.value = packageNames.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    // ---- アンインストール（順次実行）----------------------------------------

    /**
     * Android では確認なし・一括のアンインストールができない（要件定義書 §2.5）。
     * そこで「1本ずつ OS のダイアログを出す」流れを待ち行列として持つ。
     */
    data class UninstallSession(
        val queue: List<String>,
        val index: Int = 0,
        val removed: Int = 0
    ) {
        val current: String? get() = queue.getOrNull(index)
        val isFinished: Boolean get() = index >= queue.size
    }

    private val _uninstallSession = MutableStateFlow<UninstallSession?>(null)
    val uninstallSession: StateFlow<UninstallSession?> = _uninstallSession.asStateFlow()

    fun startUninstall(packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        _uninstallSession.value = UninstallSession(packageNames)
    }

    /** OS のダイアログから戻ってきたときに呼ぶ。 */
    fun onUninstallResult(removed: Boolean) {
        val session = _uninstallSession.value ?: return
        if (removed) session.current?.let { iconLoader.evict(it) }
        _uninstallSession.value = session.copy(
            index = session.index + 1,
            removed = session.removed + if (removed) 1 else 0
        )
        repository.refresh(force = true)
    }

    fun finishUninstall() {
        _uninstallSession.value = null
        clearSelection()
    }

    // ---- ユーザー操作 -------------------------------------------------------

    fun refresh() = repository.refresh()

    /** Activity の onResume から。設定画面で権限を ON にして戻ってきたケースを拾う。 */
    fun onAppResumed() {
        if (repository.refreshUsageAccessState()) repository.refresh(force = true)
    }

    fun setFavorite(packageName: String, value: Boolean) = viewModelScope.launch {
        repository.setFavorite(packageName, value)
    }

    fun setHidden(packageName: String, value: Boolean) = viewModelScope.launch {
        repository.setHidden(packageName, value)
    }

    fun setHidden(packageNames: List<String>, value: Boolean) = viewModelScope.launch {
        repository.setHidden(packageNames, value)
        clearSelection()
    }

    fun setNote(packageName: String, note: String?) = viewModelScope.launch {
        repository.setNote(packageName, note)
    }

    fun createTag(name: String, colorArgb: Int) = viewModelScope.launch {
        if (repository.createTag(name, colorArgb) == null) emit(R.string.tags_duplicate)
    }

    fun updateTag(tag: Tag) = viewModelScope.launch {
        if (!repository.updateTag(tag)) emit(R.string.tags_duplicate)
    }

    fun deleteTag(tag: Tag) = viewModelScope.launch { repository.deleteTag(tag) }

    fun assignTag(packageNames: List<String>, tagId: Long) = viewModelScope.launch {
        repository.assignTag(packageNames, tagId)
    }

    fun unassignTag(packageNames: List<String>, tagId: Long) = viewModelScope.launch {
        repository.unassignTag(packageNames, tagId)
    }

    // ---- 設定 ---------------------------------------------------------------

    fun setSortOrder(order: SortOrder) = viewModelScope.launch { settingsRepository.setSortOrder(order) }
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { settingsRepository.setViewMode(mode) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setUnusedDays(days: Int) = viewModelScope.launch { settingsRepository.setUnusedDays(days) }
    fun setLargeThresholdMb(mb: Int) = viewModelScope.launch { settingsRepository.setLargeThresholdMb(mb) }
    fun setShowSystemApps(show: Boolean) = viewModelScope.launch { settingsRepository.setShowSystemApps(show) }
    fun completeOnboarding() = viewModelScope.launch { settingsRepository.setOnboardingDone(true) }

    /** 長期トレンド（F-16）。詳細画面を開いたときに 1 回だけ読む。 */
    suspend fun loadTrend(packageName: String): List<TrendPoint> = repository.trendFor(packageName)

    fun permissionLabel(permissionName: String) = repository.permissionLabel(permissionName)

    fun usageAccessIntent() = repository.usageAccessIntent()

    // ---- 画面へのメッセージ（スナックバー）-----------------------------------

    private val _messages = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<Int> = _messages

    fun emit(messageRes: Int) {
        _messages.tryEmit(messageRes)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppShelfApp
                AppShelfViewModel(
                    repository = app.container.appRepository,
                    settingsRepository = app.container.settingsRepository,
                    iconLoader = app.container.iconLoader
                )
            }
        }
    }
}
