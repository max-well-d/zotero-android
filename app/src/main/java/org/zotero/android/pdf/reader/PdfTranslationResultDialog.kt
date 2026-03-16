package org.zotero.android.pdf.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.R
import org.zotero.android.androidx.content.copyPlainTextToClipboard

@Composable
internal fun PdfTranslationResultDialog(
    viewState: PdfReaderViewState,
    viewModel: PdfReaderViewModel,
) {
    val dialogState = viewState.translationDialogState
    if (!viewState.isTranslationLoading && dialogState == null) {
        return
    }

    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = viewModel::dismissTranslationDialog,
        title = {
            Text(text = stringResource(id = R.string.translation_result))
        },
        text = {
            if (viewState.isTranslationLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.translation_loading),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else if (dialogState != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = dialogState.translatedText)
                    if (dialogState.showOriginalText) {
                        Text(
                            text = dialogState.originalText,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Text(
                        text = dialogState.apiName,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!viewState.isTranslationLoading && dialogState != null) {
                TextButton(
                    onClick = {
                        context.copyPlainTextToClipboard(dialogState.translatedText)
                        viewModel.dismissTranslationDialog()
                    }
                ) {
                    Text(text = stringResource(id = R.string.translation_copy))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissTranslationDialog) {
                Text(text = stringResource(id = R.string.translation_close))
            }
        },
    )
}
