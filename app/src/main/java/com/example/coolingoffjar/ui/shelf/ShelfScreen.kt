package com.example.coolingoffjar.ui.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoinLayout
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.ui.jar.JarCanvas
import com.example.coolingoffjar.ui.navigation.SubScreenScaffold
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate

@Composable
fun ShelfScreen(
    onBack: () -> Unit,
    viewModel: ShelfViewModel = viewModel(factory = ShelfViewModel.Factory),
) {
    val jars by viewModel.jars.collectAsStateWithLifecycle()
    var freebieJarId by remember { mutableStateOf<Long?>(null) }

    SubScreenScaffold(title = stringResource(R.string.shelf_title), onBack = onBack) {
        val list = jars
        when {
            list == null -> Unit
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.shelf_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }) { jar ->
                    ShelfJarCard(jar, onClick = { freebieJarId = jar.id })
                }
            }
        }
    }

    // Tapping a jar with an unused freebie offers to spend it (honour-based, no amount recorded).
    val dialogJar = jars?.firstOrNull { it.id == freebieJarId }
    if (dialogJar != null && dialogJar.hasUnusedFreebie) {
        AlertDialog(
            onDismissRequest = { freebieJarId = null },
            title = { Text(stringResource(R.string.shelf_freebie_title)) },
            text = { Text(stringResource(R.string.shelf_freebie_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.useFreebie(dialogJar.id); freebieJarId = null }) {
                    Text(stringResource(R.string.freebie_use))
                }
            },
            dismissButton = {
                TextButton(onClick = { freebieJarId = null }) { Text(stringResource(R.string.shelf_freebie_not_yet)) }
            },
        )
    }
}

@Composable
private fun ShelfJarCard(jar: Jar, onClick: () -> Unit) {
    val palette = JarTheme.palette
    val completedOn = jar.completedAt?.let { formatDate(it) }.orEmpty()
    val clickable = jar.hasUnusedFreebie

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        color = palette.surface.copy(alpha = palette.surfaceAlpha),
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier
                .then(if (clickable) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JarCanvas(
                restingCoins = CoinLayout.slots.size,
                contentDescription = stringResource(R.string.shelf_jar_description, completedOn),
                modifier = Modifier.fillMaxWidth(0.62f),
            )
            Text(
                stringResource(R.string.shelf_completed_on, completedOn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (jar.hasUnusedFreebie) {
                // Clear but gentle: a soft gold pill, dark text for contrast.
                Surface(shape = MaterialTheme.shapes.extraLarge, color = palette.gold) {
                    Text(
                        stringResource(R.string.shelf_badge_unused),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            } else {
                Text(
                    stringResource(R.string.shelf_freebie_used),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
