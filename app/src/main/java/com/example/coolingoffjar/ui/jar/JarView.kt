package com.example.coolingoffjar.ui.jar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.JarRules
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * STAGE 3 PLACEHOLDER: a flat jar with a flat gold level, enough to lay out Home.
 * Stage 4 replaces the drawing (glass, lid, coins) but keeps this signature and the accessibility text.
 */
@Composable
fun JarView(filledCount: Int, notBuysPerJar: Int, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    val fill = JarRules.fillLevel(filledCount, notBuysPerJar)
    val description = stringResource(R.string.jar_description, filledCount, notBuysPerJar)

    Canvas(
        modifier
            .aspectRatio(0.8f)
            .semantics { contentDescription = description },
    ) {
        val lidH = size.height * 0.10f
        val bodyTop = lidH + size.height * 0.03f
        val bodySize = Size(size.width, size.height - bodyTop)
        val corner = CornerRadius(size.width * 0.18f)

        val body = Path().apply {
            addRoundRect(RoundRect(0f, bodyTop, bodySize.width, size.height, corner))
        }
        clipPath(body) {
            drawRect(palette.glassTint, Offset(0f, bodyTop), bodySize)
            val fillH = bodySize.height * fill
            drawRect(palette.gold.copy(alpha = 0.85f), Offset(0f, size.height - fillH), Size(size.width, fillH))
        }
        drawPath(body, palette.glassEdge, style = Stroke(width = 3f))
        drawRoundRect(
            palette.goldShadow,
            topLeft = Offset(size.width * 0.12f, 0f),
            size = Size(size.width * 0.76f, lidH),
            cornerRadius = CornerRadius(8f),
        )
    }
}
