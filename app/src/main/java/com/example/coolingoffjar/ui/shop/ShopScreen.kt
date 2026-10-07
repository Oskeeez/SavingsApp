package com.example.coolingoffjar.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CatalogItem
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfCategory
import com.example.coolingoffjar.ui.components.CategoryChip
import com.example.coolingoffjar.ui.components.CoinChip
import com.example.coolingoffjar.ui.components.CoinIcon
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.shelf.BannerItem
import com.example.coolingoffjar.ui.shelf.ShelfBanner
import com.example.coolingoffjar.ui.shelf.artRes
import com.example.coolingoffjar.ui.theme.JarTheme

/** The shelf strip along the top of the shop: what you have bought lately, or a cosy default. */
internal fun bannerFor(recentOwnedIds: List<String>): List<BannerItem> {
    val xs = listOf(0.24f, 0.52f, 0.80f)
    val mine = recentOwnedIds.takeLast(3)
    val keys = if (mine.size >= 3) mine else (mine + listOf("cat_calico", "wooden_house", "framed_landscape").filter { it !in mine }).take(3)
    return keys.mapIndexed { i, key -> BannerItem(key, xs[i]) }
}

/**
 * Opened by tapping the gacha machine. Laid out after the "Cozy Shelf Shop" reference: a strip of your shelf
 * pokes in above, then category chips and a 3-column grid of little boxes, each with its coin price.
 */
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory),
) {
    val palette = JarTheme.palette
    val state by viewModel.state.collectAsStateWithLifecycle()
    var categoryName by rememberSaveable { mutableStateOf("") } // "" = All
    val category = ShelfCategory.entries.firstOrNull { it.name == categoryName }
    val visible = ShelfCatalog.items.filter { category == null || it.category == category }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(palette.background),
        contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                // Title row: back, title, balance.
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
                            Text(stringResource(R.string.shop_title), style = MaterialTheme.typography.headlineMedium, color = palette.text, fontWeight = FontWeight.Medium)
                            SproutGlyph(24.dp, palette.sageDeep)
                        }
                        Text(stringResource(R.string.shop_subtitle), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    CoinChip(
                        state.balance,
                        description = pluralStringResource(R.plurals.shop_balance_description, state.balance, state.balance),
                        large = true,
                    )
                }
                // The shelf pokes in above the shop.
                ShelfBanner(bannerFor(state.recentOwnedIds), Modifier.padding(top = 4.dp))
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CategoryChip(stringResource(R.string.shop_all), selected = category == null, onClick = { categoryName = "" })
                for (c in ShelfCategory.entries) {
                    CategoryChip(c.label, selected = category == c, onClick = { categoryName = c.name })
                }
            }
        }
        items(visible, key = { it.id }) { item ->
            Box(Modifier.padding(horizontal = 7.dp)) {
                ProductCard(
                    item = item,
                    owned = item.id in state.ownedIds,
                    affordable = state.balance >= item.cost,
                    onClick = { onOpenItem(item.id) },
                )
            }
        }
    }
}

/** One little box: the illustration, its name, and its price in coins (or "On your shelf"). */
@Composable
private fun ProductCard(item: CatalogItem, owned: Boolean, affordable: Boolean, onClick: () -> Unit) {
    val palette = JarTheme.palette
    val description =
        if (owned) stringResource(R.string.shop_card_owned_description, item.name)
        else pluralStringResource(R.plurals.shop_card_description, item.cost, item.name, item.cost)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface.copy(alpha = palette.surfaceAlpha),
        border = BorderStroke(0.5.dp, palette.stone.copy(alpha = 0.45f)),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(
            Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painterResource(artRes(item.artKey)), null,
                Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp).alpha(if (owned) 0.55f else 1f),
                contentScale = ContentScale.Fit,
            )
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                color = palette.text,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            if (owned) {
                Text(stringResource(R.string.shop_owned), style = MaterialTheme.typography.labelMedium, color = palette.sageDeep, fontWeight = FontWeight.SemiBold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CoinIcon(18.dp, Modifier.alpha(if (affordable) 1f else 0.55f))
                    Text(
                        item.cost.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (affordable) palette.text else palette.textSecondary,
                    )
                }
            }
        }
    }
}
