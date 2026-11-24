package com.bellatrixos.linux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The .desktop and AppImage cases that decide whether a Vesktop-like app reaches the drawer and opens. */
class LinuxAppsTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun desktop(
        name: String,
        text: String,
    ): File = File(tmp.root, name).also { it.writeText(text) }

    @Test
    fun parsesVesktopDesktopEntryAndAddsWaylandFlags() {
        val app =
            parseDesktopEntry(
                desktop(
                    "vesktop.desktop",
                    """
                    [Desktop Entry]
                    Type=Application
                    Name=Vesktop
                    Exec=vesktop %U
                    Icon=io.github.Vesktop.Vesktop
                    StartupWMClass=vesktop
                    Path=/opt/Vesktop
                    """.trimIndent(),
                ),
            )!!
        assertEquals("vesktop", app.id)
        assertEquals("Vesktop", app.name)
        assertEquals("/opt/Vesktop", app.dir)
        assertTrue(app.electronLike)
        assertEquals(
            "cd '/opt/Vesktop' && vesktop --ozone-platform=wayland --enable-features=UseOzonePlatform --no-sandbox",
            app.command,
        )
    }

    @Test
    fun hidesNoDisplayAndKeepsLocalizedName() {
        assertNull(
            parseDesktopEntry(
                desktop(
                    "hidden.desktop",
                    "[Desktop Entry]\nType=Application\nName=Hidden\nExec=hidden\nNoDisplay=true",
                ),
            ),
        )
        val app =
            parseDesktopEntry(
                desktop(
                    "notes.desktop",
                    "[Desktop Entry]\nType=Application\nName=Notes\nName[in]=Catatan\nExec=notes",
                ),
            )!!
        assertEquals("Notes", app.name)
        assertFalse(app.terminal)
    }

    @Test
    fun matchesReverseDnsWindowIds() {
        val app = LinuxApp("Vesktop", "vesktop", terminal = false, id = "vesktop", wmClass = null)
        assertTrue(app.matches("io.github.Vesktop.Vesktop"))
        assertTrue(app.matches("vesktop"))
        assertFalse(app.matches("org.gnome.Terminal"))
    }

    @Test
    fun runsAppImagesFromGuestPaths() {
        val rootfs = File(tmp.root, "rootfs")
        val image = File(rootfs, "root/Downloads/Vesktop-x86_64.AppImage")
        image.parentFile.mkdirs()
        image.writeText("")
        val app = appImageEntry(image, rootfs)
        assertEquals("'/root/Downloads/Vesktop-x86_64.AppImage' --appimage-extract-and-run", app.exec)
        assertEquals("/root/Downloads", app.dir)
        assertTrue(app.electronLike)
        assertEquals(LinuxApp("Vesktop", "vesktop", false, "vesktop", null).family, app.family)
    }
}
