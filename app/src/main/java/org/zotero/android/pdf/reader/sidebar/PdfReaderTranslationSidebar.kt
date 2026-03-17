package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState

@Composable
internal fun PdfReaderTranslationSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = viewState.translationSourceText,
            onValueChange = vMInterface::onTranslationSourceTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("原文") },
        )

        OutlinedTextField(
            value = viewState.translationTranslatedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("译文") },
        )

        viewState.translationApiName.takeIf { it.isNotBlank() }?.let { apiName ->
            Text(
                text = "API: $apiName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        viewState.translationErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = viewState.translationAutoTranslateEnabled,
                    onCheckedChange = vMInterface::onTranslationAutoTranslateChange,
                )
                Text(text = "自动翻译")
            }

            Button(
                onClick = { vMInterface.translateSidebarText(force = true) },
                enabled = !viewState.isTranslationLoading && viewState.translationSourceText.isNotBlank(),
            ) {
                if (viewState.isTranslationLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = "翻译")
                }
            }
        }
    }
}
