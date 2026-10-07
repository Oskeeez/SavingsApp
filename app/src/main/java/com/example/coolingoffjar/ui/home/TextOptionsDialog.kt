package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.RoomText
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * What happens when the heading on the wall is tapped: a soft cream card offering to rename the text or remove it.
 * Renaming swaps the card for two fields (the heading and the line under it).
 */
@Composable
fun TextOptionsDialog(
    text: RoomText,
    onRename: (title: String, body: String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = JarTheme.palette
    var renaming by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(text.title) }
    var body by rememberSaveable { mutableStateOf(text.body) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(0.5.dp, palette.stone.copy(alpha = 0.5f)),
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!renaming) {
                    Text(
                        text.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.text_options_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { renaming = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                    ) { Text(stringResource(R.string.text_rename), style = MaterialTheme.typography.titleMedium) }
                    OutlinedButton(
                        onClick = {
                            onRemove()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(50),
                    ) { Text(stringResource(R.string.text_remove), color = palette.text, style = MaterialTheme.typography.titleMedium) }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cancel), color = palette.textSecondary)
                    }
                } else {
                    Text(
                        stringResource(R.string.text_rename),
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                    )
                    val colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.sageDeep,
                        unfocusedBorderColor = palette.stone,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.sageDeep,
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(RoomText.MAX_TITLE) },
                        label = { Text(stringResource(R.string.text_heading_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it.take(RoomText.MAX_BODY) },
                        label = { Text(stringResource(R.string.text_line_label)) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            onRename(title, body)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                    ) { Text(stringResource(R.string.text_save), style = MaterialTheme.typography.titleMedium) }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cancel), color = palette.textSecondary)
                    }
                }
            }
        }
    }
}
