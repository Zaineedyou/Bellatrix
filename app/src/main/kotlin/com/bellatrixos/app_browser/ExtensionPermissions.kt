package com.bellatrixos.app_browser

/** What an extension's install, update and permission prompts list, worked out from the names Gecko hands over. */
internal object ExtensionPermissions {
    /** The sites an extension asks to reach: every site, or just [sites]. */
    class HostAccess(
        val allSites: Boolean,
        val sites: List<String>,
    )

    /** Match patterns that cover every website. */
    private val everySite = setOf("<all_urls>", "*://*/*", "http://*/*", "https://*/*")

    fun hostAccess(origins: Array<String>): HostAccess {
        val named = origins.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val all = named.any { it in everySite }
        return HostAccess(all, if (all) emptyList() else named)
    }

    /** Permission names as the prompt lists them: no blanks, no repeats, and without the "none" data-collection marker. */
    fun names(
        permissions: Array<String>,
        dataCollection: Array<String>,
    ): List<String> =
        (permissions.asList() + dataCollection.asList())
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "none" }
            .distinct()
}
