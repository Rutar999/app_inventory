package io.github.rutar999.appshelf.data.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import io.github.rutar999.appshelf.model.AppSizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * アプリ容量の取得（要件定義書 §2.3）。
 *
 * 使用状況へのアクセス（PACKAGE_USAGE_STATS）が必要。
 * 全アプリぶんをループで取ると数百ms〜数秒かかるため、
 * 少しずつ [onChunk] で返して UI を先に描けるようにしている（非機能要件: 画面をブロックしない）。
 */
class StorageStatsSource(private val context: Context) {

    suspend fun query(
        packageNames: List<String>,
        chunkSize: Int = 20,
        onChunk: suspend (Map<String, AppSizes>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val statsManager = context.getSystemService(StorageStatsManager::class.java) ?: return@withContext
        val packageManager = context.packageManager
        val user = Process.myUserHandle()

        packageNames.chunked(chunkSize).forEach { chunk ->
            coroutineContext.ensureActive()
            val result = HashMap<String, AppSizes>(chunk.size)
            for (packageName in chunk) {
                val sizes = runCatching {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getApplicationInfo(
                            packageName,
                            PackageManager.ApplicationInfoFlags.of(0L)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getApplicationInfo(packageName, 0)
                    }
                    val stats = statsManager.queryStatsForPackage(
                        appInfo.storageUuid,
                        packageName,
                        user
                    )
                    AppSizes(
                        appBytes = stats.appBytes,
                        dataBytes = stats.dataBytes,
                        cacheBytes = stats.cacheBytes
                    )
                }.getOrNull()
                // SecurityException（未許可）や NameNotFoundException は単に飛ばす。
                // 取れなかったアプリは UI 側で「—」表示になる。
                if (sizes != null) result[packageName] = sizes
            }
            if (result.isNotEmpty()) onChunk(result)
        }
    }
}
