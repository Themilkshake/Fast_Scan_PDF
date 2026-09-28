package com.superfastscan.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.superfastscan.data.local.entity.DocumentEntity
import com.superfastscan.data.local.entity.PageEntity

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
}
