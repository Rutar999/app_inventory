package io.github.rutar999.appshelf.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

/**
 * 他アプリ・システム画面への遷移をまとめたもの。
 *
 * このアプリからできるのは「案内する」ところまで。
 * 他アプリの権限を剥奪したり、確認なしで削除したりすることは Android の仕様上できない（要件定義書 §2.4 / §2.5）。
 */
object AppIntents {

    /** アプリを起動する。起動できなければ false。 */
    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** そのアプリの「アプリ情報」画面。ここから権限設定にも入れる。 */
    fun appSettings(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Play ストアのアプリページ。
     * market:// が開けない端末（Play 非搭載など）では https にフォールバックする。
     */
    fun openPlayStore(context: Context, packageName: String): Boolean {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(market)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(web)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    /**
     * アンインストール用 Intent。
     *
     * PackageInstaller.uninstall() でも実装できるが、コールバック用の PendingIntent が必要で
     * 手順が増える。ACTION_DELETE + EXTRA_RETURN_RESULT なら
     * rememberLauncherForActivityResult で結果を受け取れて、順次処理フローが素直に書ける。
     * どちらにせよ OS の確認ダイアログは必ず出る（サイレント削除は不可能）。
     */
    fun uninstall(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)

    /** Android 13 以降の「アプリの言語」設定 */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun appLocaleSettings(packageName: String): Intent =
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
