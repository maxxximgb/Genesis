package dev.maxxximgb.genesis.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaylistModePreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchHistoryPreferences
