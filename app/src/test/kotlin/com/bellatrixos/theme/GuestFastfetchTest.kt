package com.bellatrixos.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestFastfetchTest {
    /** The upstream shape, with the two guest-hostile lines from Omarchy 4.5. */
    private val upstream =
        """
        {
          "logo": {
            "type": "file",
            "source": "~/.config/omarchy/branding/about.txt"
          },
          "modules": [
            {
              "type": "command",
              "key": "OS Age",
              "keyColor": "magenta",
              "text": "days=$(( ($(date +%s) - $(stat -c %W /var/log)) / 86400 )); omarchy-i18n --count \"${'$'}days\" about.days \"${'$'}days\""
            },
            {
              "type": "command",
              "key": "Uptime",
              "keyColor": "magenta",
              "text": "up=$(cut -d. -f1 /proc/uptime); omarchy-i18n agents.durationDaysHours \"${'$'}d\" \"${'$'}h\""
            }
          ]
        }
        """.trimIndent()

    @Test fun dropsOmarchyHelpers() {
        val out = guestFastfetch(upstream)
        assertFalse("omarchy-i18n" in out)
        assertFalse("/proc/uptime" in out)
        assertTrue("echo \\\"\$days days\\\"" in out)
    }

    @Test fun putsLogoOnTopAndUsesNativeUptime() {
        val out = guestFastfetch(upstream)
        assertTrue("\"position\": \"top\"" in out)
        assertTrue("\"type\": \"uptime\"" in out)
        assertTrue("\"key\": \"Uptime\"" in out)
    }

    @Test fun bundledConfigIsAlreadyGuestShaped() {
        val out = guestFastfetch(GuestThemeConfig.FASTFETCH_CONFIG)
        assertTrue("\"position\": \"top\"" in out)
        assertFalse("omarchy-i18n" in out)
        assertTrue("\"uptime\"" in out)
        assertEquals(out, guestFastfetch(out))
    }
}
