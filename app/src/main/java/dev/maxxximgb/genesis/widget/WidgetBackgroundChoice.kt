package dev.maxxximgb.genesis.widget

sealed class WidgetBackgroundChoice {
    data object Dynamic : WidgetBackgroundChoice()
    data object Theme : WidgetBackgroundChoice()
    data class Solid(val argb: Int) : WidgetBackgroundChoice()

    companion object {
        const val MODE_DYNAMIC = 0
        const val MODE_THEME = 1
        const val MODE_SOLID = 2

        val PRESETS: List<Int> = listOf(
            0xFFE0506B.toInt(), // coral
            0xFFE07A2F.toInt(), // orange
            0xFF7FAE3A.toInt(), // yellow-green
            0xFF2FA08A.toInt(), // teal
            0xFF3D6EE0.toInt(), // blue
            0xFF8246C8.toInt(), // purple
        )
    }
}

fun WidgetBackgroundChoice.encodeMode(): Int = when (this) {
    WidgetBackgroundChoice.Dynamic -> WidgetBackgroundChoice.MODE_DYNAMIC
    WidgetBackgroundChoice.Theme -> WidgetBackgroundChoice.MODE_THEME
    is WidgetBackgroundChoice.Solid -> WidgetBackgroundChoice.MODE_SOLID
}

fun decodeBackgroundChoice(mode: Int?, color: Int?): WidgetBackgroundChoice = when (mode) {
    WidgetBackgroundChoice.MODE_THEME -> WidgetBackgroundChoice.Theme
    WidgetBackgroundChoice.MODE_SOLID -> color?.let { WidgetBackgroundChoice.Solid(it) }
        ?: WidgetBackgroundChoice.Dynamic
    else -> WidgetBackgroundChoice.Dynamic
}
