package dev.maxxximgb.genesis.data.locale

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    enum class Language(val tag: String?) {
        SYSTEM(null),
        ENGLISH("en"),
        RUSSIAN("ru");

        companion object {
            fun fromTag(tag: String?): Language = when {
                tag.isNullOrBlank() -> SYSTEM
                tag.startsWith("en") -> ENGLISH
                tag.startsWith("ru") -> RUSSIAN
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
    }
}
