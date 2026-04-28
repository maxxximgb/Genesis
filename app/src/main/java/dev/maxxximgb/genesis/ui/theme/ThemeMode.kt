package dev.maxxximgb.genesis.ui.theme

enum class ThemeMode {
    AUTO, LIGHT, DARK;

    companion object {
        val DEFAULT = AUTO

        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
