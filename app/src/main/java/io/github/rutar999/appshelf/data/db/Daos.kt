package io.github.rutar999.appshelf.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetaDao {

    @Query("SELECT * FROM app_meta")
    fun observeAll(): Flow<List<AppMetaEntity>>

    @Query("SELECT * FROM app_meta WHERE packageName = :packageName")
    suspend fun find(packageName: String): AppMetaEntity?

    @Upsert
    suspend fun upsert(entity: AppMetaEntity)

    /** 初回認識時に行を作るためのもの。既にある行は触らない（ユーザー設定を潰さない）。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entities: List<AppMetaEntity>)

    @Query("UPDATE app_meta SET isHidden = :hidden, updatedAt = :now WHERE packageName IN (:packageNames)")
    suspend fun setHidden(packageNames: List<String>, hidden: Boolean, now: Long)

    /** アンインストールされたアプリの行を掃除する */
    @Query("DELETE FROM app_meta WHERE packageName NOT IN (:existingPackages)")
    suspend fun deleteMissing(existingPackages: List<String>)
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tag ORDER BY sortOrder ASC, name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM app_tag")
    fun observeCrossRefs(): Flow<List<AppTagCrossRef>>

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRefs(refs: List<AppTagCrossRef>)

    @Query("DELETE FROM app_tag WHERE packageName = :packageName AND tagId = :tagId")
    suspend fun removeCrossRef(packageName: String, tagId: Long)

    @Query("DELETE FROM app_tag WHERE packageName IN (:packageNames) AND tagId = :tagId")
    suspend fun removeCrossRefs(packageNames: List<String>, tagId: Long)

    @Query("DELETE FROM app_tag WHERE packageName NOT IN (:existingPackages)")
    suspend fun deleteMissing(existingPackages: List<String>)
}

@Dao
interface UsageSnapshotDao {

    /**
     * REPLACE を使う理由: 主キーは自動採番の id だが、実際の一意性は
     * (packageName, date) のユニークインデックスが担っている。
     * @Upsert は主キー基準で更新するため、この構成だと同日の再保存が効かない。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snapshots: List<UsageSnapshotEntity>)

    @Query("SELECT MAX(date) FROM usage_snapshot")
    suspend fun latestDate(): Int?

    @Query("SELECT * FROM usage_snapshot WHERE packageName = :packageName ORDER BY date ASC")
    suspend fun forPackage(packageName: String): List<UsageSnapshotEntity>

    /**
     * 蓄積した記録から見た最終起動日。
     * OS の保持期間を超えた過去も答えられるのがこのテーブルの存在意義。
     */
    @Query("SELECT packageName, MAX(lastTimeUsed) AS lastTimeUsed FROM usage_snapshot GROUP BY packageName")
    suspend fun lastUsedByPackage(): List<LastUsedRow>

    /** 古すぎる記録の掃除（2年より前） */
    @Query("DELETE FROM usage_snapshot WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Int)
}

data class LastUsedRow(
    val packageName: String,
    val lastTimeUsed: Long
)
