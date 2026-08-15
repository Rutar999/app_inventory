package io.github.rutar999.appshelf.data.system

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import io.github.rutar999.appshelf.model.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 使用状況（最終起動日・前面表示時間）の取得（要件定義書 §2.2）。
 *
 * PACKAGE_USAGE_STATS は「特殊権限」で、通常の権限ダイアログでは付与できない。
 * ユーザーが 設定 > アプリ > 特別なアプリアクセス > 使用状況へのアクセス で ON にする必要がある。
 * 未許可でもアプリが成立するよう、呼び出し側は null を許容すること。
 */
class UsageStatsSource(private val context: Context) {

    /**
     * 使用状況へのアクセスが許可されているか。
     *
     * unsafeCheckOpNoThrow は新しい API レベルで非推奨になっているが、
     * 「自アプリに特殊権限が付与されているか」を調べる公開された代替手段は今のところ無い。
     * 非推奨のまま動作するため、警告を抑止して使い続ける。
     */
    @Suppress("DEPRECATION")
    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        // MODE_DEFAULT のときは権限そのものの付与状態で判断する
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkPermission(
                Manifest.permission.PACKAGE_USAGE_STATS,
                Process.myPid(),
                Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** 「使用状況へのアクセス」設定画面を開く Intent。 */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * パッケージ名 → 使用状況 の対応表を作る。
     *
     * OS 側の保持期間には限りがある（日次 約7日 / 週次 約4週 / 月次 約6ヶ月 / 年次 約2年）。
     * ここでは 1 年ぶんを集計して最終起動日を取り、
     * それより古い期間は自前の UsageSnapshot（Room）で補う設計にしている。
     */
    suspend fun query(): Map<String, AppUsage> = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext emptyMap()
        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: return@withContext emptyMap()

        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        val long = runCatching { manager.queryAndAggregateUsageStats(now - 365 * day, now) }
            .getOrDefault(emptyMap())
        val last30 = runCatching { manager.queryAndAggregateUsageStats(now - 30 * day, now) }
            .getOrDefault(emptyMap())
        val last7 = runCatching { manager.queryAndAggregateUsageStats(now - 7 * day, now) }
            .getOrDefault(emptyMap())

        val packages = long.keys + last30.keys + last7.keys
        packages.associateWith { pkg ->
            AppUsage(
                lastTimeUsed = long[pkg]?.lastTimeUsed ?: 0L,
                foreground7d = last7[pkg]?.totalTimeInForeground ?: 0L,
                foreground30d = last30[pkg]?.totalTimeInForeground ?: 0L,
                foregroundTotal = long[pkg]?.totalTimeInForeground ?: 0L
            )
        }
    }
}
