package io.github.rutar999.appshelf.di

import android.content.Context
import io.github.rutar999.appshelf.data.db.AppShelfDatabase
import io.github.rutar999.appshelf.data.prefs.SettingsRepository
import io.github.rutar999.appshelf.data.repo.AppRepository
import io.github.rutar999.appshelf.data.system.PackageScanner
import io.github.rutar999.appshelf.data.system.StorageStatsSource
import io.github.rutar999.appshelf.data.system.UsageStatsSource
import io.github.rutar999.appshelf.util.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 手動 DI（要件定義書 §7: v1 では Hilt を使わない）。
 *
 * Application が 1 つだけ持ち、ViewModel はここから依存を受け取る。
 * 「どこで何が作られているか」が 1 ファイルで追える状態を優先している。
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** 画面のライフサイクルより長生きさせたい処理（一覧のバックグラウンド更新）用 */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppShelfDatabase = AppShelfDatabase.create(appContext)

    val settingsRepository = SettingsRepository(appContext)

    val iconLoader = IconLoader(appContext)

    val appRepository = AppRepository(
        scanner = PackageScanner(appContext),
        usageSource = UsageStatsSource(appContext),
        storageSource = StorageStatsSource(appContext),
        appMetaDao = database.appMetaDao(),
        tagDao = database.tagDao(),
        snapshotDao = database.usageSnapshotDao(),
        settings = settingsRepository,
        scope = applicationScope
    )
}
