package com.bellatrixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bellatrixos.Runtime
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.widgets.render.WidgetScreen

/**
 * One declarative OWL surface drawn over a Bellatrix surface that is still there.
 *
 * [name] is [com.bellatrixos.widgets.WidgetEngine.HOME_BAR] or
 * [com.bellatrixos.widgets.WidgetEngine.TASKBAR_BAR]. These are `panel` files: their nodes live in
 * [com.bellatrixos.widgets.lang.Program.screen], not [com.bellatrixos.widgets.lang.Program.root], so
 * they are drawn with [WidgetScreen] at whatever size the host gives this composable. The host is the
 * real home or taskbar view: a transparent OWL file leaves Bellatrix's own look untouched, while an
 * opaque background or nodes from the file draw on top of it. There is no second launcher screen or
 * engine; the file observer reloads the instance through [Runtime] within a second of a save.
 */
@Composable
internal fun OwlSurface(
    runtime: Runtime,
    name: String,
    modifier: Modifier = Modifier,
) {
    val engine = runtime.widgets
    // Reading specs and instances here makes a saved file redraw the surface by itself.
    val error = engine.specs.firstOrNull { it.file.name == name }?.error
    Box(modifier) {
        val instance = engine.barInstance(name)
        if (instance != null) WidgetScreen(instance, Modifier.fillMaxSize())
        // A broken file says so over the native surface instead of silently losing the user's layout.
        if (error != null) {
            BasicText(
                "$name: $error",
                Modifier
                    .fillMaxWidth()
                    .background(Palette.card)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = Mono.copy(color = Palette.red, fontSize = TextSize.Small),
            )
        }
    }
}
