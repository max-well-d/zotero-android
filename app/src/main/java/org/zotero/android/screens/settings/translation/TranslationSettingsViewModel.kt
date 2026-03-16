package org.zotero.android.screens.settings.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.zotero.android.translation.TranslationApi
import org.zotero.android.translation.TranslationLanguages
import org.zotero.android.translation.TranslationSettings
import org.zotero.android.translation.TranslationSettingsRepository
import org.zotero.android.translation.TranslationService

data class TranslationSettingsUiState(
    val isTesting: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
internal class TranslationSettingsViewModel @Inject constructor(
    private val repository: TranslationSettingsRepository,
    private val translationService: TranslationService,
) : ViewModel() {

    private val _settings = MutableStateFlow(repository.getSettings().normalized())
    val settings: StateFlow<TranslationSettings> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow(TranslationSettingsUiState())
    val uiState: StateFlow<TranslationSettingsUiState> = _uiState.asStateFlow()

    fun updateApi(api: TranslationApi) {
        val fallbackLanguage = _settings.value.targetLanguage.takeIf { TranslationLanguages.isSupported(api, it) }
            ?: TranslationLanguages.defaultCodeFor(api)
        _settings.value = _settings.value.copy(api = api, targetLanguage = fallbackLanguage).normalized()
    }

    fun updateTargetLanguage(targetLanguage: String) {
        val api = _settings.value.api
        val resolved = if (TranslationLanguages.isSupported(api, targetLanguage)) {
            targetLanguage
        } else {
            TranslationLanguages.defaultCodeFor(api)
        }
        _settings.value = _settings.value.copy(targetLanguage = resolved)
    }

    fun updateShowOriginal(showOriginal: Boolean) {
        _settings.value = _settings.value.copy(showOriginalText = showOriginal)
    }

    fun updateBaiduAppId(value: String) {
        _settings.value = _settings.value.copy(baiduAppId = value)
    }

    fun updateBaiduSecretKey(value: String) {
        _settings.value = _settings.value.copy(baiduSecretKey = value)
    }

    fun updateApiKey(value: String) {
        _settings.value = _settings.value.copy(apiKey = value)
    }

    fun save() {
        val normalized = _settings.value.normalized()
        _settings.value = normalized
        repository.save(normalized)
        _uiState.value = _uiState.value.copy(statusMessage = "Translation settings saved")
    }

    fun testConnection() {
        viewModelScope.launch {
            val candidate = _settings.value.normalized()
            _settings.value = candidate
            _uiState.value = _uiState.value.copy(isTesting = true, statusMessage = null)
            val result = translationService.testConnection(candidate)
            val message = result.fold(
                onSuccess = {
                    repository.save(candidate)
                    "Connection test succeeded"
                },
                onFailure = { it.message ?: "Connection test failed" },
            )
            _uiState.value = _uiState.value.copy(isTesting = false, statusMessage = message)
        }
    }

    fun consumeStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
