package org.zotero.android.translation

data class TranslationSettings(
    val api: TranslationApi = TranslationApi.BAIDU,
    val targetLanguage: String = "zh",
    val showOriginalText: Boolean = true,
    val autoTranslateDelayMs: Int = 350,
    val baiduAppId: String = "",
    val baiduSecretKey: String = "",
    val apiKey: String = "",
) {
    fun hasValidCredentials(): Boolean {
        return when (api) {
            TranslationApi.BAIDU -> baiduAppId.isNotBlank() && baiduSecretKey.isNotBlank()
            TranslationApi.DEEPL -> apiKey.isNotBlank()
        }
    }

    fun normalized(): TranslationSettings {
        val supportedCodes = TranslationLanguages.codesFor(api)
        val normalizedTarget = targetLanguage.ifBlank { TranslationLanguages.defaultCodeFor(api) }
            .let { code -> if (code in supportedCodes) code else TranslationLanguages.defaultCodeFor(api) }

        return copy(
            targetLanguage = normalizedTarget,
            autoTranslateDelayMs = autoTranslateDelayMs.coerceIn(0, 60_000),
            baiduAppId = baiduAppId.trim(),
            baiduSecretKey = baiduSecretKey.trim(),
            apiKey = apiKey.trim(),
        )
    }
}

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val detectedSourceLang: String,
    val targetLanguage: String,
    val api: TranslationApi,
)

data class TranslationLanguage(
    val code: String,
    val displayName: String,
)

object TranslationLanguages {
    private val baidu = listOf(
        TranslationLanguage(code = "zh", displayName = "简体中文"),
        TranslationLanguage(code = "en", displayName = "English"),
        TranslationLanguage(code = "ja", displayName = "日本語"),
        TranslationLanguage(code = "ko", displayName = "한국어"),
        TranslationLanguage(code = "fr", displayName = "Français"),
        TranslationLanguage(code = "de", displayName = "Deutsch"),
        TranslationLanguage(code = "es", displayName = "Español"),
        TranslationLanguage(code = "ru", displayName = "Русский"),
    )

    private val deepl = listOf(
        TranslationLanguage(code = "zh", displayName = "简体中文"),
        TranslationLanguage(code = "en", displayName = "English"),
        TranslationLanguage(code = "ja", displayName = "日本語"),
        TranslationLanguage(code = "ko", displayName = "한국어"),
        TranslationLanguage(code = "fr", displayName = "Français"),
        TranslationLanguage(code = "de", displayName = "Deutsch"),
        TranslationLanguage(code = "es", displayName = "Español"),
        TranslationLanguage(code = "pt", displayName = "Português"),
        TranslationLanguage(code = "it", displayName = "Italiano"),
        TranslationLanguage(code = "ru", displayName = "Русский"),
    )

    val supported: List<TranslationLanguage>
        get() = forApi(TranslationApi.BAIDU)

    fun forApi(api: TranslationApi): List<TranslationLanguage> {
        return when (api) {
            TranslationApi.BAIDU -> baidu
            TranslationApi.DEEPL -> deepl
        }
    }

    fun codesFor(api: TranslationApi): Set<String> = forApi(api).mapTo(linkedSetOf()) { it.code }

    fun defaultCodeFor(api: TranslationApi): String = forApi(api).firstOrNull()?.code ?: "zh"

    fun isSupported(api: TranslationApi, code: String): Boolean = code in codesFor(api)
}
