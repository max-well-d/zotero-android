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
            baiduAppId = decryptOrEmpty(KEY_BAIDU_APP_ID),
            baiduSecretKey = decryptOrEmpty(KEY_BAIDU_SECRET),
            apiKey = decryptOrEmpty(KEY_API_KEY),
        ).normalized()
    }

    fun save(settings: TranslationSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putString(KEY_API, normalized.api.name)
            .putString(KEY_TARGET_LANGUAGE, normalized.targetLanguage)
            .putBoolean(KEY_SHOW_ORIGINAL, normalized.showOriginalText)
            .putString(KEY_BAIDU_APP_ID, cipher.encrypt(normalized.baiduAppId))
            .putString(KEY_BAIDU_SECRET, cipher.encrypt(normalized.baiduSecretKey))
            .putString(KEY_API_KEY, cipher.encrypt(normalized.apiKey))
            .apply()
    }

    private fun decryptOrEmpty(key: String): String {
        val stored = prefs.getString(key, "").orEmpty()
        if (stored.isBlank()) {
            return ""
        }
        return runCatching { cipher.decrypt(stored) }
            .getOrElse {
                prefs.edit().remove(key).apply()
                ""
            }
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
