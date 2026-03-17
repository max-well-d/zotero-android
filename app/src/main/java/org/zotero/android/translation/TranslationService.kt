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
    ): Result<TranslationResult> {
        val settings = settingsRepository.getSettings()
        return translateWithSettings(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            settings = settings,
        )
    }

    suspend fun testConnection(): Result<Unit> {
        return translate(text = "Hello world").map { Unit }
    }

    suspend fun testConnection(settings: TranslationSettings): Result<Unit> {
        return translateWithSettings(
            text = "Hello world",
            settings = settings,
        ).map { Unit }
    }

    suspend fun testConnection(testText: String): Result<Unit> {
        return translate(text = testText).map { Unit }
    }

    private suspend fun translateWithSettings(
        text: String,
        sourceLanguage: String = "auto",
        targetLanguage: String? = null,
        settings: TranslationSettings,
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        val resolvedTarget = targetLanguage ?: settings.targetLanguage
        val normalizedInputText = normalizeInputText(text)

        if (normalizedInputText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("翻译文本不能为空"))
        }
        if (!settings.hasValidCredentials()) {
            return@withContext Result.failure(IllegalStateException("请先在设置中配置翻译凭证"))
        }

        runCatching {
            when (settings.api) {
                TranslationApi.BAIDU -> translateWithBaidu(
                    originalText = text,
                    requestText = normalizedInputText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = resolvedTarget,
                    settings = settings,
                )
                TranslationApi.DEEPL -> translateWithDeepL(
                    originalText = text,
                    requestText = normalizedInputText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = resolvedTarget,
                    settings = settings,
                )
            }
        }
    }

    private fun translateWithBaidu(
        originalText: String,
        requestText: String,
        sourceLanguage: String,
        targetLanguage: String,
        settings: TranslationSettings,
    ): TranslationResult {
        val salt = UUID.randomUUID().toString().take(8)
        val sign = BaiduSignUtil.generate(
            appId = settings.baiduAppId,
            query = requestText,
            salt = salt,
            secretKey = settings.baiduSecretKey,
        )
        val params = linkedMapOf(
            "q" to requestText,
            "from" to sourceLanguage,
            "to" to normalizeBaiduLanguage(targetLanguage),
            "appid" to settings.baiduAppId,
            "salt" to salt,
            "sign" to sign,
        )
        val json = postForm("https://fanyi-api.baidu.com/api/trans/vip/translate", params)
        if (json.has("error_code")) {
            throw IllegalStateException(json.optString("error_msg", "百度翻译请求失败"))
        }
        val translations = json.optJSONArray("trans_result") ?: JSONArray()
        val translatedText = buildBaiduTranslatedText(translations)
        if (translatedText.isBlank()) {
            throw IllegalStateException("百度翻译未返回结果")
        }
        return TranslationResult(
            originalText = originalText,
            translatedText = translatedText,
            detectedSourceLang = json.optString("from", sourceLanguage),
            targetLanguage = json.optString("to", targetLanguage),
            api = TranslationApi.BAIDU,
        )
    }

    private fun translateWithDeepL(
        originalText: String,
        requestText: String,
        sourceLanguage: String,
        targetLanguage: String,
        settings: TranslationSettings,
    ): TranslationResult {
        val params = linkedMapOf(
            "text" to requestText,
            "target_lang" to normalizeDeepLLanguage(targetLanguage),
        )
        if (sourceLanguage.isNotBlank() && sourceLanguage != "auto") {
            params["source_lang"] = normalizeDeepLLanguage(sourceLanguage)
        }
        val json = postForm(
            url = "https://api-free.deepl.com/v2/translate",
            params = params,
            headers = mapOf("Authorization" to "DeepL-Auth-Key ${settings.apiKey}"),
        )
        val translations = json.optJSONArray("translations") ?: JSONArray()
        val translatedText = buildDeepLTranslatedText(translations)
        if (translatedText.isBlank()) {
            throw IllegalStateException("DeepL 未返回结果")
        }
        val firstResult = translations.optJSONObject(0) ?: JSONObject()
        return TranslationResult(
            originalText = originalText,
            translatedText = translatedText,
            detectedSourceLang = firstResult.optString("detected_source_language", sourceLanguage),
            targetLanguage = normalizeDeepLLanguage(targetLanguage),
            api = TranslationApi.DEEPL,
        )
    }

    private fun buildBaiduTranslatedText(translations: JSONArray): String {
        val parts = buildList {
            for (index in 0 until translations.length()) {
                val dst = normalizeParagraphText(
                    translations.optJSONObject(index)?.optString("dst").orEmpty(),
                )
                if (dst.isNotBlank()) {
                    add(dst)
                }
            }
        }
        return normalizeParagraphText(parts.joinToString(" "))
    }

    private fun buildDeepLTranslatedText(translations: JSONArray): String {
        val parts = buildList {
            for (index in 0 until translations.length()) {
                val text = normalizeParagraphText(
                    translations.optJSONObject(index)?.optString("text").orEmpty(),
                )
                if (text.isNotBlank()) {
                    add(text)
                }
            }
        }
        return normalizeParagraphText(parts.joinToString(" "))
    }

    private fun normalizeInputText(text: String): String {
        return text
            .replace(Regex("[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeParagraphText(text: String): String {
        return text
            .replace(Regex("[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun normalizeBaiduLanguage(language: String): String {
        return when (language.lowercase()) {
            "zh", "zh-cn", "zh_hans" -> "zh"
            else -> language.lowercase()
        }
    }

    private fun normalizeDeepLLanguage(language: String): String {
        return when (language.lowercase()) {
            "zh", "zh-cn", "zh_hans" -> "ZH"
            else -> language.uppercase()
        }
    }
}
