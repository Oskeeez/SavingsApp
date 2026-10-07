package com.example.coolingoffjar.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.ui.theme.JarTheme
import kotlin.math.roundToInt

/**
 * A settings card with a big value, a coin-shaped slider thumb, and − / + buttons for exact steps.
 * The slider only saves when released, so dragging does not churn storage (or complete a jar early
 * when it is the coins-per-jar setting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderSetting(
    title: String,
    value: Int,
    range: IntRange,
    valueLabel: @Composable (Int) -> String,
    description: String,
    decreaseDescription: String,
    increaseDescription: String,
    onValueCommitted: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = JarTheme.palette
    // Local while dragging; resets whenever the saved value changes.
    var live by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val shown = live.roundToInt().coerceIn(range.first, range.last)

    fun commit(newValue: Int) {
        val clamped = newValue.coerceIn(range.first, range.last)
        live = clamped.toFloat()
        if (clamped != value) onValueCommitted(clamped)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = palette.surface.copy(alpha = palette.surfaceAlpha),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                valueLabel(shown),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StepButton("−", decreaseDescription) { commit(shown - 1) }
                Slider(
                    value = live,
                    onValueChange = { live = it },
                    onValueChangeFinished = { commit(live.roundToInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    modifier = Modifier.weight(1f),
                    thumb = { CoinThumb() },
                    colors = SliderDefaults.colors(
                        activeTrackColor = palette.gold,
                        inactiveTrackColor = palette.glassEdge.copy(alpha = if (palette.isDark) 0.35f else 0.8f),
                    ),
                )
                StepButton("+", increaseDescription) { commit(shown + 1) }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The slider thumb is a little gold coin. */
@Composable
private fun CoinThumb() {
    val palette = JarTheme.palette
    Box(
        Modifier
            .size(32.dp)
            .shadow(3.dp, CircleShape)
            .background(
                Brush.radialGradient(
                    0f to palette.goldHighlight,
                    0.55f to palette.gold,
                    1f to palette.goldShadow,
                ),
                CircleShape,
            )
            .border(1.5.dp, palette.goldShadow, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(18.dp).border(1.dp, palette.goldShadow.copy(alpha = 0.6f), CircleShape))
    }
}
