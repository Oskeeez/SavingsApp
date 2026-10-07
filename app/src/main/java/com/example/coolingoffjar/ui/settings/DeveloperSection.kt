package com.example.coolingoffjar.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.domain.DebugTime
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import java.time.ZoneId

/** Debug builds only. Lets you time-travel and shortcut the jar so every flow can be tried in minutes. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSection(viewModel: SettingsViewModel, debugOffsetMs: Long, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val simulatedNow = AppClock.now()

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = palette.surface.copy(alpha = palette.surfaceAlpha),
        border = BorderStroke(1.dp, palette.glassEdge),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.dev_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.dev_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.dev_clock), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (debugOffsetMs == 0L) stringResource(R.string.dev_clock_real, formatDate(simulatedNow))
                else stringResource(R.string.dev_clock_simulated, formatDate(simulatedNow)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DevButton("+1 day") { viewModel.shiftClockDays(1) }
                DevButton("+7 days") { viewModel.shiftClockDays(7) }
                DevButton("+30 days") { viewModel.shiftClockDays(30) }
                DevButton("−1 day") { viewModel.shiftClockDays(-1) }
                DevButton(stringResource(R.string.dev_pick_date)) { showDatePicker = true }
                DevButton(stringResource(R.string.dev_reset_clock)) { viewModel.resetClock() }
            }

            Text(stringResource(R.string.dev_data), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DevButton(stringResource(R.string.dev_add_samples)) { viewModel.addSampleWants() }
                DevButton(stringResource(R.string.dev_make_ready)) { viewModel.makeAllReady() }
                DevButton(stringResource(R.string.dev_add_coin)) { viewModel.addCoin() }
                DevButton(stringResource(R.string.dev_add_coins)) { viewModel.addCoins(5) }
                DevButton(stringResource(R.string.dev_complete_jar)) { viewModel.completeJar() }
                DevButton(stringResource(R.string.dev_reset_all)) { confirmReset = true }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DebugTime.pickerMillisFor(simulatedNow, ZoneId.systemDefault()),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::setClockToDate)
                    showDatePicker = false
                }) { Text(stringResource(R.string.dev_set_date)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.dev_reset_title)) },
            text = { Text(stringResource(R.string.dev_reset_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllData(); confirmReset = false }) { Text(stringResource(R.string.dev_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DevButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
