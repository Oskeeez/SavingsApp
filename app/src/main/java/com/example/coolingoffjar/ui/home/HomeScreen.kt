package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.WantStatus
import com.example.coolingoffjar.ui.jar.JarView
import com.example.coolingoffjar.ui.shelf.ShelfBoard
import com.example.coolingoffjar.ui.shelf.ShelfFloorColor
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import com.example.coolingoffjar.ui.util.rememberNowMillis

/** Fraction of the screen the jar part of Home fills; the rest of the first view is the top of the shelf. */
private const val HomeShare = 0.84f

/**
 * Home is one tall room, not two pages: the jar and its controls on top, the shelf directly below.
 * The top of the shelf is already in view at launch, so scrolling is the obvious thing to do; the clock
 * and gacha machine standing on it are how you reach Settings and the Shop. No bars, no buttons.
 */
@Composable
fun HomeScreen(
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var decisionWantId by rememberSaveable { mutableLongStateOf(-1L) }
    var memoryJarId by rememberSaveable { mutableLongStateOf(-1L) }
    var justAddedId by remember { mutableLongStateOf(-1L) }
    var listExpanded by rememberSaveable { mutableStateOf(false) }

    val now = rememberNowMillis(nextUnlockAt = state.wants.map { it.unlockAt }.filter { it > AppClock.now() }.minOrNull())
    val items = remember(state.wants, now) { CoolOffRules.sortForDisplay(state.wants, now) }

    val addedMessage = stringResource(R.string.add_snackbar)
    val boughtMessage = stringResource(R.string.decision_bought_noted)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHost.currentSnackbarData?.dismiss()
            when (event) {
                is HomeEvent.WantAdded -> {
                    justAddedId = event.want.id
                    snackbarHost.showSnackbar(String.format(addedMessage, formatDate(event.want.unlockAt)))
                }
                HomeEvent.BoughtNoted -> snackbarHost.showSnackbar(boughtMessage)
            }
        }
    }

    // While the freebie card is up, show the jar that was just completed (full and glowing).
    val celebration = state.celebration
    val shownJar = celebration ?: state.jar
    LaunchedEffect(celebration?.id) {
        if (celebration != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val scroll = rememberScrollState()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(JarTheme.palette.background),
    ) {
        val viewport = maxHeight

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // ---- The jar part: sized so the shelf's top edge always peeks in at the bottom of the first view.
            val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapsedCount = 2
            val visibleCount = if (listExpanded) items.size else minOf(items.size, collapsedCount)
            val showMoreRow = items.size > collapsedCount
            val listHeight = if (items.isEmpty()) 96.dp else 76.dp * visibleCount + (if (showMoreRow) 44.dp else 0.dp)
            val chrome = 64.dp /* add button */ + 24.dp /* gaps */ + listHeight + 20.dp
            val jarHeight = (viewport * HomeShare - statusBar - chrome).coerceAtLeast(220.dp)

            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport * HomeShare)
                    .padding(top = statusBar),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(jarHeight).padding(top = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoaded) {
                        JarView(
                            filledCount = shownJar.filledCount,
                            notBuysPerJar = state.settings.notBuysPerJar,
                            modifier = Modifier.fillMaxHeight(),
                            glow = if (celebration != null) 1f else 0f,
                        )
                    }
                }
                Button(
                    onClick = { showAddSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp).fillMaxWidth().heightIn(min = 64.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarTheme.palette.gold,
                        contentColor = JarTheme.palette.onGold,
                    ),
                ) {
                    Text(stringResource(R.string.home_add), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                if (state.isLoaded && items.isEmpty()) {
                    Text(
                        stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                    )
                }
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (want in items.take(visibleCount)) {
                        WantItem(
                            want = want,
                            now = now,
                            animateIn = want.id == justAddedId,
                            onClick = { decisionWantId = want.id },
                        )
                    }
                    if (showMoreRow) {
                        TextButton(
                            onClick = { listExpanded = !listExpanded },
                            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                        ) {
                            Text(
                                if (listExpanded) stringResource(R.string.home_show_less)
                                else stringResource(R.string.home_show_more, items.size - collapsedCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ---- The shelf, continuing straight on below.
            ShelfBoard(
                onOpenShop = onOpenShop,
                onOpenSettings = onOpenSettings,
                memoryJars = state.completedJars,
                onMemoryJar = { memoryJarId = it.id },
            )
            // The shelf stands on a wooden floor: carry it under the gesture bar.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(Color(ShelfFloorColor)),
            )
        }

        if (celebration != null) {
            FreebieCard(
                onUseFreebie = { viewModel.useFreebie(celebration.id) },
                onLater = viewModel::dismissCelebration,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }

        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (showAddSheet) {
        AddWantSheet(
            coolOffDays = state.settings.coolOffDays,
            onDismiss = { showAddSheet = false },
            onConfirm = viewModel::addWant,
        )
    }

    // Decision sheet: only for a want that is still open and really READY at this moment.
    val deciding = state.wants.firstOrNull { it.id == decisionWantId }
    if (deciding != null && CoolOffRules.effectiveStatus(deciding, now) == WantStatus.READY) {
        DecisionSheet(
            want = deciding,
            onNotBuying = { viewModel.notBuying(deciding.id) },
            onStillWant = { viewModel.stillWant(deciding.id) },
            onDismiss = { decisionWantId = -1L },
        )
    }

    // A completed jar on the shelf: a memory, and the place to spend a freebie you said "Later" to.
    val memoryJar = state.completedJars.firstOrNull { it.id == memoryJarId }
    if (memoryJar != null) {
        val completedOn = memoryJar.completedAt?.let { formatDate(it) }.orEmpty()
        AlertDialog(
            onDismissRequest = { memoryJarId = -1L },
            title = {
                Text(
                    if (memoryJar.hasUnusedFreebie) stringResource(R.string.shelf_freebie_title)
                    else stringResource(R.string.memory_jar_title),
                )
            },
            text = {
                Text(
                    if (memoryJar.hasUnusedFreebie) stringResource(R.string.shelf_freebie_body)
                    else stringResource(R.string.memory_jar_body, completedOn),
                )
            },
            confirmButton = {
                if (memoryJar.hasUnusedFreebie) {
                    TextButton(onClick = { viewModel.useFreebie(memoryJar.id); memoryJarId = -1L }) {
                        Text(stringResource(R.string.freebie_use))
                    }
                } else {
                    TextButton(onClick = { memoryJarId = -1L }) { Text(stringResource(R.string.ok)) }
                }
            },
            dismissButton = {
                if (memoryJar.hasUnusedFreebie) {
                    TextButton(onClick = { memoryJarId = -1L }) { Text(stringResource(R.string.shelf_freebie_not_yet)) }
                }
            },
        )
    }
}
