package dev.maxxximgb.genesis.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.repository.MediaLibraryRepositoryImpl
import dev.maxxximgb.genesis.data.repository.PlaylistRepositoryImpl
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaLibraryRepository(
        impl: MediaLibraryRepositoryImpl,
    ): MediaLibraryRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl,
    ): PlaylistRepository
}
