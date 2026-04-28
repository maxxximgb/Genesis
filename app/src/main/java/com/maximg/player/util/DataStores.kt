package com.maximg.player.util

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.widgetDataStore by preferencesDataStore(name = "widget_prefs")
val Context.playbackDataStore by preferencesDataStore(name = "playback_state")
val Context.uiDataStore by preferencesDataStore(name = "ui_prefs")

val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
