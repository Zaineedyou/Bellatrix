package com.bellatrixos.widgets

import com.bellatrixos.linux.Agent
import com.bellatrixos.linux.Distro
import com.bellatrixos.linux.agentInvocation
import com.bellatrixos.theme.StyleStore
import com.bellatrixos.widgets.lang.OwlKind

/** What agents are told about dependencies when they write or change a widget. */
private val DEPENDENCIES: String
    get() =
        "Declare every package the file's commands need in requires { ${Distro.active.manager}: [...] } and install them with " +
            "${Distro.active.installHint}."

private val PACKAGE_TOOL: String get() = Distro.active.installHint

/** How agents see their work: they can't see the phone. */
private const val CHECK =
    "After saving, run owl-check <file>: it prints the errors and the path of a PNG with live data (a widget's tile and its screen, if it has one; an app's view); look at it to check the layout."

/** Starts [agentBin] in the widgets dir with a first prompt. */
private fun agentCommand(
    agentBin: String,
    prompt: String,
    readOnly: Boolean = false,
) = "cd ~/${WidgetEngine.GUEST_DIR} && ${agentInvocation(agentBin, prompt, readOnly)}; exec bash"

/** Session about Bellatrix themes: an agent creates or edits local theme folders in ~/.config/bellatrix/themes. */
fun editThemeCommand(agentBin: String) =
    "cd ~/.config/bellatrix/themes && " +
        agentInvocation(
            agentBin,
            "Ask me what to change about my Bellatrix theme. A theme is a folder here with colors.toml " +
                "(background, foreground, accent, muted, and color0..color15) and an optional backgrounds/ folder. " +
                "Read ~/.config/bellatrix/style/defaults.toml for the color names. Change or add folders directly; " +
                "the launcher rescans them within a second. Then pick the theme in Settings → Style.",
        ) + "; exec bash"

/** What each kind of owl file is, for the agent: where it shows up on the phone. */
private fun OwlKind.what() =
    when (this) {
        OwlKind.Widget -> "a home-screen widget (widget \"name\" { }, a tile on my home screen)"
        OwlKind.Face -> "a face (face \"name\" { }: a tile on Bellatrix's one hard-coded home page, like a widget, as docs/faces.md says)"
        OwlKind.App -> "an app (app \"name\" { }: it shows up in my app drawer and opens full screen; give it an icon: as docs/apps.md says)"
        OwlKind.Panel ->
            "a panel (panel \"name\" { }: swiping up on my home screen opens it as my app drawer, or swiping down opens it from the top, " +
                "as docs/panels.md says). Ask me which swipe it's for, and once it works, pick it for that swipe by setting " +
                "gestures.swipe-up or gestures.swipe-down to its file name in ~/${StyleStore.GUEST_DIR}/style.toml. " +
                "Two names are NOT swipe panels: home.owl and taskbar.owl draw over Bellatrix's real home and taskbar views " +
                "(transparent by default, so the native look stays; use Flex/Box/Anchor and an opaque background to skin them); " +
                "if you mean one of those, edit that file in place rather than picking a gesture"
        OwlKind.Keybar ->
            "a terminal key bar (keybar \"name\" { }: the bar over the soft keyboard while I type in a terminal, as docs/keybar.md " +
                "says). The launcher shows ${WidgetEngine.KEYBAR}, so change that file rather than making a new one"
    }

/** A session in the widgets dir that creates an owl file of [kind]; [page] is ignored: home is one page. */
@Suppress("UNUSED_PARAMETER")
fun newOwlCommand(
    agentBin: String,
    kind: OwlKind,
    page: Int? = null,
) = agentCommand(
    agentBin,
    "Read README.md, then ask me what to create: ${kind.what()}. " +
        "Bellatrix home is one hard-coded page; ignore any `page:` in the file and put the tile there. " +
        "$DEPENDENCIES $CHECK",
)

/** An agent session about one widget: its file, the language docs and its error log. */
fun editWidgetCommand(
    agentBin: String,
    spec: WidgetSpec,
    problem: String? = null,
): String {
    val file = spec.file.name
    val what =
        when (spec.kind) {
            OwlKind.Widget -> "home-screen widget"
            OwlKind.Face -> "face"
            OwlKind.App -> "app"
            OwlKind.Panel -> "panel"
            OwlKind.Keybar -> "terminal key bar"
        }
    val prompt =
        if (problem != null) {
            "The $what \"${spec.name}\" in $file has a problem: $problem Read README.md for the widget language " +
                "and $file, find out what's wrong, and fix it; ask me before changing the system beyond installing packages. $CHECK"
        } else {
            "I want to change the $what \"${spec.name}\" in $file. Read README.md for the widget language " +
                "and $file, and run owl-check $file to see it and its problems. $DEPENDENCIES $CHECK Saved changes show up on my " +
                "phone within seconds. Ask me what to change."
        }
    return agentCommand(agentBin, prompt)
}

/** A widget whose declared packages failed to install: the agent gets the package manager's output. */
fun fixDependenciesCommand(
    agentBin: String,
    spec: WidgetSpec,
    log: String,
) = editWidgetCommand(
    agentBin,
    spec,
    "installing its requirements (${spec.program?.requires.orEmpty().entries.joinToString { (m, p) -> "$m: ${p.joinToString(" ")}" }}) failed. " +
        "The last lines from $PACKAGE_TOOL were:\n$log\n",
)

/** A terminal tile that opens straight into [agent] in the home directory, and falls back to a shell when it exits. */
fun agentWidget(agent: Agent) =
    """
    widget "${agent.bin}" {
      size: 6x6
      minSize: 3x4
      padding: 0

      meta { owl: 2; description: "${agent.name} in a terminal on the home screen" }

      // Tap to type; tap outside or press Back to let go. When ${agent.name} exits, the shell stays.
      Terminal { command: 'cd ~ && ${agent.command}; exec bash' }
    }
    """.trimIndent() + "\n"
