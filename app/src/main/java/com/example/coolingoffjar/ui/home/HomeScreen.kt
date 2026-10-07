package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.ui.jar.JarView
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import com.example.coolingoffjar.ui.util.rememberNowMillis

@Composable
fun HomeScreen(
    onOpenShelf: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var justAddedId by remember { mutableLongStateOf(-1L) }

    val now = rememberNowMillis(nextUnlockAt = state.wants.map { it.unlockAt }.filter { it > System.currentTimeMillis() }.minOrNull())
    val items = remember(state.wants, now) { CoolOffRules.sortForDisplay(state.wants, now) }

    val addedMessage = stringResource(R.string.add_snackbar)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.WantAdded -> {
                    justAddedId = event.want.id
                    snackbarHost.currentSnackbarData?.dismiss()
                    snackbarHost.showSnackbar(String.format(addedMessage, formatDate(event.want.unlockAt)))
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(JarTheme.palette.background)
            .safeDrawingPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val jarAreaHeight = maxHeight * 0.52f

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "jar") {
                    Box(Modifier.fillMaxWidth().height(jarAreaHeight), contentAlignment = Alignment.Center) {
                        if (state.isLoaded) {
                            JarView(
                                filledCount = state.jar.filledCount,
                                notBuysPerJar = state.settings.notBuysPerJar,
                                modifier = Modifier.fillMaxHeight(),
                            )
                        }
                    }
                }
                item(key = "add") {
                    Button(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarTheme.palette.gold,
                            contentColor = JarTheme.palette.onGold,
                        ),
                    ) {
                        Text(stringResource(R.string.home_add), style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (state.isLoaded && items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
                        )
                    }
                }
                items(items, key = { it.id }) { want ->
                    WantItem(want = want, now = now, animateIn = want.id == justAddedId)
                }
            }

            // Quiet top bar floats over the list.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenShelf) { Text(stringResource(R.string.home_shelf)) }
                TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.home_settings)) }
            }
        }

        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
    }

    if (showAddSheet) {
        AddWantSheet(
            coolOffDays = state.settings.coolOffDays,
            onDismiss = { showAddSheet = false },
            onConfirm = viewModel::addWant,
        )
    }
}
