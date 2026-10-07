package com.example.coolingoffjar.ui.jar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.JarArt
import com.example.coolingoffjar.ui.theme.JarTheme

/** Width / height of the jar pictures (they are all cut to the same size, so the jar never shifts between states). */
const val JarAspect = 186f / 215f

/** The jar picture for state 0 (empty) .. 5 (full). */
@DrawableRes
fun jarStateRes(state: Int): Int = when (state.coerceIn(0, JarArt.FULL)) {
    0 -> R.drawable.jar_state_0
    1 -> R.drawable.jar_state_1
    2 -> R.drawable.jar_state_2
    3 -> R.drawable.jar_state_3
    4 -> R.drawable.jar_state_4
    else -> R.drawable.jar_state_5
}

/**
 * The jar, from the supplied watercolour artwork. The picture shown follows the coin count (see [JarArt]), and it
 * is the full picture exactly when the jar is full. [glow] (0..1) adds a soft golden halo behind it for the
 * celebration. (Coin drop animation comes later.)
 */
@Composable
fun AssetJar(
    filledCount: Int,
    notBuysPerJar: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    glow: Float = 0f,
) {
    val palette = JarTheme.palette
    val state = JarArt.stateFor(filledCount, notBuysPerJar)
    Box(
        modifier
            .aspectRatio(JarAspect)
            .drawBehind {
                if (glow > 0.01f) {
                    val r = size.width * 0.95f
                    val c = Offset(size.width / 2f, size.height * 0.55f)
                    drawCircle(
                        Brush.radialGradient(
                            0f to palette.gold.copy(alpha = 0.55f * glow),
                            1f to palette.gold.copy(alpha = 0f),
                            center = c,
                            radius = r,
                        ),
                        radius = r,
                        center = c,
                    )
                }
            }
            .semantics { this.contentDescription = contentDescription },
    ) {
        Image(
            painter = painterResource(jarStateRes(state)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
