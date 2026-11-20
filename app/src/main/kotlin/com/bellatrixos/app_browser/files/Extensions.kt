package com.bellatrixos.app_browser.files

/** Tebak ekstensi dari awalan data Base64. */
object Extensions {
    @JvmStatic
    fun a(str: String?): String {
        if (str != null && str.length >= 5) {
            if (str.startsWith("/9j/")) return "jpg"
            if (str.startsWith("iVBOR")) return "png"
            if (str.startsWith("R0lGOD")) return "gif"
            if (str.startsWith("UklGR")) return "webp"
            if (str.startsWith("AAABAA")) return "ico"
            if (str.startsWith("JVBER")) return "pdf"
            if (str.startsWith("UEsDB")) return "zip"
            if (str.startsWith("TVqQA")) return "exe"
            if (str.startsWith("f0VMR")) return "elf"
            if (str.startsWith("Qk0")) return "bmp"
            if (str.startsWith("T2dnUw")) return "ogg"
            if (str.startsWith("SUQz")) return "mp3"
            if (str.startsWith("AAAAF")) return "mp4"
            if (str.startsWith("MThjY")) return "txt"
        }
        return "bin"
    }
}
