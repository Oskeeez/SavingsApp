package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.WantIcons
import com.example.coolingoffjar.ui.components.wantIconLabel
import com.example.coolingoffjar.ui.components.wantIconRes
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import kotlinx.coroutines.launch

/**
 * "Add something I want": a name, a picture for it, and one button. Tapping the picture swaps the sheet to the
 * "Choose an icon" grid (after the design system's selection list) and back. Shows "Ready on [date]" live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWantSheet(
    coolOffDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconKey: String) -> Unit,
) {
    val palette = JarTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var name by rememberSaveable { mutableStateOf("") }
    var iconKey by rememberSaveable { mutableStateOf(WantIcons.DEFAULT) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val readyOn = formatDate(CoolOffRules.unlockAt(AppClock.now(), coolOffDays))

    fun confirm() {
        if (name.isBlank()) return
        onConfirm(name, iconKey)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
    ) {
        if (picking) {
            IconPicker(
                selected = iconKey,
                onPick = { iconKey = it; picking = false },
                onClose = { picking = false },
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.add_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.text,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.add_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val change = stringResource(R.string.add_choose_icon_action, wantIconLabel(iconKey))
                    Image(
                        painter = painterResource(wantIconRes(iconKey)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clickable(role = Role.Button, onClickLabel = null, onClick = { picking = true })
                            .semantics { contentDescription = change },
                        contentScale = ContentScale.Fit,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(CoolingOffRepository.MAX_NAME_LENGTH) },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.add_hint)) },
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { confirm() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.sageDeep,
                            cursorColor = palette.sageDeep,
                            unfocusedBorderColor = palette.stone,
                        ),
                    )
                }
                Text(
                    stringResource(R.string.add_ready_on, readyOn),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = ::confirm,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.sageDeep,
                        contentColor = palette.onSage,
                        disabledContainerColor = palette.sageDeep.copy(alpha = 0.35f),
                        disabledContentColor = palette.onSage.copy(alpha = 0.7f),
                    ),
                ) {
                    Text(stringResource(R.string.add_confirm), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** "Choose an icon": a four-column grid of the icon tiles, the current one outlined, with a close button. */
@Composable
private fun IconPicker(selected: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    val palette = JarTheme.palette
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.add_choose_icon),
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.add_choose_icon_close), tint = palette.text)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (rowKeys in WantIcons.all.chunked(4)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (key in rowKeys) {
                        val isSelected = key == selected
                        val label = wantIconLabel(key)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, palette.sageDeep, RoundedCornerShape(18.dp)) else Modifier,
                                )
                                .clickable(role = Role.Button, onClick = { onPick(key) })
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(wantIconRes(key)),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().padding(2.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }
        }
    }
}
