package dev.maxxximgb.genesis.data.locale

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.maxxximgb.genesis.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetUpdater: WidgetUpdater,
) {

    enum class Language(val tag: String?) {
        SYSTEM(null),
        ENGLISH("en"),
        RUSSIAN("ru"),
        SPANISH("es"),
        CHINESE("zh"),
        GERMAN("de"),
        FRENCH("fr"),
        PORTUGUESE("pt"),
        JAPANESE("ja");

        companion object {
            fun fromTag(tag: String?): Language = when {
                tag.isNullOrBlank() -> SYSTEM
                tag.startsWith("en") -> ENGLISH
                tag.startsWith("ru") -> RUSSIAN
                tag.startsWith("es") -> SPANISH
                tag.startsWith("zh") -> CHINESE
                tag.startsWith("de") -> GERMAN
                tag.startsWith("fr") -> FRENCH
                tag.startsWith("pt") -> PORTUGUESE
                tag.startsWith("ja") -> JAPANESE
                else -> SYSTEM
            }
        }
    }

    private val manager: LocaleManager =
        context.getSystemService(LocaleManager::class.java)

    fun current(): Language = Language.fromTag(currentTag())

    fun currentTag(): String? = manager.applicationLocales
        .takeUnless { it.isEmpty }
        ?.get(0)
        ?.toLanguageTag()

    fun setLanguage(language: Language) {
        manager.applicationLocales = when (val tag = language.tag) {
            null -> LocaleList.getEmptyLocaleList()
            else -> LocaleList.forLanguageTags(tag)
        }
        // The locale switch only takes effect on the next process recreate for activities, but
        // widget strings (track row "Foreign playlist", configure-card text, content descriptions)
        // re-resolve immediately when the next provideContent runs. Ping the updater so the user
        // doesn't see English chrome on the widget until the next state change happens to refresh it.
        widgetUpdater.requestUpdate()
    }
}
