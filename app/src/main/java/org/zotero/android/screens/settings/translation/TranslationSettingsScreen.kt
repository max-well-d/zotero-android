package org.zotero.android.screens.settings.translation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.R
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItemWithDescription
import org.zotero.android.screens.settings.quickcopy.SettingsQuickCopySwitchItem
import org.zotero.android.translation.TranslationApi
import org.zotero.android.translation.TranslationLanguages
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun TranslationSettingsScreen(
    onBack: () -> Unit,
    viewModel: TranslationSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val languages = remember(settings.api) { TranslationLanguages.forApi(settings.api) }
    var showApiDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    AppThemeM3 {
        CustomScaffoldM3(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.translation_settings_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(Drawables.arrow_back_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NewSettingsItemWithDescription(
                    title = stringResource(id = R.string.translation_api),
                    description = settings.api.displayName,
                    onItemTapped = { showApiDialog = true },
                )

                val selectedLanguage = languages.firstOrNull {
                    it.code == settings.targetLanguage
                }?.displayName ?: settings.targetLanguage
                NewSettingsItemWithDescription(
                    title = stringResource(id = R.string.translation_target_language),
                    description = selectedLanguage,
                    onItemTapped = { showLanguageDialog = true },
                )

                SettingsQuickCopySwitchItem(
                    title = stringResource(id = R.string.translation_show_original),
                    isChecked = settings.showOriginalText,
                    onCheckedChange = viewModel::updateShowOriginal,
                )

                NewSettingsDivider()

                if (settings.api == TranslationApi.BAIDU) {
                    OutlinedTextField(
                        value = settings.baiduAppId,
                        onValueChange = viewModel::updateBaiduAppId,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.translation_app_id)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = settings.baiduSecretKey,
                        onValueChange = viewModel::updateBaiduSecretKey,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.translation_secret_key)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = settings.apiKey,
                        onValueChange = viewModel::updateApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.translation_api_key)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.translation_save))
                    }
                    Button(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isTesting,
                    ) {
                        Text(
                            text = stringResource(
                                id = if (uiState.isTesting) {
                                    R.string.translation_testing
                                } else {
                                    R.string.translation_test_connection
                                }
                            )
                        )
                    }
                }

                uiState.statusMessage?.let { message ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (showApiDialog) {
            SelectionDialog(
                title = stringResource(id = R.string.translation_api),
                items = TranslationApi.entries.map { api ->
                    api.displayName to { viewModel.updateApi(api) }
                },
                onDismiss = { showApiDialog = false },
            )
        }

        if (showLanguageDialog) {
            SelectionDialog(
                title = stringResource(id = R.string.translation_target_language),
                items = languages.map { language ->
                    language.displayName to { viewModel.updateTargetLanguage(language.code) }
                },
                onDismiss = { showLanguageDialog = false },
            )
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    items: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { (label, action) ->
                    TextButton(
                        onClick = {
                            action()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.translation_close))
            }
        },
    )
}
