package com.example.coolingoffjar.ui.jar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.CoinLayout
import com.example.coolingoffjar.domain.JarGeometry
import com.example.coolingoffjar.domain.JarRules
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.rememberAnimationsEnabled

private class DropPlan(val from: Int, val to: Int, val staggerMs: Float, val fallMs: Float)

/**
 * The jar for [filledCount] of [notBuysPerJar]: visible coins = round(F * S), F = min(1, k / N).
 * When more coins are due than are showing, they drop in one after another (never an instant jump up).
 * With system animations off they simply appear. [glow] (0..1) fades in a golden glow behind the jar.
 */
@Composable
fun JarView(filledCount: Int, notBuysPerJar: Int, modifier: Modifier = Modifier, glow: Float = 0f) {
    val target = CoinLayout.visibleCoinCount(JarRules.fillLevel(filledCount, notBuysPerJar))
    val motion = rememberAnimationsEnabled()
    val haptic = LocalHapticFeedback.current

    var settled by remember { mutableIntStateOf(target) } // coins at rest
    var plan by remember { mutableStateOf<DropPlan?>(null) }
    val clock = remember { Animatable(0f) }

    LaunchedEffect(target, motion) {
        if (target <= settled || !motion) {
            plan = null
            settled = target
            return@LaunchedEffect
        }
        val from = settled
        val count = target - from
        val stagger = minOf(60f, 1_200f / count) // keep big clusters (small N) from taking forever
        val fall = 650f
        val total = fall + stagger * (count - 1)
        plan = DropPlan(from, target, stagger, fall)
        clock.snapTo(0f)
        clock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // one light tick when the cluster lands
        settled = target
        plan = null
    }

    val glowLevel = animateFloatAsState(glow, tween(if (motion) 700 else 150), label = "jarGlow")

    JarCanvas(
        restingCoins = settled,
        contentDescription = stringResource(R.string.jar_description, filledCount, notBuysPerJar),
        modifier = modifier,
        drop = { plan?.let { CoinDrop(it.from, it.to, clock.value, it.staggerMs, it.fallMs) } },
        glow = { glowLevel.value },
    )
}

/** Lower-level jar: [restingCoins] at rest, plus optional [drop] in flight and [glow], read at draw time. */
@Composable
fun JarCanvas(
    restingCoins: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    drop: () -> CoinDrop? = { null },
    glow: () -> Float = { 0f },
) {
    val palette = JarTheme.palette
    val paints = remember(palette) { JarPaints(palette) }
    val body = remember { jarBodyPath() }

    Canvas(
        modifier
            .aspectRatio(JarGeometry.ASPECT)
            .semantics { this.contentDescription = contentDescription },
    ) {
        // Draw in width units, so one set of numbers serves every jar size.
        withTransform({ scale(size.width, size.width, Offset.Zero) }) {
            drawJar(paints, body, restingCoins, drop(), glow())
        }
    }
}
