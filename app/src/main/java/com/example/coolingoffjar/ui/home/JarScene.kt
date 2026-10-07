package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.ui.components.PaperNote
import com.example.coolingoffjar.ui.components.JarProgress
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.components.TornPaperShape
import com.example.coolingoffjar.ui.jar.AssetJar
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * What you see when you tap the jar on the shelf: a closer look at it, like the left screen of the
 * "how the app should scroll" reference. The jar stands on a desk beside a few of your things, and a
 * sheet of torn paper below holds the heading, the progress, the Add button and what you are waiting on.
 */
@Composable
fun JarScene(
    jarFilled: Int,
    notBuysPerJar: Int,
    glow: Float,
    items: List<Want>,
    now: Long,
    justAddedId: Long,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onItemClick: (Want) -> Unit,
    onRemove: (Want) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = JarTheme.palette
    var expanded by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier
            .background(palette.background)
            // Swallow every gesture so nothing falls through to the shelf underneath while this is up.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val screenH = maxHeight
        val screenW = maxWidth
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val panelTop = screenH * 0.56f

        // ---- The room: wall, a little shelf with a note, the desk and what is on it.
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFF1E3CA), Color(0xFFE5D0AE))),
                ),
            )
            SunShafts(Modifier.fillMaxSize())

            val boardY = statusBar + 118.dp
            val jarTop = boardY + 34.dp
            val jarHeight = (panelTop - jarTop - 6.dp).coerceIn(150.dp, screenH * 0.38f)
            val jarFeet = jarTop + jarHeight
            val deskTop = jarFeet - 44.dp

            // Little shelf with books, the cat and the wooden house, and the note beside them.
            Box(
                Modifier.offset(y = boardY).fillMaxWidth().height(12.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFD8AB70), Color(0xFFB98550)))),
            )
            Prop(R.drawable.item_books_standing, screenW * 0.10f, boardY, 88.dp)
            Prop(R.drawable.item_cat_calico, screenW * 0.36f, boardY, 54.dp)
            Prop(R.drawable.item_wooden_house, screenW * 0.58f, boardY, 62.dp)
            PaperNote(
                stringResource(R.string.scene_note),
                Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = statusBar + 28.dp).width(96.dp),
            )

            // The desk.
            Image(
                painterResource(R.drawable.scene_wood), null,
                Modifier.offset(y = deskTop).fillMaxWidth().height(screenH - deskTop),
                contentScale = ContentScale.FillBounds,
            )
            Box(Modifier.offset(y = deskTop).fillMaxWidth().height(3.dp).background(Color(0x55FFF3DC)))
            Tablecloth(Modifier.offset(y = deskTop + 6.dp).align(Alignment.TopCenter).width(screenW * 0.78f).height(jarHeight * 0.24f))

            // Pen cup on a stack of books, a framed print behind, and the jar in the middle.
            Prop(R.drawable.item_framed_flowers, screenW * 0.86f, deskTop + 20.dp, 104.dp)
            Prop(R.drawable.item_books_stack, screenW * 0.14f, deskTop + 38.dp, 54.dp)
            Prop(R.drawable.item_pen_cup, screenW * 0.14f, deskTop + 38.dp - 40.dp, 72.dp)

            Box(
                Modifier.align(Alignment.TopCenter).offset(y = jarTop).height(jarHeight),
            ) {
                AssetJar(
                    filledCount = jarFilled,
                    notBuysPerJar = notBuysPerJar,
                    contentDescription = stringResource(R.string.jar_description, jarFilled, notBuysPerJar),
                    modifier = Modifier.height(jarHeight),
                    glow = glow,
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp, top = statusBar + 4.dp)
                    .size(48.dp)
                    .background(palette.background.copy(alpha = 0.8f), CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scene_back), tint = palette.text)
            }
        }

        // ---- The paper.
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = screenH * 0.66f),
            shape = TornPaperShape(),
            color = palette.background,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(top = 26.dp, bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.scene_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                    )
                    SproutGlyph(28.dp, palette.sageDeep)
                }
                Text(
                    stringResource(R.string.scene_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )

                JarProgress(filled = jarFilled, perJar = notBuysPerJar, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))

                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                ) {
                    Text("+   " + stringResource(R.string.home_add), style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    stringResource(R.string.scene_waiting),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                if (items.isEmpty()) {
                    Text(
                        stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                } else {
                    val collapsed = 3
                    val shown = if (expanded) items else items.take(collapsed)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (want in shown) {
                            WantItem(
                                want = want,
                                now = now,
                                animateIn = want.id == justAddedId,
                                onClick = { onItemClick(want) },
                                onRemove = { onRemove(want) },
                            )
                        }
                        if (items.size > collapsed) {
                            TextButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                            ) {
                                Text(
                                    if (expanded) stringResource(R.string.home_show_less)
                                    else stringResource(R.string.home_show_more, items.size - collapsed),
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A prop standing at [centerX] with its feet at [feetY] in the scene (screen coordinates from the top). */
@Composable
private fun BoxScope.Prop(drawable: Int, centerX: Dp, feetY: Dp, height: Dp) {
    val painter = painterResource(drawable)
    val ratio = painter.intrinsicSize.let { it.width / it.height }
    val w = height * ratio
    Image(
        painter, null,
        Modifier.align(Alignment.TopStart).offset(x = centerX - w / 2, y = feetY - height).size(w, height),
        contentScale = ContentScale.Fit,
    )
}

/** Faint diagonal bands of light across the wall, like afternoon sun through a window. */
@Composable
private fun SunShafts(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun band(x0: Float, width: Float, alpha: Float) {
            val p = Path().apply {
                moveTo(x0, 0f); lineTo(x0 + width, 0f)
                lineTo(x0 + width - w * 0.55f, h * 0.7f); lineTo(x0 - w * 0.55f, h * 0.7f); close()
            }
            drawPath(p, Color.White.copy(alpha = alpha))
        }
        band(w * 0.15f, w * 0.18f, 0.16f)
        band(w * 0.50f, w * 0.12f, 0.12f)
        band(w * 0.80f, w * 0.20f, 0.14f)
    }
}

/** The blue-and-cream gingham cloth the jar stands on, seen at a slant. */
@Composable
private fun Tablecloth(modifier: Modifier) {
    val palette = JarTheme.palette
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val slant = w * 0.10f
        val cloth = Path().apply {
            moveTo(slant, 0f); lineTo(w - slant, 0f); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(cloth, Color(0xFFF4EEDF))
        val stripe = palette.paleBlue.copy(alpha = if (palette.isDark) 0.45f else 0.85f)
        val cols = 9
        for (i in 0 until cols step 2) {
            val x0 = w * i / cols
            val x1 = w * (i + 1) / cols
            val strip = Path().apply {
                moveTo(slant + (x0 / w) * (w - 2 * slant), 0f); lineTo(slant + (x1 / w) * (w - 2 * slant), 0f)
                lineTo(x1, h); lineTo(x0, h); close()
            }
            drawPath(strip, stripe)
        }
        val rows = 3
        for (j in 0 until rows step 2) {
            drawRect(stripe.copy(alpha = stripe.alpha * 0.7f), Offset(0f, h * j / rows), Size(w, h / rows))
        }
        drawPath(cloth, Color(0x22000000), style = Stroke(width = 1.2f))
    }
}
