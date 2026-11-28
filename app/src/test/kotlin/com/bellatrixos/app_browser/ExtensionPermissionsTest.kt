package com.bellatrixos.app_browser

import com.bellatrixos.app_browser.widgets.AddressIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionPermissionsTest {
    @Test
    fun allUrlsMeansEverySite() {
        val access = ExtensionPermissions.hostAccess(arrayOf("<all_urls>", "https://a.example/*"))
        assertTrue(access.allSites)
        assertTrue(access.sites.isEmpty())
    }

    @Test
    fun namedSitesAreListedOnceEach() {
        val access = ExtensionPermissions.hostAccess(arrayOf("https://a.example/*", " https://a.example/* ", "", "https://b.example/*"))
        assertFalse(access.allSites)
        assertEquals(listOf("https://a.example/*", "https://b.example/*"), access.sites)
    }

    @Test
    fun namesSkipBlanksRepeatsAndTheNoneMarker() {
        val names = ExtensionPermissions.names(arrayOf("tabs", "storage", "tabs", " "), arrayOf("none", "locationInfo"))
        assertEquals(listOf("tabs", "storage", "locationInfo"), names)
    }

    @Test
    fun identityFollowsTheScheme() {
        assertEquals(AddressIdentity.SECURE, AddressIdentity.of("https://example.org/"))
        assertEquals(AddressIdentity.INSECURE, AddressIdentity.of("HTTP://example.org/"))
        assertEquals(AddressIdentity.NONE, AddressIdentity.of("about:blank"))
        assertEquals(AddressIdentity.NONE, AddressIdentity.of(null))
    }
}
