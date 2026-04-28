package com.maximg.player.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetUpdater {
    private val updateMutex = Mutex()

    suspend fun updateAll(context: Context) {
        updateMutex.withLock {
            MorningPlayerWidgetHolder.widget.updateAll(context.applicationContext)
        }
    }

    suspend fun update(context: Context, glanceId: GlanceId) {
        updateMutex.withLock {
            MorningPlayerWidgetHolder.widget.update(context.applicationContext, glanceId)
        }
    }

    suspend fun update(context: Context, appWidgetId: Int) {
        update(context = context, glanceId = AppWidgetId(appWidgetId))
    }
}
