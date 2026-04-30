package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.domain.model.LoopState

data class TransportEnabled(val previous: Boolean, val next: Boolean) {
    companion object {
        val DISABLED = TransportEnabled(previous = false, next = false)
    }
}

fun transportEnabled(
    mode: LoopState,
    currentIndex: Int,
    lastIndex: Int,
): TransportEnabled {
    if (currentIndex < 0 || lastIndex < 0 || currentIndex > lastIndex) {
        return TransportEnabled.DISABLED
    }
    return when (mode) {
        LoopState.OFF -> TransportEnabled(
            previous = currentIndex > 0,
            next = currentIndex < lastIndex,
        )
        LoopState.REPEAT_ALL,
        LoopState.REPEAT_ONE,
        LoopState.SHUFFLE -> TransportEnabled(previous = true, next = true)
    }
}
