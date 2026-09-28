package com.superfastscan.data.di

import com.superfastscan.data.repository.DocumentRepositoryImpl
import com.superfastscan.data.repository.SettingsRepositoryImpl
import com.superfastscan.domain.repository.DocumentRepository
import com.superfastscan.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindOcrService(
        impl: com.superfastscan.data.service.MlKitOcrService
    ): com.superfastscan.domain.service.OcrService

    @Binds
    @Singleton
    abstract fun bindSearchablePdfService(
        impl: com.superfastscan.data.service.SearchablePdfServiceImpl
    ): com.superfastscan.domain.service.SearchablePdfService
}
