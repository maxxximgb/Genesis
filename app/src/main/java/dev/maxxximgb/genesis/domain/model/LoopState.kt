package dev.maxxximgb.genesis.domain.model

enum class LoopState {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE,
}

fun nextLoopState(current: LoopState): LoopState = when (current) {
    LoopState.OFF -> LoopState.REPEAT_ALL
    LoopState.REPEAT_ALL -> LoopState.REPEAT_ONE
    LoopState.REPEAT_ONE -> LoopState.SHUFFLE
    LoopState.SHUFFLE -> LoopState.OFF
}

fun loopStateOf(repeatMode: RepeatMode, shuffleEnabled: Boolean): LoopState = when {
    shuffleEnabled -> LoopState.SHUFFLE
    repeatMode == RepeatMode.ONE -> LoopState.REPEAT_ONE
    repeatMode == RepeatMode.ALL -> LoopState.REPEAT_ALL
    else -> LoopState.OFF
}

fun LoopState.toRepeatAndShuffle(): Pair<RepeatMode, Boolean> = when (this) {
    LoopState.OFF -> RepeatMode.OFF to false
    LoopState.REPEAT_ALL -> RepeatMode.ALL to false
    LoopState.REPEAT_ONE -> RepeatMode.ONE to false
    LoopState.SHUFFLE -> RepeatMode.OFF to true
}
