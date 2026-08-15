package io.github.rutar999.appshelf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppMetaEntity::class,
        TagEntity::class,
        AppTagCrossRef::class,
        UsageSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppShelfDatabase : RoomDatabase() {

    abstract fun appMetaDao(): AppMetaDao
    abstract fun tagDao(): TagDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao

    companion object {
        private const val NAME = "appshelf.db"

        fun create(context: Context): AppShelfDatabase =
            Room.databaseBuilder(context.applicationContext, AppShelfDatabase::class.java, NAME)
                // 読み書きが並行しても詰まらないようにする（外部キー制約は Room が既定で有効化する）
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
