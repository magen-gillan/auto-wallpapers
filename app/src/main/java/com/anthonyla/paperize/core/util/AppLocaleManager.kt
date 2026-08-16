package com.anthonyla.paperize.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import com.anthonyla.paperize.domain.model.AppSettings
import java.util.Locale

/** Applies and persists the app-specific language selection. */
object AppLocaleManager {
    private const val LEGACY_PREFS = "app_locale_preferences"
    private const val LEGACY_LANGUAGE_KEY = "language_code"

    fun currentLanguage(context: Context): String =
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_LANGUAGE_KEY, AppSettings.LANGUAGE_SYSTEM)
            ?: AppSettings.LANGUAGE_SYSTEM

    fun apply(context: Context, languageCode: String) {
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LEGACY_LANGUAGE_KEY, languageCode)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applyModern(context, languageCode)
        }
    }

    fun wrap(context: Context): Context {
        val languageCode = currentLanguage(context)
        if (languageCode == AppSettings.LANGUAGE_SYSTEM) return context

        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applyModern(context: Context, languageCode: String) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = if (languageCode == AppSettings.LANGUAGE_SYSTEM) {
            android.os.LocaleList.getEmptyLocaleList()
        } else {
            android.os.LocaleList.forLanguageTags(languageCode)
        }
    }
}
