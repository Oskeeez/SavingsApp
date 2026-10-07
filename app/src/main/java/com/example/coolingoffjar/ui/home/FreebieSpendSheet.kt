package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantStatus
import com.example.coolingoffjar.ui.components.wantIconRes
import com.example.coolingoffjar.ui.theme.JarTheme
import kotlinx.coroutines.launch

/**
 * Shown when you spend a freebie: "What will you spend it on?". Your waiting list is the menu: tap one to select it,
 * then confirm. Whatever you pick is simply marked as bought, with no guilt and no amounts. "Something else" spends
 * it on anything not on the list, and "Not yet" keeps it for later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreebieSpendSheet(
    items: List<Want>,
    now: Long,
    onSpend: (wantId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = JarTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable { mutableLongStateOf(-1L) }

    fun spend(wantId: Long?) {
        onSpend(wantId)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.spend_title),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(if (items.isEmpty()) R.string.spend_subtitle_empty else R.string.spend_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            )

            Column(
                Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (want in items) {
                    val isSelected = want.id == selectedId
                    val ready = CoolOffRules.effectiveStatus(want, now) == WantStatus.READY
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface.copy(alpha = palette.surfaceAlpha),
                        border = if (isSelected) BorderStroke(2.dp, palette.sageDeep) else BorderStroke(0.5.dp, palette.stone.copy(alpha = 0.45f)),
                        modifier = Modifier.semantics { selected = isSelected },
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.RadioButton) { selectedId = want.id }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Image(painterResource(wantIconRes(want.iconKey)), null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f)) {
                                Text(want.name, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2)
                                val days = CoolOffRules.daysLeft(want.unlockAt, now)
                                Text(
                                    if (ready) stringResource(R.string.item_ready_short) else pluralStringResource(R.plurals.item_days_left, days, days),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { spend(selectedId.takeIf { it >= 0 }) },
                enabled = selectedId >= 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.sageDeep,
                    contentColor = palette.onSage,
                    disabledContainerColor = palette.sageDeep.copy(alpha = 0.35f),
                    disabledContentColor = palette.onSage.copy(alpha = 0.7f),
                ),
            ) { Text(stringResource(R.string.spend_confirm), style = MaterialTheme.typography.titleMedium) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.spend_not_yet), color = palette.textSecondary)
                }
                TextButton(onClick = { spend(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.spend_something_else), color = palette.text)
                }
            }
        }
    }
}
