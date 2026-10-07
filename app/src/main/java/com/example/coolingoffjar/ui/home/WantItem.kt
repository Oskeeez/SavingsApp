package com.example.coolingoffjar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantStatus
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.rememberAnimationsEnabled

/**
 * The design system's "item card": a beige card with a sprout tile, the name, and a quiet caption.
 * A READY item gets a gold outline and can be tapped to decide; the "..." menu removes an item (with undo).
 * [animateIn] is true only for an item the user just added: it fades and slides in (~220-250 ms), or just
 * fades when system animations are off.
 */
@Composable
fun WantItem(
    want: Want,
    now: Long,
    animateIn: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = JarTheme.palette
    val status = CoolOffRules.effectiveStatus(want, now)
    val ready = status == WantStatus.READY
    val motion = rememberAnimationsEnabled()
    var menuOpen by remember { mutableStateOf(false) }

    val visible = remember { MutableTransitionState(!animateIn).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(if (motion) 220 else 150)) +
            if (motion) slideInVertically(tween(250)) { it / 4 } else EnterTransition.None,
    ) {
        val tapLabel = stringResource(R.string.item_decide_action)
        Surface(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = false) {},
            shape = RoundedCornerShape(18.dp),
            color = palette.surface.copy(alpha = palette.surfaceAlpha),
            border = if (ready) BorderStroke(1.5.dp, palette.gold) else BorderStroke(0.5.dp, palette.stone.copy(alpha = 0.45f)),
        ) {
            Row(
                Modifier
                    .then(if (ready) Modifier.clickable(onClickLabel = tapLabel, role = Role.Button, onClick = onClick) else Modifier)
                    .heightIn(min = 68.dp)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(48.dp).background(palette.beige, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { SproutGlyph(26.dp, palette.sageDeep) }

                Column(Modifier.weight(1f)) {
                    Text(want.name, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2)
                    val daysSince = ((now - want.createdAt) / CoolOffRules.DAY_MS).toInt().coerceAtLeast(0)
                    val added = if (daysSince == 0) stringResource(R.string.item_added_today)
                    else pluralStringResource(R.plurals.item_added_days_ago, daysSince, daysSince)
                    Text(
                        text = if (ready) stringResource(R.string.item_ready_short)
                        else added + " · " + pluralStringResource(R.plurals.item_days_left, CoolOffRules.daysLeft(want.unlockAt, now), CoolOffRules.daysLeft(want.unlockAt, now)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ready) palette.text else palette.textSecondary,
                        fontWeight = if (ready) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }

                Box {
                    val more = stringResource(R.string.item_more_options, want.name)
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp).semantics { contentDescription = more }) {
                        Text("···", style = MaterialTheme.typography.titleMedium, color = palette.textSecondary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.item_remove)) },
                            onClick = { menuOpen = false; onRemove() },
                        )
                    }
                }
            }
        }
    }
}
