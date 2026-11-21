package com.bellatrixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.bellatrixos.widgets.render.WidgetContent

/**
 * One declarative OWL bar, rendered inside Bellatrix's own home bar rather than as a tile or a swiped panel.
 *
 * [name] is [com.bellatrixos.widgets.WidgetEngine.HOME_BAR] or
 * [com.bellatrixos.widgets.WidgetEngine.TASKBAR_BAR]. Reading specs/instances here means a file the user
 * saved reloads and redraws within a second; there's no separate screen, engine or launcher view involved.
 * A file that doesn't compile shows its error in place, like the bar for a broken style.toml, instead of
 * silently taking the user's layout away.
 */
@Composable
internal fun OwlBar(
    runtime: Runtime,
    name: String,
    modifier: Modifier = Modifier,
) {
    val engine = runtime.widgets
    val spec = engine.specs.firstOrNull { it.file.name == name } ?: return
    Box(modifier) {
        // A file with a compile error has no instance; keep the bar in place and say what's wrong, like
        // the config-error bar does for a broken style.toml, so the user's layout isn't silently dropped.
        val instance = engine.barInstance(name)
        if (instance != null) WidgetContent(instance, Modifier.matchParentSize())
        if (spec.error != null) {
            BasicText(
                "$name: ${spec.error}",
                Modifier
                    .fillMaxWidth()
                    .background(Palette.card)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = Mono.copy(color = Palette.red, fontSize = TextSize.Small),
            )
        }
    }
}
