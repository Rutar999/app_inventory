package io.github.rutar999.appshelf.util

import io.github.rutar999.appshelf.model.AppEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 一覧の書き出し（F-37）。
 *
 * JSON は Android 標準の org.json を使う。
 * kotlinx-serialization を入れてもよいが、この用途では依存を増やす価値が薄い。
 */
object Exporter {

    private val isoDate: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())

    private val COLUMNS = listOf(
        "packageName", "label", "versionName", "versionCode",
        "installedAt", "updatedAt", "isSystemApp",
        "appBytes", "dataBytes", "cacheBytes", "totalBytes",
        "lastUsedAt", "foreground7dMs", "foreground30dMs",
        "isFavorite", "isHidden", "tags", "grantedSensitivePermissions", "note"
    )

    /**
     * @param note ファイル冒頭に入れる注記。
     *   この書き出しは画面の絞り込み（システムアプリを隠す等）を反映せず**全アプリ**を対象にするため、
     *   件数が画面表示と食い違う。誤解を防ぐために明示する。
     *   CSV に公式のコメント構文は無いが、`#` 始まりの行を無視する慣習が広く使われている。
     *   表計算ソフトで開くと 1 行目に見えるが、それでも黙って件数がずれるより良い。
     */
    fun toCsv(entries: List<AppEntry>, note: String): String = buildString {
        appendLine("# $note")
        appendLine(COLUMNS.joinToString(","))
        entries.forEach { entry ->
            appendLine(
                listOf(
                    entry.packageName,
                    entry.info.label,
                    entry.info.versionName.orEmpty(),
                    entry.info.versionCode.toString(),
                    formatDate(entry.info.firstInstallTime),
                    formatDate(entry.info.lastUpdateTime),
                    entry.info.isSystemApp.toString(),
                    entry.sizes?.appBytes?.toString().orEmpty(),
                    entry.sizes?.dataBytes?.toString().orEmpty(),
                    entry.sizes?.cacheBytes?.toString().orEmpty(),
                    entry.sizes?.total?.toString().orEmpty(),
                    entry.usage?.lastTimeUsed?.takeIf { it > 0L }?.let { formatDate(it) }.orEmpty(),
                    entry.usage?.foreground7d?.toString().orEmpty(),
                    entry.usage?.foreground30d?.toString().orEmpty(),
                    entry.meta.isFavorite.toString(),
                    entry.meta.isHidden.toString(),
                    entry.tags.joinToString("|") { it.name },
                    entry.info.grantedGroups.joinToString("|") { it.name },
                    entry.meta.note.orEmpty()
                ).joinToString(",") { escapeCsv(it) }
            )
        }
    }

    /** JSON にはコメント構文が無いので、注記は `note` フィールドとして持たせる。 */
    fun toJson(entries: List<AppEntry>, note: String): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("packageName", entry.packageName)
            obj.put("label", entry.info.label)
            obj.put("versionName", entry.info.versionName ?: JSONObject.NULL)
            obj.put("versionCode", entry.info.versionCode)
            obj.put("installedAt", formatDate(entry.info.firstInstallTime))
            obj.put("updatedAt", formatDate(entry.info.lastUpdateTime))
            obj.put("isSystemApp", entry.info.isSystemApp)
            obj.put("canUninstall", entry.info.canUninstall)

            entry.sizes?.let { sizes ->
                obj.put(
                    "sizes",
                    JSONObject()
                        .put("appBytes", sizes.appBytes)
                        .put("dataBytes", sizes.dataBytes)
                        .put("cacheBytes", sizes.cacheBytes)
                        .put("totalBytes", sizes.total)
                )
            }
            entry.usage?.let { usage ->
                obj.put(
                    "usage",
                    JSONObject()
                        .put("lastUsedAt", if (usage.lastTimeUsed > 0L) formatDate(usage.lastTimeUsed) else JSONObject.NULL)
                        .put("foreground7dMs", usage.foreground7d)
                        .put("foreground30dMs", usage.foreground30d)
                )
            }

            obj.put("isFavorite", entry.meta.isFavorite)
            obj.put("isHidden", entry.meta.isHidden)
            obj.put("note", entry.meta.note ?: JSONObject.NULL)
            obj.put("tags", JSONArray(entry.tags.map { it.name }))
            obj.put(
                "grantedSensitivePermissions",
                JSONArray(entry.info.grantedGroups.map { it.name })
            )
            array.put(obj)
        }

        return JSONObject()
            .put("note", note)
            .put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            .put("appCount", entries.size)
            .put("apps", array)
            .toString(2)
    }

    fun defaultFileName(extension: String): String =
        "appshelf-${isoDate.format(Instant.now())}.$extension"

    private fun formatDate(epochMillis: Long): String =
        if (epochMillis <= 0L) "" else isoDate.format(Instant.ofEpochMilli(epochMillis))

    /** CSV のエスケープ。カンマ・引用符・改行を含む場合だけ引用符で囲む。 */
    private fun escapeCsv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
