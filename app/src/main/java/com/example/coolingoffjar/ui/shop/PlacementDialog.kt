package com.example.coolingoffjar.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CatalogItem
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.SlotRef
import com.example.coolingoffjar.ui.shelf.PlacementMode
import com.example.coolingoffjar.ui.shelf.ShelfBoard
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * Full-screen "Where should it go?": your own shelf, with a dashed "+" on every empty spot. Tap the spot you want
 * (the item appears there so you can see it), then "Place it here". The spot suggested first is the next free one.
 */
@Composable
fun PlacementDialog(
    item: CatalogItem,
    owned: List<OwnedItem>,
    onConfirm: (SlotRef) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = JarTheme.palette
    val firstFree = remember(owned) { ShelfLayout.nextFreeSlot(ShelfLayout.resolve(owned).map { it.slotRef }.toSet()) }
    var tier by rememberSaveable { mutableStateOf(firstFree.tier) }
    var slot by rememberSaveable { mutableStateOf(firstFree.slot) }
    val selected = SlotRef(tier, slot)
    val scroll = rememberScrollState()
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = palette.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = maxWidth
                val density = LocalDensity.current

                // Start the view at the suggested spot so it is not off-screen on a tall shelf.
                LaunchedEffect(Unit) {
                    val unit = with(density) { (width / ShelfGeometry.ART_WIDTH.toFloat()).toPx() }
                    val feet = ShelfGeometry.standLine(firstFree.tier, ShelfLayout.tiersNeeded(ShelfLayout.resolve(owned)) + 1) * unit
                    scroll.scrollTo((feet - with(density) { 330.dp.toPx() }).toInt().coerceAtLeast(0))
                }

                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Spacer(Modifier.height(statusBar + 84.dp))
                    ShelfBoard(
                        owned = owned,
                        jarFilled = 0,
                        notBuysPerJar = 5,
                        readyCount = 0,
                        jarGlow = 0f,
                        memoryJars = emptyList(),
                        onOpenJar = {},
                        onOpenShop = {},
                        onOpenSettings = {},
                        onMemoryJar = {},
                        placement = PlacementMode(item.artKey, selected) { tier = it.tier; slot = it.slot },
                    )
                    Spacer(Modifier.height(96.dp + navBar))
                }

                // Header.
                Surface(Modifier.align(Alignment.TopCenter).fillMaxWidth(), color = palette.background.copy(alpha = 0.94f), shadowElevation = 3.dp) {
                    Row(
                        Modifier.padding(top = statusBar + 6.dp, start = 20.dp, end = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.place_title), style = MaterialTheme.typography.titleLarge, color = palette.text, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.place_subtitle, item.name), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.place_cancel), tint = palette.text)
                        }
                    }
                }

                // Confirm.
                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = palette.background.copy(alpha = 0.96f), shadowElevation = 6.dp) {
                    Button(
                        onClick = { onConfirm(selected) },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp + navBar).fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                    ) {
                        Text(stringResource(R.string.place_confirm), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
