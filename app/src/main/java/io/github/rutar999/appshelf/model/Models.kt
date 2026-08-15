package io.github.rutar999.appshelf.model

/**
 * このアプリが扱うデータの形（ドメインモデル）。
 *
 * 方針（要件定義書 §5）:
 * - OS から毎回取れる情報（アイコン・バージョン等）は DB に保存せず、[AppInfo] としてメモリ上に組み立てる
 * - ユーザーが作った情報（タグ・お気に入り・非表示・メモ）だけを Room に永続化する
 */

/** アプリが宣言している権限 1 件分。 */
data class AppPermission(
    val name: String,
    /** ユーザーが実際に許可しているか（PackageInfo.REQUESTED_PERMISSION_GRANTED） */
    val granted: Boolean,
    /** 危険権限グループ。該当しない（通常権限など）場合は null */
    val group: PermissionGroup?
)

/** PackageManager から読み取ったアプリ情報。 */
data class AppInfo(
    val packageName: String,
    val label: String,
    /** 検索用に正規化した文字列（ひらがな化・小文字化済み。パッケージ名も含む） */
    val searchKey: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    /** プリインストールだが後からアップデートされたアプリ（更新分だけ削除できる） */
    val isUpdatedSystemApp: Boolean,
    val categoryTitle: String?,
    val permissions: List<AppPermission>
) {
    /** アンインストール（または更新の削除）が可能か。要件定義書 §2.5 */
    val canUninstall: Boolean get() = !isSystemApp || isUpdatedSystemApp

    /** 実際に許可されている危険権限グループ */
    val grantedGroups: Set<PermissionGroup> =
        permissions.asSequence().filter { it.granted }.mapNotNull { it.group }.toSet()

    /** 宣言だけされている（許可未確定を含む）危険権限グループ */
    val declaredGroups: Set<PermissionGroup> =
        permissions.asSequence().mapNotNull { it.group }.toSet()
}

/**
 * StorageStatsManager から取れる容量内訳。使用状況アクセス未許可なら null になる。
 *
 * 注意: Android の仕様上 [dataBytes] には [cacheBytes] が **含まれている**。
 * そのため合計は app + data であり、cache を足すと二重計上になる。
 */
data class AppSizes(
    val appBytes: Long,
    /** ユーザーデータ（キャッシュを含む） */
    val dataBytes: Long,
    val cacheBytes: Long
) {
    val total: Long get() = appBytes + dataBytes

    /** 画面で「データ」として見せる値（キャッシュを別行に出すため差し引く） */
    val dataExcludingCache: Long get() = (dataBytes - cacheBytes).coerceAtLeast(0L)
}

/** UsageStatsManager から取れる使用状況。使用状況アクセス未許可なら null になる。 */
data class AppUsage(
    /** 最終起動時刻（epoch ミリ秒）。記録がなければ 0 */
    val lastTimeUsed: Long,
    val foreground7d: Long,
    val foreground30d: Long,
    val foregroundTotal: Long
)

/** ユーザーが付けた情報（Room に保存する分のドメイン表現）。 */
data class AppMeta(
    val packageName: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val note: String? = null,
    val firstSeenAt: Long = 0L
)

data class Tag(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int
)

/** 画面に流す 1 行分。OS 由来の情報とユーザー由来の情報を結合したもの。 */
data class AppEntry(
    val info: AppInfo,
    val sizes: AppSizes?,
    val usage: AppUsage?,
    val meta: AppMeta,
    val tags: List<Tag>
) {
    val packageName: String get() = info.packageName

    /** 最終起動からの経過日数。使用状況が取れない／起動記録がない場合は null。 */
    fun daysSinceLastUse(now: Long = System.currentTimeMillis()): Int? {
        val last = usage?.lastTimeUsed ?: return null
        if (last <= 0L) return null
        return ((now - last) / MILLIS_PER_DAY).toInt().coerceAtLeast(0)
    }

    /** インストールからの経過日数（使用状況が未許可のときの代替指標。F-15）。 */
    fun daysSinceInstall(now: Long = System.currentTimeMillis()): Int =
        ((now - info.firstInstallTime) / MILLIS_PER_DAY).toInt().coerceAtLeast(0)

    /**
     * 「未使用」と判定するか。
     * 使用状況が取れているのに起動記録が無い場合も未使用として扱う。
     * 使用状況アクセスが未許可（usage == null）のときは判定不能なので false。
     */
    fun isUnused(thresholdDays: Int, now: Long = System.currentTimeMillis()): Boolean {
        val u = usage ?: return false
        if (u.lastTimeUsed <= 0L) return true
        return (now - u.lastTimeUsed) / MILLIS_PER_DAY >= thresholdDays
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}

/**
 * 長期トレンド（F-16）の 1 点。日次スナップショットから組み立てる。
 *
 * OS の使用統計は日次で約7日ぶんしか残らないので、これより長い期間を見るには
 * 自前で貯めた UsageSnapshotEntity（Room）を使うしかない。
 */
data class TrendPoint(
    /** yyyyMMdd 形式 */
    val date: Int,
    /** 本体 + データ（キャッシュはデータに含まれるので足さない） */
    val totalBytes: Long,
    /** その時点での「直近30日の使用時間」 */
    val foregroundMs: Long
)

enum class SortOrder {
    NAME, INSTALLED_DESC, UPDATED_DESC, SIZE_DESC, LAST_USED_ASC, USAGE_DESC;

    companion object {
        fun fromName(value: String?): SortOrder =
            entries.firstOrNull { it.name == value } ?: NAME
    }
}

enum class ViewMode {
    LIST, GRID;

    companion object {
        fun fromName(value: String?): ViewMode =
            entries.firstOrNull { it.name == value } ?: LIST
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** 一覧の絞り込み条件（F-05）。 */
data class AppFilter(
    val favoriteOnly: Boolean = false,
    val unusedOnly: Boolean = false,
    val largeOnly: Boolean = false,
    val untaggedOnly: Boolean = false,
    val tagId: Long? = null
) {
    val isEmpty: Boolean
        get() = !favoriteOnly && !unusedOnly && !largeOnly && !untaggedOnly && tagId == null
}
