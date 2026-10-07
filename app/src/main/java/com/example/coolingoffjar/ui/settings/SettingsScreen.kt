package com.example.coolingoffjar.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.SettingsRules
import com.example.coolingoffjar.ui.navigation.SubScreenScaffold

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SubScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoaded) {
                SliderSetting(
                    title = stringResource(R.string.settings_cool_off_title),
                    value = state.settings.coolOffDays,
                    range = SettingsRules.MIN_COOL_OFF_DAYS..SettingsRules.MAX_COOL_OFF_DAYS,
                    valueLabel = { days -> pluralStringResource(R.plurals.settings_days, days, days) },
                    description = stringResource(R.string.settings_cool_off_desc),
                    decreaseDescription = stringResource(R.string.settings_cool_off_less),
                    increaseDescription = stringResource(R.string.settings_cool_off_more),
                    onValueCommitted = viewModel::setCoolOffDays,
                )
                SliderSetting(
                    title = stringResource(R.string.settings_jar_title),
                    value = state.settings.notBuysPerJar,
                    range = SettingsRules.MIN_NOT_BUYS_PER_JAR..SettingsRules.MAX_NOT_BUYS_PER_JAR,
                    valueLabel = { n -> pluralStringResource(R.plurals.settings_coins, n, n) },
                    description = stringResource(R.string.settings_jar_desc),
                    decreaseDescription = stringResource(R.string.settings_jar_less),
                    increaseDescription = stringResource(R.string.settings_jar_more),
                    onValueCommitted = viewModel::setNotBuysPerJar,
                )
            }

            Text(
                text = stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )

            if (viewModel.isDebuggable && state.isLoaded) {
                DeveloperSection(viewModel = viewModel, debugOffsetMs = state.debugOffsetMs)
            }
        }
    }
}
