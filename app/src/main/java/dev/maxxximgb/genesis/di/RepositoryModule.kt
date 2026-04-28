package dev.maxxximgb.genesis.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.repository.MediaLibraryRepositoryImpl
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaLibraryRepository(
        impl: MediaLibraryRepositoryImpl,
    ): MediaLibraryRepository
}
