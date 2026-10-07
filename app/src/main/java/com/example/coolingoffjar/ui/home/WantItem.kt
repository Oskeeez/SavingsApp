package com.example.coolingoffjar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantStatus
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.rememberAnimationsEnabled

/**
 * A quiet card for one want. READY gets a subtle gold highlight; COOLING shows "X days left".
 * [animateIn] is true only for an item the user just added: it fades and slides in (~220-250 ms).
 * With system animations off it simply fades.
 */
@Composable
fun WantItem(want: Want, now: Long, animateIn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    val status = CoolOffRules.effectiveStatus(want, now)
    val ready = status == WantStatus.READY
    val motion = rememberAnimationsEnabled()

    val visible = remember { MutableTransitionState(!animateIn).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(if (motion) 220 else 150)) +
            if (motion) slideInVertically(tween(250)) { it / 4 } else EnterTransition.None,
    ) {
        val base = palette.surface.copy(alpha = palette.surfaceAlpha)
        Surface(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            shape = MaterialTheme.shapes.medium,
            color = if (ready) palette.gold.copy(alpha = 0.16f).compositeOver(base) else base,
            border = if (ready) BorderStroke(1.dp, palette.gold.copy(alpha = 0.75f)) else null,
            shadowElevation = 1.dp,
        ) {
            val tapLabel = stringResource(R.string.item_decide_action)
            Column(
                Modifier
                    .then(if (ready) Modifier.clickable(onClickLabel = tapLabel, role = Role.Button, onClick = onClick) else Modifier)
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(
                    want.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (ready) {
                        stringResource(R.string.item_ready)
                    } else {
                        val days = CoolOffRules.daysLeft(want.unlockAt, now)
                        pluralStringResource(R.plurals.item_days_left, days, days)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    // Gold-on-cream text fails contrast, so READY is signalled by the card highlight and a stronger text colour.
                    color = if (ready) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
