package com.example.coolingoffjar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.ProgressArt
import com.example.coolingoffjar.ui.theme.JarTheme
import kotlin.math.PI
import kotlin.math.sin

/*
 * Small building blocks taken from the "Cooling-Off Jar UI Design System" board (crops of the board are in
 * Assets/UI): the coin chip, category chips, progress dots, the torn-paper panel edge and the pinned note.
 */

/** The little sprout coin from the artwork. */
@Composable
fun CoinIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.item_coin), contentDescription = null, modifier = modifier.size(size), contentScale = ContentScale.Fit)
}

/** Coin + number in a soft pill: the balance in the shop, or a price on a card. */
@Composable
fun CoinChip(amount: Int, modifier: Modifier = Modifier, description: String? = null, large: Boolean = false) {
    val palette = JarTheme.palette
    Row(
        modifier
            .background(palette.beige.copy(alpha = if (palette.isDark) 1f else 0.9f), RoundedCornerShape(50))
            .padding(horizontal = if (large) 14.dp else 10.dp, vertical = if (large) 8.dp else 4.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(if (large) 28.dp else 20.dp)
        Text(
            amount.toString(),
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = palette.text,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** All / Plants / Decor ... : the selected one is a filled sage pill, the rest are quiet text. */
@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .background(if (selected) palette.sageDeep else Color.Transparent, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) palette.onSage else palette.textSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/** The progress picture for state 0..5 (dots filled), from the supplied progress-bar artwork. */
@androidx.annotation.DrawableRes
fun progressRes(state: Int): Int = when (state.coerceIn(0, 5)) {
    0 -> R.drawable.progress_0
    1 -> R.drawable.progress_1
    2 -> R.drawable.progress_2
    3 -> R.drawable.progress_3
    4 -> R.drawable.progress_4
    else -> R.drawable.progress_5
}

/**
 * "3 of 5": the progress-bar artwork (five dots joined by a line) with the real count beside it. The artwork has five
 * dots whatever the jar size, so the picture follows the fraction while the words always say the true numbers.
 */
@Composable
fun JarProgress(filled: Int, perJar: Int, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    val state = ProgressArt.stateFor(filled, perJar)
    val description = stringResource(R.string.scene_progress, filled, perJar)
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
    ) {
        Image(
            painterResource(progressRes(state)), null,
            Modifier.weight(1f, fill = false).widthIn(max = 260.dp).fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Text(description, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    }
}

/** Cream panel with a hand-torn top edge, like the paper sheet over the jar scene on the reference. */
class TornPaperShape(private val bumpsPerWidth: Float = 14f, private val depth: Dp = 7.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val d = with(density) { depth.toPx() }
        val path = Path().apply {
            moveTo(0f, d * 0.5f)
            val steps = 80
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = size.width * t
                // two overlaid waves so the edge looks torn rather than scalloped; always within 0..d
                val y = d * (0.5f + 0.30f * sin(t * bumpsPerWidth * 2 * PI).toFloat() + 0.16f * sin(t * bumpsPerWidth * 3.7f * PI + 1.3f).toFloat())
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** A small pinned paper note, slightly tilted: "Good things take time." */
@Composable
fun PaperNote(text: String, modifier: Modifier = Modifier, tilt: Float = -2f) {
    val palette = JarTheme.palette
    Surface(
        modifier = modifier.rotate(tilt),
        shape = RoundedCornerShape(3.dp),
        color = if (palette.isDark) Color(0xFF4A4034) else Color(0xFFF7EBD2),
        shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, palette.stone.copy(alpha = 0.5f)),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = palette.text)
        }
    }
}

/** The little two-leaf sprout used on coins, tiles and headings throughout the artwork. */
@Composable
fun SproutGlyph(size: Dp, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        val leaves = Path().apply {
            moveTo(12f * k, 13f * k)
            quadraticTo(5f * k, 12f * k, 4f * k, 5f * k)
            quadraticTo(11f * k, 5f * k, 12f * k, 13f * k)
            moveTo(12f * k, 12f * k)
            quadraticTo(19f * k, 11f * k, 20f * k, 3f * k)
            quadraticTo(13f * k, 4f * k, 12f * k, 12f * k)
        }
        drawPath(leaves, color)
        drawLine(color, Offset(12f * k, 21f * k), Offset(12f * k, 11f * k), strokeWidth = 1.6f * k, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
