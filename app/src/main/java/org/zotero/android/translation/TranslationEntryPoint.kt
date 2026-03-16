package org.zotero.android.translation

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TranslationEntryPoint {
    fun translationService(): TranslationService
    fun translationSettingsRepository(): TranslationSettingsRepository
}
