package com.example.coolingoffjar.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfCategory
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.ShelfSurface
import com.example.coolingoffjar.ui.components.CategoryChip
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.shelf.ShelfBanner
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * The storage box: the shop's layout, but showing only what you own. Things you put away wait here; tap one to take
 * it out and choose where it goes, or tap something on display to put it away.
 */
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory),
) {
    val palette = JarTheme.palette
    val state by viewModel.state.collectAsStateWithLifecycle()
    var categoryName by rememberSaveable { mutableStateOf("") }
    var takeOutId by rememberSaveable { mutableStateOf<String?>(null) }
    var putAwayId by rememberSaveable { mutableStateOf<String?>(null) }

    val ownedById = state.owned.associateBy { it.itemId }
    val mine = ShelfCatalog.items.filter { item ->
        item.id in ownedById && item.surface != ShelfSurface.DECOR && item.category != ShelfCategory.CORE
    }
    val categories = ShelfCategory.entries.filter { c -> mine.any { it.category == c } }
    val category = categories.firstOrNull { it.name == categoryName }
    val visible = mine.filter { category == null || it.category == category }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(palette.background),
        contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 4.dp, start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = palette.text)
                    }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.storage_title), style = MaterialTheme.typography.headlineMedium, color = palette.text, fontWeight = FontWeight.Medium)
                            SproutGlyph(24.dp, palette.sageDeep)
                        }
                        Text(stringResource(R.string.storage_subtitle), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
                ShelfBanner(bannerFor(state.recentOwnedIds), state.look, Modifier.padding(top = 4.dp))
                if (!state.text.visible) {
                    OutlinedButton(
                        onClick = viewModel::restoreText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                    ) { Text(stringResource(R.string.storage_text_back), color = palette.text) }
                }
            }
        }
        if (categories.size > 1) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    CategoryChip(stringResource(R.string.shop_all), selected = category == null, onClick = { categoryName = "" })
                    for (c in categories) CategoryChip(c.label, selected = category == c, onClick = { categoryName = c.name })
                }
            }
        }
        if (visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.storage_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
        items(visible, key = { it.id }) { item ->
            val stored = ownedById[item.id]?.stored == true
            Box(Modifier.padding(horizontal = 7.dp)) {
                ProductCard(
                    item = item,
                    owned = true,
                    affordable = true,
                    status = stringResource(if (stored) R.string.storage_in_storage else R.string.storage_on_display),
                    onClick = {
                        if (stored) takeOutId = item.id
                        else if (item.id !in ShelfLayout.STORAGE_BOXES) putAwayId = item.id
                    },
                )
            }
        }
    }

    // Taking something out: choose where it goes.
    val takeOut = takeOutId?.let { ShelfCatalog.find(it) }
    if (takeOut != null) {
        PlacementDialog(
            item = takeOut,
            owned = state.owned,
            look = state.look,
            jarFilled = 0,
            notBuysPerJar = 5,
            memoryJars = emptyList(),
            onConfirm = { slot, point ->
                viewModel.bringBack(takeOut.id, slot, point)
                takeOutId = null
            },
            onDismiss = { takeOutId = null },
        )
    }

    // Putting something on display away.
    val putAway = putAwayId?.let { ShelfCatalog.find(it) }
    if (putAway != null) {
        Dialog(onDismissRequest = { putAwayId = null }) {
            Surface(shape = RoundedCornerShape(28.dp), color = palette.background, shadowElevation = 6.dp) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.storage_put_away_title, putAway.name),
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.text,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            viewModel.store(putAway.id)
                            putAwayId = null
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                    ) { Text(stringResource(R.string.storage_put_away), style = MaterialTheme.typography.titleMedium) }
                    TextButton(onClick = { putAwayId = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cancel), color = palette.textSecondary)
                    }
                }
            }
        }
    }
}
