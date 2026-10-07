package com.example.coolingoffjar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.ui.components.JarProgress
import com.example.coolingoffjar.ui.components.SproutGlyph
import com.example.coolingoffjar.ui.components.TornPaperShape
import com.example.coolingoffjar.ui.theme.JarTheme

/**
 * The paper that rises over the shelf while it is zoomed in on the jar. The jar itself is the real one on the
 * shelf (the home screen zooms the shelf toward it), so this holds only the words and buttons, all centred:
 * the heading, the progress, Add, and the things being waited for (nothing at all when there are none).
 */
@Composable
fun JarPanel(
    jarFilled: Int,
    notBuysPerJar: Int,
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

    BoxWithConstraints(modifier) {
        val screenH = maxHeight
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = statusBar + 4.dp)
                .size(48.dp)
                .background(palette.background.copy(alpha = 0.8f), CircleShape),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scene_back), tint = palette.text)
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = screenH * 0.55f),
            shape = TornPaperShape(),
            color = palette.background,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(top = 26.dp, bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.scene_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    SproutGlyph(28.dp, palette.sageDeep)
                }
                Text(
                    stringResource(R.string.scene_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )

                JarProgress(filled = jarFilled, perJar = notBuysPerJar, modifier = Modifier.padding(top = 14.dp))

                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.sageDeep, contentColor = palette.onSage),
                ) {
                    Text("+   " + stringResource(R.string.home_add), style = MaterialTheme.typography.titleMedium)
                }

                if (items.isNotEmpty()) {
                    val collapsed = 3
                    val shown = if (expanded) items else items.take(collapsed)
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

/**
 * The paper that rises while the camera is close up on one of the user's things: its name and the day it was unlocked,
 * centred, with a way back.
 */
@Composable
fun ItemPanel(
    name: String,
    unlockedOn: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = JarTheme.palette
    BoxWithConstraints(modifier) {
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = statusBar + 4.dp)
                .size(48.dp)
                .background(palette.background.copy(alpha = 0.8f), CircleShape),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scene_back), tint = palette.text)
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = TornPaperShape(),
            color = palette.background,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 22.dp)
                    .padding(top = 30.dp, bottom = 28.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    SproutGlyph(26.dp, palette.sageDeep)
                }
                Text(
                    stringResource(R.string.item_unlocked_on, unlockedOn),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
