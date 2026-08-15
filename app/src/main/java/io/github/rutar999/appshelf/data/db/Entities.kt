package io.github.rutar999.appshelf.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room のテーブル定義（要件定義書 §5）。
 *
 * OS から毎回取れる情報（アイコン・バージョン・権限など）はここに保存しない。
 * 保存するのは「ユーザーが作った情報」と「時間が経つと OS から消える情報」だけ。
 */

/** ユーザーが付けた情報。行が無いアプリは「何も設定していない」状態。 */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val packageName: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val note: String? = null,
    /** このアプリが最初にそのパッケージを認識した日時 */
    val firstSeenAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "tag",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0
)

/** アプリとタグの多対多。タグを消したら紐付けも消える（CASCADE）。 */
@Entity(
    tableName = "app_tag",
    primaryKeys = ["packageName", "tagId"],
    indices = [Index(value = ["tagId"])],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppTagCrossRef(
    val packageName: String,
    val tagId: Long
)

/**
 * 日次スナップショット。
 * OS 側の使用統計は日次で約7日ぶんしか残らないため、
 * 「1年使っていない」を正確に言うには自前で貯めるしかない（要件定義書 §2.2）。
 */
@Entity(
    tableName = "usage_snapshot",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class UsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    /** yyyyMMdd 形式の整数 */
    val date: Int,
    val foregroundMs: Long,
    val lastTimeUsed: Long,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long
)
