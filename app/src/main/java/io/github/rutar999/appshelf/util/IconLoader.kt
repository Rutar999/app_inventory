package io.github.rutar999.appshelf.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * アプリアイコンの読み込みとメモリキャッシュ（非機能要件: 遅延読み込み＋メモリキャッシュ）。
 *
 * 画像ライブラリ（Coil 等）を入れてもよいが、
 * ここで扱うのは URL ではなく PackageManager が返す Drawable なので、
 * 依存を増やさず自前で持つほうが単純になる。
 */
class IconLoader(private val context: Context) {

    private val sizePx: Int = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {}

    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) cache.put(packageName, bitmap)
        return bitmap
    }

    fun evict(packageName: String) {
        cache.remove(packageName)
    }

    /** アンインストール直後などにまとめて捨てる */
    fun clear() = cache.evictAll()

    private companion object {
        /** アプリ 300 本ぶんのアイコンでも 48dp なら数 MB に収まる */
        const val MAX_ENTRIES = 400
    }
}

/** PackageManager が例外を投げるケースを吸収するヘルパ */
internal fun PackageManager.isInstalled(packageName: String): Boolean = try {
    getApplicationInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
