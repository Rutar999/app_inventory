package io.github.rutar999.appshelf.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.rutar999.appshelf.model.SortOrder
import io.github.rutar999.appshelf.model.ThemeMode
import io.github.rutar999.appshelf.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** アプリ設定のまとまり。 */
data class UserSettings(
    val onboardingDone: Boolean = false,
    /** 未使用と判定する日数（要件定義書 §10-2 の既定値は 90 日にした） */
    val unusedDays: Int = SettingsRepository.DEFAULT_UNUSED_DAYS,
    /** 「大容量」と判定するしきい値（MB） */
    val largeThresholdMb: Int = SettingsRepository.DEFAULT_LARGE_MB,
    val showSystemApps: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NAME,
    val viewMode: ViewMode = ViewMode.LIST,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** 最後に日次スナップショットを保存した日付（yyyyMMdd） */
    val lastSnapshotDate: Int = 0
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            unusedDays = prefs[Keys.UNUSED_DAYS] ?: DEFAULT_UNUSED_DAYS,
            largeThresholdMb = prefs[Keys.LARGE_MB] ?: DEFAULT_LARGE_MB,
            showSystemApps = prefs[Keys.SHOW_SYSTEM] ?: false,
            sortOrder = SortOrder.fromName(prefs[Keys.SORT_ORDER]),
            viewMode = ViewMode.fromName(prefs[Keys.VIEW_MODE]),
            themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            lastSnapshotDate = prefs[Keys.LAST_SNAPSHOT_DATE] ?: 0
        )
    }

    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING_DONE] = done }
    suspend fun setUnusedDays(days: Int) = edit { it[Keys.UNUSED_DAYS] = days }
    suspend fun setLargeThresholdMb(mb: Int) = edit { it[Keys.LARGE_MB] = mb }
    suspend fun setShowSystemApps(show: Boolean) = edit { it[Keys.SHOW_SYSTEM] = show }
    suspend fun setSortOrder(order: SortOrder) = edit { it[Keys.SORT_ORDER] = order.name }
    suspend fun setViewMode(mode: ViewMode) = edit { it[Keys.VIEW_MODE] = mode.name }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setLastSnapshotDate(date: Int) = edit { it[Keys.LAST_SNAPSHOT_DATE] = date }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val UNUSED_DAYS = intPreferencesKey("unused_days")
        val LARGE_MB = intPreferencesKey("large_threshold_mb")
        val SHOW_SYSTEM = booleanPreferencesKey("show_system_apps")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_SNAPSHOT_DATE = intPreferencesKey("last_snapshot_date")
    }

    companion object {
        const val DEFAULT_UNUSED_DAYS = 90
        const val DEFAULT_LARGE_MB = 200
        val UNUSED_DAY_CHOICES = listOf(30, 90, 180, 365)
        val LARGE_MB_CHOICES = listOf(100, 200, 500, 1000)
    }
}
