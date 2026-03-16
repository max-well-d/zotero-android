package org.zotero.android.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Singleton

@Singleton
class TranslationService(
    private val settingsRepository: TranslationSettingsRepository,
) {
    suspend fun translate(
        text: String,
        sourceLanguage: String = "auto",
        targetLanguage: String? = null,
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings().normalized()
        val resolvedTarget = targetLanguage?.trim().orEmpty().ifBlank { settings.targetLanguage }
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No text selected"))
        }
        if (!settings.hasValidCredentials()) {
            return@withContext Result.failure(IllegalStateException("Configure translation credentials in Settings first"))
        }
        if (!TranslationLanguages.isSupported(settings.api, resolvedTarget)) {
            return@withContext Result.failure(IllegalArgumentException("Unsupported target language for ${settings.api.displayName}"))
        }
        runCatching {
            when (settings.api) {
                TranslationApi.BAIDU -> translateWithBaidu(cleanedText, sourceLanguage, resolvedTarget, settings)
                TranslationApi.DEEPL -> translateWithDeepL(cleanedText, sourceLanguage, resolvedTarget, settings)
            }
        }
    }

    suspend fun testConnection(candidateSettings: TranslationSettings? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val settings = (candidateSettings ?: settingsRepository.getSettings()).normalized()
        if (!settings.hasValidCredentials()) {
            return@withContext Result.failure(IllegalStateException("Configure translation credentials in Settings first"))
        }
        runCatching {
            when (settings.api) {
                TranslationApi.BAIDU -> {
                    translateWithBaidu(
                        text = "Hello world",
                        sourceLanguage = "auto",
                        targetLanguage = settings.targetLanguage,
                        settings = settings,
                    )
                }

                TranslationApi.DEEPL -> {
                    translateWithDeepL(
                        text = "Hello world",
                        sourceLanguage = "auto",
                        targetLanguage = settings.targetLanguage,
                        settings = settings,
                    )
                }
            }
            Unit
        }
    }

    private fun translateWithBaidu(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        settings: TranslationSettings,
    ): TranslationResult {
        val salt = UUID.randomUUID().toString().take(8)
        val sign = BaiduSignUtil.generate(
            appId = settings.baiduAppId,
            query = text,
            salt = salt,
            secretKey = settings.baiduSecretKey,
        )
        val params = linkedMapOf(
            "q" to text,
            "from" to normalizeBaiduLanguage(sourceLanguage),
            "to" to normalizeBaiduLanguage(targetLanguage),
            "appid" to settings.baiduAppId,
            "salt" to salt,
            "sign" to sign,
        )
        val json = postForm("https://fanyi-api.baidu.com/api/trans/vip/translate", params)
        if (json.has("error_code")) {
            throw IllegalStateException(json.optString("error_msg", "Baidu translation request failed"))
        }
        val translations = json.optJSONArray("trans_result") ?: JSONArray()
        val translatedText = translations.optJSONObject(0)?.optString("dst").orEmpty()
        if (translatedText.isBlank()) {
            throw IllegalStateException("Baidu translation returned no result")
        }
        return TranslationResult(
            originalText = text,
            translatedText = translatedText,
            detectedSourceLang = json.optString("from", sourceLanguage),
            targetLanguage = json.optString("to", targetLanguage),
            api = TranslationApi.BAIDU,
        )
    }

    private fun translateWithDeepL(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        settings: TranslationSettings,
    ): TranslationResult {
        val normalizedTarget = normalizeDeepLLanguage(targetLanguage)
        val params = linkedMapOf(
            "text" to text,
            "target_lang" to normalizedTarget,
        )
        if (sourceLanguage.isNotBlank() && sourceLanguage != "auto") {
            params["source_lang"] = normalizeDeepLLanguage(sourceLanguage)
        }
        val json = postForm(
            url = deepLBaseUrl(settings.apiKey),
            params = params,
            headers = mapOf("Authorization" to "DeepL-Auth-Key ${settings.apiKey}"),
        )
        val translations = json.optJSONArray("translations") ?: JSONArray()
        val result = translations.optJSONObject(0)
            ?: throw IllegalStateException("DeepL returned no result")
        val translatedText = result.optString("text")
        if (translatedText.isBlank()) {
            throw IllegalStateException("DeepL returned empty text")
        }
        return TranslationResult(
            originalText = text,
            translatedText = translatedText,
            detectedSourceLang = result.optString("detected_source_language", sourceLanguage),
            targetLanguage = normalizedTarget,
            api = TranslationApi.DEEPL,
        )
    }

    private fun postForm(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val body = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }
        DataOutputStream(connection.outputStream).use { it.writeBytes(body) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        val responseText = BufferedReader(stream.reader()).use { reader ->
            buildString {
                var line = reader.readLine()
                while (line != null) {
                    append(line)
                    line = reader.readLine()
                }
            }
        }
        return JSONObject(responseText)
    }

    private fun deepLBaseUrl(apiKey: String): String {
        return if (apiKey.endsWith(":fx")) {
            "https://api-free.deepl.com/v2/translate"
        } else {
            "https://api.deepl.com/v2/translate"
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun normalizeBaiduLanguage(language: String): String {
        return when (language.lowercase()) {
            "auto", "" -> "auto"
            "zh", "zh-cn", "zh_hans" -> "zh"
            else -> language.lowercase()
        }
    }

    private fun normalizeDeepLLanguage(language: String): String {
        return when (language.lowercase()) {
            "zh", "zh-cn", "zh_hans" -> "ZH"
            "pt-br" -> "PT-BR"
            "pt-pt" -> "PT-PT"
            else -> language.uppercase()
        }
    }
}
