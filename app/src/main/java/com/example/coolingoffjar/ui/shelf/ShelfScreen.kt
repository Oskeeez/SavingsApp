package com.example.coolingoffjar.ui.shelf

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.ui.navigation.SubScreenScaffold

/** Skeleton only: the grid of completed jars arrives in stage 6. */
@Composable
fun ShelfScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = stringResource(R.string.shelf_title), onBack = onBack) {
        Text(
            text = stringResource(R.string.shelf_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
