package com.example.coolingoffjar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.rememberAnimationsEnabled

/** "Freebie unlocked" card: slides up (or just fades if animations are off) over the glowing jar. */
@Composable
fun FreebieCard(onUseFreebie: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    val motion = rememberAnimationsEnabled()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(if (motion) 300 else 150)) +
            if (motion) slideInVertically(tween(450)) { it / 2 } else EnterTransition.None,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = palette.surface.copy(alpha = if (palette.isDark) 1f else 0.94f),
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.freebie_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.freebie_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onUseFreebie,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gold, contentColor = palette.onGold),
                ) {
                    Text(stringResource(R.string.freebie_use), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.freebie_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
