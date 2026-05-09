package dev.maxxximgb.genesis.ui.equalizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset

@Composable
fun PresetNameDialog(
    title: String,
    initialName: String,
    validate: (String) -> EqualizerViewModel.NameValidation,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val validation = validate(name)
    val errorText = when (validation) {
        EqualizerViewModel.NameValidation.Duplicate -> stringResource(R.string.equalizer_preset_name_duplicate)
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= EqualizerPreset.MAX_NAME_LENGTH) name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.equalizer_preset_name_hint)) },
                    isError = errorText != null,
                    supportingText = if (errorText != null) {
                        { Text(errorText, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = validation == EqualizerViewModel.NameValidation.Valid,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
