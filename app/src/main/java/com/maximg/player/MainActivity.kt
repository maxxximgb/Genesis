package com.maximg.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.maximg.player.ui.components.PermissionGate
import com.maximg.player.util.EXTRA_PLAYLIST_ID

class MainActivity : ComponentActivity() {
    private val startPlaylistIdState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        val container = AppContainerProvider.get(applicationContext)
        setContent {
            val currentStartId by startPlaylistIdState
            PermissionGate {
                MorningPlayerApp(container, startPlaylistId = currentStartId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PLAYLIST_ID, -1L) ?: -1L
        startPlaylistIdState.value = if (id > 0) id else null
    }
}
