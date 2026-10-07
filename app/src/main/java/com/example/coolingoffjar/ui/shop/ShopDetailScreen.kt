package com.example.coolingoffjar.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.PurchaseCheck
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfCategory
import com.example.coolingoffjar.domain.ShelfSurface
import com.example.coolingoffjar.ui.components.CoinChip
import com.example.coolingoffjar.ui.components.CoinIcon
import com.example.coolingoffjar.ui.components.PaperNote
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.components.TornPaperShape
import com.example.coolingoffjar.ui.shelf.BannerItem
import com.example.coolingoffjar.ui.shelf.ShelfBanner
import com.example.coolingoffjar.ui.shelf.artRes
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * One item, after the "Anthurium clarinervium" screen of the shop reference: the thing itself standing on a
 * shelf, a sheet of paper with its name, a cosy line about it, its price, the buy button, and a preview of how
 * it will look among your things. You choose exactly what you buy; nothing is random.
 */
@Composable
fun ShopDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onViewShelf: () -> Unit,
    viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory),
) {
    val item = ShelfCatalog.find(itemId)
    if (item == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val palette = JarTheme.palette
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val owned = item.id in state.ownedIds
    var placing by rememberSaveable { mutableStateOf(false) }
    val affordable = state.balance >= item.cost
    val placed = stringResource(R.string.shop_placed, item.name)
    val notEnough = stringResource(R.string.shop_not_enough_snackbar)

    LaunchedEffect(viewModel, item.id) {
        viewModel.events.collect { event ->
            when (event) {
                is ShopEvent.Bought -> if (event.itemId == item.id) snackbar.showSnackbar(placed)
                is ShopEvent.Refused -> if (event.reason is PurchaseCheck.NotEnoughCoins) snackbar.showSnackbar(notEnough)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(palette.background)) {
        val screenH = maxHeight
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // ---- The item, standing on a shelf in a sunny corner.
        Box(Modifier.fillMaxWidth().height(screenH * 0.52f)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF1E3CA), Color(0xFFE2CBA6)))))
            val boardY = screenH * 0.52f - 40.dp
            Box(
                Modifier.offset(y = boardY).fillMaxWidth().height(14.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFD8AB70), Color(0xFFB98550)))),
            )
            // Neighbours, a little cropped, like the reference.
            Image(painterResource(artRes("books_standing")), null, Modifier.align(Alignment.TopStart).offset(x = (-26).dp, y = boardY - 110.dp).size(width = 100.dp, height = 110.dp), contentScale = ContentScale.Fit)
            Image(painterResource(artRes("wooden_house")), null, Modifier.align(Alignment.TopEnd).offset(x = 22.dp, y = boardY - 84.dp).size(width = 80.dp, height = 84.dp), contentScale = ContentScale.Fit)
            // The item itself (a wall, floor or shelf is shown as a swatch card instead).
            if (item.surface == ShelfSurface.DECOR) {
                val cardW = (this@BoxWithConstraints.maxWidth * 0.5f).coerceAtMost(220.dp)
                val cardH = if (item.category == ShelfCategory.SHELVES) cardW * 1.2f else cardW * 0.8f
                Image(
                    painterResource(artRes(item.artKey)), contentDescription = item.name,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = boardY - cardH + 6.dp)
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = if (item.category == ShelfCategory.SHELVES) ContentScale.Fit else ContentScale.Crop,
                )
            } else {
                val itemHeight = (screenH * 0.34f).coerceAtMost(300.dp)
                val itemWidth = itemHeight * item.aspect
                val widthLimit = this@BoxWithConstraints.maxWidth * 0.72f
                val (w, h) = if (itemWidth > widthLimit) widthLimit to widthLimit / item.aspect else itemWidth to itemHeight
                Image(
                    painterResource(artRes(item.artKey)), contentDescription = item.name,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = boardY - h + 6.dp).size(w, h),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // Top controls.
        Row(
            Modifier.fillMaxWidth().padding(top = statusBar + 4.dp, start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).background(palette.background.copy(alpha = 0.8f), CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = palette.text)
            }
            CoinChip(
                state.balance,
                description = pluralStringResource(R.plurals.shop_balance_description, state.balance, state.balance),
                large = true,
            )
        }

        // ---- The paper.
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = screenH * 0.64f),
            shape = TornPaperShape(),
            color = palette.background,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(top = 26.dp, bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, style = MaterialTheme.typography.headlineSmall, color = palette.text, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    SproutGlyph(22.dp, palette.sageDeep)
                }
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoinIcon(30.dp)
                    Text(item.cost.toString(), style = MaterialTheme.typography.headlineSmall, color = palette.text)
                }

                val decor = item.surface == ShelfSurface.DECOR
                val inUse = decor && state.look.let { it.wall == item.id || it.floor == item.id || it.shelf == item.id }
                if (owned && decor) {
                    Button(
                        onClick = { viewModel.use(item.id) },
                        enabled = !inUse,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 56.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.sageDeep,
                            contentColor = palette.onSage,
                            disabledContainerColor = palette.beige,
                            disabledContentColor = palette.textSecondary,
                        ),
                    ) { Text(stringResource(if (inUse) R.string.shop_in_use else R.string.shop_use_this), style = MaterialTheme.typography.titleMedium) }
                } else if (owned) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 56.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = palette.beige, disabledContentColor = palette.textSecondary),
                    ) { Text(stringResource(R.string.shop_owned_button), style = MaterialTheme.typography.titleMedium) }
                    OutlinedButton(
                        onClick = onViewShelf,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(50),
                    ) { Text(stringResource(R.string.shop_view_shelf), color = palette.text) }
                } else {
                    Button(
                        onClick = { if (decor) viewModel.purchase(item.id) else placing = true },
                        enabled = affordable,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 56.dp),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.sageDeep,
                            contentColor = palette.onSage,
                            disabledContainerColor = palette.beige,
                            disabledContentColor = palette.textSecondary,
                        ),
                    ) {
                        Text(
                            if (affordable) pluralStringResource(R.plurals.shop_get_for, item.cost, item.cost)
                            else pluralStringResource(R.plurals.shop_need_more, item.cost - state.balance, item.cost - state.balance),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Text(
                    stringResource(R.string.shop_preview),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface.copy(alpha = palette.surfaceAlpha),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val previewLook = when (item.category) {
                        ShelfCategory.WALLS -> state.look.copy(wall = item.id)
                        ShelfCategory.SHELVES -> state.look.copy(shelf = item.id)
                        ShelfCategory.FLOORS -> state.look.copy(floor = item.id)
                        else -> state.look
                    }
                    val previewItems =
                        if (item.surface == ShelfSurface.DECOR) listOf(BannerItem("books_standing", 0.20f), BannerItem("cat_calico", 0.55f), BannerItem("bonsai", 0.84f))
                        else listOf(BannerItem("books_standing", 0.14f), BannerItem(item.artKey, 0.50f), BannerItem("cat_calico", 0.84f))
                    ShelfBanner(previewItems, previewLook, Modifier.padding(6.dp))
                }
                PaperNote(stringResource(R.string.shop_note), Modifier.padding(top = 16.dp), tilt = -1.5f)
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    // Buying asks where it should stand first: nothing is spent until a spot is chosen.
    if (placing && !owned) {
        PlacementDialog(
            item = item,
            owned = state.owned,
            look = state.look,
            jarFilled = 0,
            notBuysPerJar = 5,
            memoryJars = emptyList(),
            onConfirm = { slot, point ->
                placing = false
                viewModel.purchase(item.id, slot, point)
            },
            onDismiss = { placing = false },
        )
    }
}
