package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import kotlinx.coroutines.launch

/**
 * Two taps total: "Add something I want" opens this sheet with the keyboard up, then one confirm.
 * Shows "Ready on [date]" as a live confirmation of exactly what will be saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWantSheet(
    coolOffDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val palette = JarTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var name by rememberSaveable { mutableStateOf("") }
    val readyOn = formatDate(CoolOffRules.unlockAt(AppClock.now(), coolOffDays))

    fun confirm() {
        if (name.isBlank()) return
        onConfirm(name)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
    ) {
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
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(CoolingOffRepository.MAX_NAME_LENGTH) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.add_hint)) },
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.gold,
                    cursorColor = palette.goldShadow,
                    unfocusedBorderColor = palette.glassEdge,
                ),
            )
            Text(
                stringResource(R.string.add_ready_on, readyOn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = ::confirm,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.extraLarge,
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
