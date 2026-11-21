package com.bellatrixos.widgets

import android.content.Context
import com.bellatrixos.Runtime
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.upgrade.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The two declarative layout files that skin Bellatrix's own home and taskbar surfaces.
 *
 * These are ordinary OWL panel files in the widgets folder, so the launcher's file observer reloads them
 * within a second and `owl-check <file>` previews them like any other file. What makes them special is
 * where they render: not swiped open as a panel, but over the native home area and over the native
 * taskbar, at the size of those real views. Flex/Box/Anchor and the `anchor` property (owl 24) place
 * modules there. Bellatrix's desktop, windows, start menu and taskbar stay exactly where they are; an
 * OWL file with a transparent background leaves their shipped look alone, while an opaque one covers it.
 *
 * They are seeded from the templates below so a fresh install already has a working Flex/Box/Anchor
 * example; editing either file just opens what is there.
 */
object OwlBars {
    /** Guest paths, which [Runtime.edit] maps back to [LauncherData.widgetsDir] through proot's binds. */
    fun guestPath(name: String) = "/root/${WidgetEngine.GUEST_DIR}/$name"

    /** The starter home surface: a full-size Box + Anchor with hidden modules pinned to the edges. */
    val homeTemplate =
        """
        // HOME SURFACE — Bellatrix's native home, skinned with OWL.
        // Long-press empty home → "edit home surface" opens this file in Bellatrix's own editor.
        // The root Box fills the real home area above the taskbar. Keep its background transparent to
        // leave Bellatrix's icons and wallpaper alone, or set one to cover them with your own surface.
        // Flex flows modules into a row; Anchor and `anchor` pin them to the edges. Save to apply within a second.
        panel "home" {
          meta { owl: 24; description: "OWL skin for Bellatrix home" }

          source now = clock every 1s
          source bat = android.battery

          Box {
            fill: true
            background: transparent

            Anchor {
              fill: true

              // Switch visible to true, or add your own nodes, to draw on the home surface.
              Flex { anchor: topStart; gap: 12; visible: false
                Text { text: "Bellatrix"; style: title; color: accent }
                Text { text: fmtTime(now, "HH:mm"); style: small }
              }

              Flex { anchor: topEnd; gap: 12; visible: false
                Text { text: bat ? "${'$'}{round(bat.level)}%" : ""; style: small; color: muted }
              }
            }
          }
        }
        """.trimIndent() + "\n"

    /** The starter taskbar surface: a full-size Box + Anchor over the native taskbar. */
    val taskbarTemplate =
        """
        // TASKBAR SURFACE — Bellatrix's native taskbar, skinned with OWL.
        // Long-press empty home → "edit taskbar surface" opens this file in Bellatrix's own editor.
        // There is no second taskbar: the root Box's background, radius, border, borderColor and padding
        // restyle the real native taskbar views. A visible Flex/Row/Column gap spaces task buttons, and a
        // visible Text's color tints native icons. Keep background transparent to leave Bellatrix's
        // shipped look untouched; save to apply.
        panel "taskbar" {
          meta { owl: 24; description: "OWL skin for Bellatrix taskbar" }

          Box {
            fill: true
            background: transparent

            Anchor {
              fill: true

              Flex { anchor: bottomStart; gap: 8; visible: false
                Text { text: "tasks"; style: small; color: muted }
              }

              Flex { anchor: bottomEnd; gap: 8; visible: false
                Text { text: "▸"; style: small }
              }
            }
          }
        }
        """.trimIndent() + "\n"

    private fun template(name: String) =
        when (name) {
            WidgetEngine.HOME_BAR -> homeTemplate
            WidgetEngine.TASKBAR_BAR -> taskbarTemplate
            else -> null
        }

    /**
     * Creates [name] in the widgets dir if the user hasn't made it yet, then opens it in Bellatrix's editor.
     * The write is on IO so the file observer sees a complete file; the editor opens back on the caller's main thread.
     */
    suspend fun edit(
        context: Context,
        runtime: Runtime,
        name: String,
    ) {
        val body = template(name) ?: return
        withContext(Dispatchers.IO) {
            val file = File(LauncherData.widgetsDir(context), name)
            if (!file.exists()) runCatching { writeAtomically(file, body.toByteArray()) }
        }
        runtime.edit(guestPath(name))
    }

    /** A fresh widget template, opened in the editor: Flex/Box/Anchor on Bellatrix's one home page. */
    val widgetTemplate =
        """
        // A home-screen widget. Bellatrix has one hard-coded home page; this tile lands there.
        // Flex, Box and Anchor are live here, so you can test them without an agent. Save and it shows within a second.
        widget "new widget" {
          page: 1
          order: 999
          size: 2x2
          minSize: 1x1

          meta { owl: 24; description: "My widget" }

          source now = clock every 1s

          Box {
            fill: width
            Anchor {
              fill: width
              Flex { anchor: topStart; gap: 6
                Text { text: "new widget"; style: title; color: accent }
              }
              Flex { anchor: bottomEnd
                Text { text: fmtTime(now, "HH:mm"); style: small; color: muted }
              }
            }
          }
        }
        """.trimIndent() + "\n"

    /** A fresh face template: a tile on the same one home page, using the same declarative layout nodes. */
    @Suppress("UNUSED_PARAMETER")
    fun faceTemplate(page: Int) =
        """
        // A face is a tile on Bellatrix's one home page, not a second screen. Edit and save: it updates within a second.
        face "new face" {
          page: 1
          order: 999
          size: 3x3
          minSize: 2x2

          meta { owl: 24; description: "My face" }

          source now = clock every 1s

          Box {
            fill: width
            Anchor {
              fill: width
              Flex { anchor: topStart
                Text { text: fmtTime(now, "HH:mm"); style: huge; color: accent }
              }
              Flex { anchor: bottomEnd
                Text { text: "new face"; style: muted }
              }
            }
          }
        }
        """.trimIndent() + "\n"

    /** Creates a new, uniquely named file from [body] and opens it in Bellatrix's editor. */
    private suspend fun create(
        context: Context,
        runtime: Runtime,
        prefix: String,
        body: String,
    ) {
        val path =
            withContext(Dispatchers.IO) {
                val dir = LauncherData.widgetsDir(context)
                var n = 1
                while (File(dir, "$prefix-$n.owl").exists()) n++
                val file = File(dir, "$prefix-$n.owl")
                runCatching { writeAtomically(file, body.toByteArray()) }
                guestPath(file.name)
            }
        runtime.edit(path)
    }

    /** Creates a new widget file and opens it in Bellatrix's editor. */
    suspend fun newWidget(
        context: Context,
        runtime: Runtime,
    ) = create(context, runtime, "widget", widgetTemplate)

    /** Creates a new face on [page] and opens it in Bellatrix's editor. */
    suspend fun newFace(
        context: Context,
        runtime: Runtime,
        page: Int,
    ) = create(context, runtime, "face", faceTemplate(page))
}
