package com.bellatrixos.app_browser.widgets

/** What the address bar's icon says about the page, as Firefox's identity icon does. */
enum class AddressIdentity {
    NONE,
    SECURE,
    INSECURE,
    ;

    companion object {
        fun of(address: String?): AddressIdentity =
            when {
                address == null -> NONE
                address.startsWith("https://", ignoreCase = true) -> SECURE
                address.startsWith("http://", ignoreCase = true) -> INSECURE
                else -> NONE
            }
    }
}
