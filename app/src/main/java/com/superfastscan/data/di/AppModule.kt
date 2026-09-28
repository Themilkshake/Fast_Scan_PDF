package com.superfastscan.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Additional app-wide singleton provisions can be added here.
    // DataStore and use cases are auto-provided via @Inject constructors.
}
