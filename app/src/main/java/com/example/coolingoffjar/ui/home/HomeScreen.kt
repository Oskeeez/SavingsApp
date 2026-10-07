package com.example.coolingoffjar.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.WantStatus
import com.example.coolingoffjar.ui.shelf.ShelfBoard
import com.example.coolingoffjar.ui.shelf.ShelfFloorColor
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import com.example.coolingoffjar.ui.util.rememberAnimationsEnabled
import com.example.coolingoffjar.ui.util.rememberNowMillis

/**
 * Home is the shelf: a tall bookcase you scroll through, with your jar standing on it, the gacha machine
 * (Shop) and the desk clock (Settings). Tap the jar and the view zooms in to the jar scene. No bars, no
 * buttons: the objects are the navigation.
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
    val motion = rememberAnimationsEnabled()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var decisionWantId by rememberSaveable { mutableLongStateOf(-1L) }
    var memoryJarId by rememberSaveable { mutableLongStateOf(-1L) }
    var spendJarId by rememberSaveable { mutableLongStateOf(-1L) }
    var justAddedId by remember { mutableLongStateOf(-1L) }
    var jarSceneOpen by rememberSaveable { mutableStateOf(false) }
    var jarBounds by remember { mutableStateOf<Rect?>(null) } // the jar on screen when it was tapped

    val now = rememberNowMillis(nextUnlockAt = state.wants.map { it.unlockAt }.filter { it > AppClock.now() }.minOrNull())
    val items = remember(state.wants, now) { CoolOffRules.sortForDisplay(state.wants, now) }
    val readyCount = items.count { CoolOffRules.effectiveStatus(it, now) == WantStatus.READY }

    val addedMessage = stringResource(R.string.add_snackbar)
    val boughtMessage = stringResource(R.string.decision_bought_noted)
    val removedMessage = stringResource(R.string.item_removed)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHost.currentSnackbarData?.dismiss()
            when (event) {
                is HomeEvent.WantAdded -> {
                    justAddedId = event.want.id
                    snackbarHost.showSnackbar(String.format(addedMessage, formatDate(event.want.unlockAt)))
                }
                HomeEvent.BoughtNoted -> snackbarHost.showSnackbar(boughtMessage)
                is HomeEvent.WantRemoved -> {
                    val result = snackbarHost.showSnackbar(removedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreWant(event.want)
                }
            }
        }
    }

    // While the freebie card is up, the jar shown is the one that was just completed (full and glowing).
    val celebration = state.celebration
    val shownJar = celebration ?: state.jar
    LaunchedEffect(celebration?.id) {
        if (celebration != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    BackHandler(enabled = jarSceneOpen) { jarSceneOpen = false }

    // 0 = looking at the whole shelf, 1 = zoomed right in on the jar. One smooth move in both directions.
    val zoom by animateFloatAsState(
        targetValue = if (jarSceneOpen) 1f else 0f,
        animationSpec = tween(if (motion) 650 else 120, easing = FastOutSlowInEasing),
        label = "jarZoom",
    )

    val scroll = rememberScrollState()

    BoxWithConstraints(Modifier.fillMaxSize().background(JarTheme.palette.background)) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        // Where the jar should end up when zoomed in: centred, in the upper part of the screen above the panel.
        val targetCenter = Offset(screenW / 2f, screenH * 0.24f)
        val targetHeight = screenH * 0.30f
        val bounds = jarBounds
        val fullScale = if (bounds != null && bounds.height > 1f) (targetHeight / bounds.height).coerceIn(1f, 5f) else 1f

        // ---- The shelf, scrollable. Zooming scales the whole picture about the jar and slides the jar to the target.
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (bounds != null) {
                        val s = 1f + (fullScale - 1f) * zoom
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(bounds.center.x / size.width, bounds.center.y / size.height)
                        translationX = (targetCenter.x - bounds.center.x) * zoom
                        translationY = (targetCenter.y - bounds.center.y) * zoom
                    }
                }
                .then(if (jarSceneOpen) Modifier.clearAndSetSemantics { } else Modifier)
                .verticalScroll(scroll, enabled = !jarSceneOpen),
        ) {
            Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
            ShelfBoard(
                owned = state.owned,
                jarFilled = shownJar.filledCount,
                notBuysPerJar = state.settings.notBuysPerJar,
                readyCount = readyCount,
                jarGlow = if (celebration != null) 1f else 0f,
                memoryJars = state.completedJars,
                onOpenJar = { rect ->
                    if (rect != null) {
                        jarBounds = rect
                        jarSceneOpen = true
                    }
                },
                onOpenShop = onOpenShop,
                onOpenSettings = onOpenSettings,
                onMemoryJar = { memoryJarId = it.id },
                onMoveItem = viewModel::moveItem,
            )
            // The shelf stands on a wooden floor: carry it under the gesture bar.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(Color(ShelfFloorColor)),
            )
        }

        // ---- While zoomed in, a tap anywhere above the panel goes back to the whole shelf.
        if (jarSceneOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { jarSceneOpen = false }
                    .clearAndSetSemantics { },
            )
        }

        // ---- The words and buttons, rising as the jar fills the view.
        if (zoom > 0.001f) {
            JarPanel(
                jarFilled = shownJar.filledCount,
                notBuysPerJar = state.settings.notBuysPerJar,
                items = items,
                now = now,
                justAddedId = justAddedId,
                onBack = { jarSceneOpen = false },
                onAdd = { showAddSheet = true },
                onItemClick = { decisionWantId = it.id },
                onRemove = viewModel::removeWant,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = zoom
                        translationY = (1f - zoom) * size.height * 0.2f
                    },
            )
        }

        if (celebration != null) {
            FreebieCard(
                onUseFreebie = { spendJarId = celebration.id },
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

    // Using a freebie first asks what it is being spent on.
    val spendJar = (listOfNotNull(celebration) + state.completedJars).firstOrNull { it.id == spendJarId }
    if (spendJar != null && spendJar.hasUnusedFreebie) {
        FreebieSpendSheet(
            items = items,
            now = now,
            onSpend = { wantId ->
                viewModel.useFreebie(spendJar.id, wantId)
                spendJarId = -1L
            },
            onDismiss = { spendJarId = -1L },
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
                    TextButton(onClick = { spendJarId = memoryJar.id; memoryJarId = -1L }) {
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
