package com.bellatrixos.linux

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Shell quoting and stream helpers used by the guest scripts.
 *
 * Ported verbatim from Bellatrix's `shQuote` (owl Builtins) and
 * `Shell.shellQuote` / `InputStream.readUpTo`, so generated scripts stay
 * byte-for-byte identical to the ones the original launcher runs.
 */

/** Quote [s] as a single POSIX shell word. */
fun shQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

/** The alias the guest scripts use. */
fun shellQuote(s: String): String = shQuote(s)

/** Read at most [limit] bytes, without closing [this]. */
fun InputStream.readUpTo(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (out.size() < limit) {
        val n = read(chunk, 0, minOf(chunk.size, limit - out.size()))
        if (n < 0) break
        out.write(chunk, 0, n)
    }
    return out.toByteArray()
}
