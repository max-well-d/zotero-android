package org.zotero.android.translation

import android.content.Context
import javax.inject.Singleton

@Singleton
class TranslationSettingsRepository(
    context: Context,
    private val cipher: KeystoreStringCipher,
) {
    private val prefs = context.getSharedPreferences("translation_settings", Context.MODE_PRIVATE)

    fun getSettings(): TranslationSettings {
        return TranslationSettings(
            api = prefs.getString(KEY_API, TranslationApi.BAIDU.name)
                ?.let { runCatching { TranslationApi.valueOf(it) }.getOrDefault(TranslationApi.BAIDU) }
                ?: TranslationApi.BAIDU,
            targetLanguage = prefs.getString(KEY_TARGET_LANGUAGE, "zh") ?: "zh",
            showOriginalText = prefs.getBoolean(KEY_SHOW_ORIGINAL, true),
            baiduAppId = cipher.decrypt(prefs.getString(KEY_BAIDU_APP_ID, "") ?: ""),
            baiduSecretKey = cipher.decrypt(prefs.getString(KEY_BAIDU_SECRET, "") ?: ""),
            apiKey = cipher.decrypt(prefs.getString(KEY_API_KEY, "") ?: ""),
        )
    }

    fun save(settings: TranslationSettings) {
        prefs.edit()
            .putString(KEY_API, settings.api.name)
            .putString(KEY_TARGET_LANGUAGE, settings.targetLanguage)
            .putBoolean(KEY_SHOW_ORIGINAL, settings.showOriginalText)
            .putString(KEY_BAIDU_APP_ID, cipher.encrypt(settings.baiduAppId.trim()))
            .putString(KEY_BAIDU_SECRET, cipher.encrypt(settings.baiduSecretKey.trim()))
            .putString(KEY_API_KEY, cipher.encrypt(settings.apiKey.trim()))
            .apply()
    }

    private companion object {
        const val KEY_API = "api"
        const val KEY_TARGET_LANGUAGE = "targetLanguage"
        const val KEY_SHOW_ORIGINAL = "showOriginal"
        const val KEY_BAIDU_APP_ID = "baiduAppId"
        const val KEY_BAIDU_SECRET = "baiduSecret"
        const val KEY_API_KEY = "apiKey"
    }
}
