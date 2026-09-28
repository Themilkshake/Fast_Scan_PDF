package com.superfastscan.data.di

import android.content.Context
import androidx.room.Room
import com.superfastscan.data.local.db.AppDatabase
import com.superfastscan.data.local.db.DocumentDao
import com.superfastscan.data.local.db.PageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "super_fast_scan.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDocumentDao(database: AppDatabase): DocumentDao =
        database.documentDao()

    @Provides
    fun providePageDao(database: AppDatabase): PageDao =
        database.pageDao()
}
