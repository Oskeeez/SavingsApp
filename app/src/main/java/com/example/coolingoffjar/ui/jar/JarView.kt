package com.example.coolingoffjar.ui.jar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoinLayout
import com.example.coolingoffjar.domain.JarGeometry
import com.example.coolingoffjar.domain.JarRules
import com.example.coolingoffjar.ui.theme.JarTheme

/** The jar for [filledCount] of [notBuysPerJar]: visible coins = round(F * S), F = min(1, k / N). */
@Composable
fun JarView(filledCount: Int, notBuysPerJar: Int, modifier: Modifier = Modifier) {
    val fill = JarRules.fillLevel(filledCount, notBuysPerJar)
    JarCanvas(
        restingCoins = CoinLayout.visibleCoinCount(fill),
        contentDescription = stringResource(R.string.jar_description, filledCount, notBuysPerJar),
        modifier = modifier,
    )
}

/** Lower-level jar: shows exactly [restingCoins] resting coins. Stage 5 animates coins on top of this. */
@Composable
fun JarCanvas(restingCoins: Int, contentDescription: String, modifier: Modifier = Modifier) {
    val paints = remember(JarTheme.palette) { JarPaints(JarTheme.palette) }
    val body = remember { jarBodyPath() }

    Canvas(
        modifier
            .aspectRatio(JarGeometry.ASPECT)
            .semantics { this.contentDescription = contentDescription },
    ) {
        // Draw in width units, so one set of numbers serves every jar size.
        withTransform({ scale(size.width, size.width, Offset.Zero) }) {
            drawJar(paints, body, restingCoins)
        }
    }
}
