package org.zotero.android.translation

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranslationModule {
    @Provides
    @Singleton
    fun provideTranslationSettingsRepository(
        @ApplicationContext context: Context,
        cipher: KeystoreStringCipher,
    ): TranslationSettingsRepository {
        return TranslationSettingsRepository(context = context, cipher = cipher)
    }

    @Provides
    @Singleton
    fun provideTranslationService(
        repository: TranslationSettingsRepository,
    ): TranslationService {
        return TranslationService(settingsRepository = repository)
    }
}
