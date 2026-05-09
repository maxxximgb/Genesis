package dev.maxxximgb.genesis.data.audiofx

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.audiofx.AudioFxCapabilities
import dev.maxxximgb.genesis.domain.audiofx.AudioFxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the lifecycle of the `Equalizer` effect attached to ExoPlayer's audio session, and
 * observes [UserPreferencesStore] to apply changes in real time.
 *
 * BassBoost / Virtualizer / Spatializer were removed: on most OEM stacks (notably Samsung
 * Sound Alive) those effects are intercepted at the HAL and produce barely-audible or no
 * output, while the UI implies they're working — which is worse UX than not exposing them.
 *
 * Lifecycle:
 *  - [attach] on player creation with the player's audio session id
 *  - [release] on player destruction
 *  - [reattach] when the player's audio session id changes (rare, but ExoPlayer emits this
 *    via AnalyticsListener.onAudioSessionIdChanged)
 *
 * Master toggle behavior: when off, the underlying `Equalizer` is physically released to free
 * DSP resources. When on, it is recreated and the persisted band levels are reapplied.
 */
@Singleton
class AudioFxController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null

    private var equalizer: Equalizer? = null

    private var currentSessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE
    private var lastAppliedState: AudioFxState? = null

    private val _capabilities = MutableStateFlow(AudioFxCapabilities.Unsupported)
    val capabilities: StateFlow<AudioFxCapabilities> = _capabilities.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == AudioManager.ERROR) return
        if (currentSessionId == audioSessionId && (equalizer != null || lastAppliedState?.masterEnabled == false)) {
            return
        }
        currentSessionId = audioSessionId
        // Probe device capabilities once. Even when master is off we want to know what the UI
        // can offer if the user toggles on.
        probeCapabilitiesIfNeeded()
        startObservingPreferences()
    }

    fun reattach(audioSessionId: Int) {
        releaseEffects()
        attach(audioSessionId)
    }

    /**
     * Force a re-application of the last seen audio-fx state. Called on track transitions —
     * when ExoPlayer recreates its internal AudioTrack for a new format, some HALs (notably
     * Samsung Sound Alive) lose the band levels we set, leading to brief noise/hiss until the
     * next pref write nudges the observer. Re-asserting the bands on every transition keeps
     * the Equalizer in sync with the live AudioTrack without releasing/recreating effects.
     */
    fun reapplyState() {
        val state = lastAppliedState ?: return
        if (!state.masterEnabled) return
        scope.launch { applyState(state) }
    }

    /**
     * Idempotent capability probe. Called both when the player publishes a real audio session
     * id AND when the EQ screen opens — the latter handles the case where the user opens the
     * equalizer before any track has played, so the probe doesn't have to wait on playback.
     *
     * Uses a temporary [AudioManager.generateAudioSessionId] when no real session is attached,
     * so the probe runs against the same audio HAL the player will eventually use, but without
     * holding system effect resources after we're done.
     */
    fun probeCapabilitiesIfNeeded() {
        if (_capabilities.value !== AudioFxCapabilities.Unsupported) return
        val sessionForProbe = currentSessionId
            .takeIf { it != 0 && it != AudioManager.AUDIO_SESSION_ID_GENERATE && it != AudioManager.ERROR }
            ?: generateTempSessionId() ?: return
        _capabilities.value = probeCapabilities(sessionForProbe)
        val caps = _capabilities.value
        Log.i(
            TAG,
            "Probed audio FX: eq=${caps.equalizerSupported}/${caps.numberOfBands}b (session=$sessionForProbe)",
        )
    }

    private fun generateTempSessionId(): Int? {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        return runCatching { am.generateAudioSessionId() }
            .getOrNull()
            ?.takeIf { it != AudioManager.ERROR && it != 0 }
    }

    fun release() {
        observerJob?.cancel()
        observerJob = null
        releaseEffects()
        scope.cancel()
    }

    private fun startObservingPreferences() {
        observerJob?.cancel()
        observerJob = scope.launch {
            preferences.observeAudioFxState().collect { state ->
                applyState(state)
                lastAppliedState = state
            }
        }
    }

    private fun applyState(state: AudioFxState) {
        if (!state.masterEnabled) {
            if (equalizer != null) Log.i(TAG, "Master off — releasing equalizer")
            releaseEffects()
            return
        }
        val sessionId = currentSessionId
        if (sessionId == 0 || sessionId == AudioManager.AUDIO_SESSION_ID_GENERATE) {
            Log.w(TAG, "Master on but no audio session yet — applyState skipped")
            return
        }
        val caps = _capabilities.value
        Log.d(TAG, "applyState session=$sessionId master=on bands=${state.bandLevelsMillibels}")

        if (caps.equalizerSupported) {
            val eq = equalizer ?: createEqualizer(sessionId).also { equalizer = it }
            eq?.let { applyBands(it, state.bandLevelsMillibels, caps) }
        }
    }

    private fun applyBands(eq: Equalizer, bands: List<Int>, caps: AudioFxCapabilities) {
        eq.runCatching {
            enabled = true
            val n = caps.numberOfBands
            val range = caps.bandLevelRangeMillibels
            for (i in 0 until n) {
                val mb = bands.getOrNull(i) ?: 0
                val coerced = mb.coerceIn(range.first, range.last).toShort()
                setBandLevel(i.toShort(), coerced)
            }
        }
    }

    private fun probeCapabilities(sessionId: Int): AudioFxCapabilities {
        val eq = createEqualizer(sessionId)
        val numberOfBands = eq?.numberOfBands?.toInt() ?: 0
        // numberOfBands == 0 happens on devices (notably some Samsung models with Sound Alive)
        // that allow constructing the Equalizer object but don't expose any per-band controls
        // to third-party apps. Treat that as unsupported — the UI has nothing to render.
        val eqSupported = eq != null && numberOfBands > 0
        val bandFrequencies = if (eq != null) {
            buildList {
                for (i in 0 until numberOfBands) {
                    val freqMilliHz = eq.runCatching { getCenterFreq(i.toShort()) }.getOrDefault(0)
                    add(freqMilliHz / 1000)
                }
            }
        } else emptyList()
        val range = eq?.bandLevelRange?.let { it[0].toInt()..it[1].toInt() } ?: 0..0
        eq?.release()

        return AudioFxCapabilities(
            equalizerSupported = eqSupported,
            numberOfBands = numberOfBands,
            bandCenterFrequenciesHz = bandFrequencies,
            bandLevelRangeMillibels = range,
        )
    }

    private fun createEqualizer(sessionId: Int): Equalizer? = runCatching {
        Equalizer(EFFECT_PRIORITY, sessionId)
    }.onFailure { Log.w(TAG, "Equalizer not supported: ${it.message}") }.getOrNull()

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        equalizer = null
    }

    private companion object {
        const val TAG = "AudioFxController"
        // Priority above 0 means our app outranks system defaults for this session.
        const val EFFECT_PRIORITY = 1
    }
}
