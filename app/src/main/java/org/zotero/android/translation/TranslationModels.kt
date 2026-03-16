package org.zotero.android.translation

data class TranslationSettings(
    val api: TranslationApi = TranslationApi.BAIDU,
    val targetLanguage: String = "zh",
    val showOriginalText: Boolean = true,
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
    val supported = listOf(
        TranslationLanguage(code = "zh", displayName = "简体中文"),
        TranslationLanguage(code = "en", displayName = "English"),
        TranslationLanguage(code = "ja", displayName = "日本語"),
        TranslationLanguage(code = "ko", displayName = "한국어"),
        TranslationLanguage(code = "fr", displayName = "Français"),
        TranslationLanguage(code = "de", displayName = "Deutsch"),
    )
}
