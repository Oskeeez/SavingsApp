package com.example.coolingoffjar.ui.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.ui.navigation.SubScreenScaffold
import com.example.coolingoffjar.ui.theme.JarTheme

/** Placeholder entries until the real shop (catalogue, prices, purchasing) is built. */
private class PreviewProduct(val name: String, @DrawableRes val image: Int)

private val PreviewProducts = listOf(
    PreviewProduct("Anthurium clarinervium", R.drawable.item_anthurium),
    PreviewProduct("Monstera adansonii", R.drawable.item_monstera),
    PreviewProduct("Cozy lamp", R.drawable.item_mushroom_lamp),
    PreviewProduct("Film camera", R.drawable.item_film_camera),
    PreviewProduct("Snow globe", R.drawable.item_snow_globe),
    PreviewProduct("Calico cat", R.drawable.item_cat_calico),
    PreviewProduct("World globe", R.drawable.item_world_globe),
    PreviewProduct("Book stack", R.drawable.item_books_stack),
    PreviewProduct("Pothos", R.drawable.item_pothos_potted),
    PreviewProduct("Wooden house", R.drawable.item_wooden_house),
    PreviewProduct("Reed diffuser", R.drawable.item_reed_diffuser),
    PreviewProduct("Framed print", R.drawable.item_framed_landscape),
)

/** Opened by tapping the gacha machine on the shelf. For now: a look at the items, nothing to buy yet. */
@Composable
fun ShopScreen(onBack: () -> Unit) {
    val palette = JarTheme.palette
    SubScreenScaffold(title = stringResource(R.string.shop_title), onBack = onBack) {
        Text(
            stringResource(R.string.shop_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(PreviewProducts) { product ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = palette.surface.copy(alpha = palette.surfaceAlpha),
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Image(
                            painter = painterResource(product.image),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            product.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
