package io.github.rutar999.appshelf.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import io.github.rutar999.appshelf.model.AppInfo
import io.github.rutar999.appshelf.model.AppPermission
import io.github.rutar999.appshelf.model.PermissionGroup
import io.github.rutar999.appshelf.util.SearchText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * インストール済みアプリの一覧を PackageManager から取得する（要件定義書 §2.1）。
 *
 * AndroidManifest の <queries> でランチャー Intent を宣言しているため、
 * ここで見えるのは「ホーム画面に出るアプリ」に限られる。
 * QUERY_ALL_PACKAGES は Play 審査のリスクが高いので使わない。
 */
class PackageScanner(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    suspend fun scan(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageNames = queryLauncherActivities(launcherIntent).toMutableSet()
        // 自分自身も一覧に出す（ユーザーが「これは何だ」と思わないように）
        packageNames.add(context.packageName)

        packageNames.mapNotNull { buildAppInfo(it) }.sortedBy { it.searchKey }
    }

    private fun queryLauncherActivities(intent: Intent): List<String> {
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.map { it.activityInfo.packageName }
    }

    private fun buildAppInfo(packageName: String): AppInfo? = try {
        val info = getPackageInfo(packageName)
        val appInfo = info.applicationInfo
        if (appInfo == null) {
            null
        } else {
            val label = pm.getApplicationLabel(appInfo).toString()
            AppInfo(
                packageName = packageName,
                label = label,
                searchKey = SearchText.normalize("$label $packageName"),
                versionName = info.versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                categoryTitle = ApplicationInfo.getCategoryTitle(context, appInfo.category)?.toString(),
                permissions = buildPermissions(info)
            )
        }
    } catch (_: PackageManager.NameNotFoundException) {
        // スキャン中にアンインストールされた場合など。一覧から落とすだけでよい。
        null
    } catch (_: RuntimeException) {
        // 巨大な PackageInfo を返すアプリで TransactionTooLargeException 系が出ることがある
        null
    }

    private fun getPackageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    /**
     * 宣言権限と「実際に付与されているか」を取り出す（要件定義書 §2.4）。
     * requestedPermissionsFlags の REQUESTED_PERMISSION_GRANTED ビットが立っていれば付与済み。
     */
    @Suppress("DEPRECATION")
    private fun buildPermissions(info: PackageInfo): List<AppPermission> {
        val names = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags
        return names.mapIndexed { index, name ->
            val granted = flags != null &&
                index < flags.size &&
                flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            AppPermission(
                name = name,
                granted = granted,
                group = PermissionGroup.of(name)
            )
        }
    }

    /**
     * 権限の人間向けラベル（例: android.permission.CAMERA → 「カメラ」）。
     * 端末の言語で返る。取得できなければ権限名の末尾を返す。
     */
    fun permissionLabel(permissionName: String): String = try {
        // getPackageInfo などと違い、getPermissionInfo は API 33 でも int 引数のまま。
        // PermissionInfoFlags というクラスは存在しないので、バージョン分岐は不要。
        pm.getPermissionInfo(permissionName, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        permissionName.substringAfterLast('.')
    }
}
