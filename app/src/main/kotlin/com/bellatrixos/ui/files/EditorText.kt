package com.bellatrixos.ui.files

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Files past this go to micro: a text field lays out all of its text, and that gets slow. */
internal const val EDITOR_MAX_BYTES = 1L shl 20

/** A file as the editor reads it. */
internal sealed interface EditorFile {
    /**
     * [text] has LF line endings; [crlf] says whether to write CRLF back. [raw] keeps one byte per
     * character, so a file Android calls binary (NULs, another encoding) can still be read and written
     * instead of being waved away as a thumbnail. A missing file is empty text.
     */
    data class Text(
        val text: String,
        val crlf: Boolean,
        val raw: Boolean = false,
    ) : EditorFile

    data object TooBig : EditorFile

    /** Not UTF-8, or has NUL bytes: saving it as text would change it. */
    data object Binary : EditorFile
}

internal fun readEditorFile(file: File): EditorFile {
    if (!file.exists()) return EditorFile.Text("", crlf = false)
    if (file.length() > EDITOR_MAX_BYTES) return EditorFile.TooBig
    val bytes = file.readBytes()
    val raw = bytes.any { it == 0.toByte() }
    val text =
        if (raw) {
            // Latin-1 is a byte-for-byte view: every byte has a character and back again.
            String(bytes, Charsets.ISO_8859_1)
        } else {
            try {
                Charsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (e: CharacterCodingException) {
                return EditorFile.Text(String(bytes, Charsets.ISO_8859_1), crlf = false, raw = true)
            }
        }
    val crlf = "\r\n" in text
    return EditorFile.Text(if (crlf) text.replace("\r\n", "\n") else text, crlf, raw)
}

/** Encodes with the file's own byte mapping, so a Latin-1 view writes the same bytes back. */
internal fun encodeEditorText(
    text: String,
    crlf: Boolean,
    raw: Boolean = false,
): ByteArray {
    val withEndings = if (crlf) text.replace("\n", "\r\n") else text
    return if (raw) withEndings.toByteArray(Charsets.ISO_8859_1) else withEndings.toByteArray(Charsets.UTF_8)
}

/**
 * What the tab key inserts: the file's own indent (a tab, or the narrowest run
 * of spaces that starts a line), two spaces if nothing is indented yet.
 */
internal fun indentUnit(text: String): String {
    var narrowest = Int.MAX_VALUE
    for (line in text.lineSequence()) {
        if (line.startsWith('\t')) return "\t"
        val spaces = line.takeWhile { it == ' ' }.length
        if (spaces in 1 until narrowest && spaces < line.length) narrowest = spaces
    }
    return " ".repeat(if (narrowest in 2..8) narrowest else 2)
}

/** The files git has you write a message in. */
internal val GIT_MESSAGES = setOf("COMMIT_EDITMSG", "TAG_EDITMSG", "MERGE_MSG", "SQUASH_MSG", "EDIT_DESCRIPTION")

/** Commit messages and notes: the keyboard may capitalize and autocorrect them, unlike code and configs. */
internal fun isProse(name: String) = name in GIT_MESSAGES || extension(name) in setOf("md", "txt")

/** Offsets where each line starts; the first is 0. */
internal fun lineStarts(text: CharSequence): IntArray {
    var count = 1
    for (c in text) if (c == '\n') count++
    val starts = IntArray(count)
    var i = 1
    for ((offset, c) in text.withIndex()) if (c == '\n') starts[i++] = offset + 1
    return starts
}

/** The next match of [query] at or after [from] (case-insensitive), wrapping around; [backward] looks before [from] instead. */
internal fun findMatch(
    text: CharSequence,
    query: String,
    from: Int,
    backward: Boolean = false,
): Int? {
    if (query.isEmpty()) return null
    val s = text.toString()
    val hit =
        if (backward) {
            s.lastIndexOf(query, from - 1, ignoreCase = true).takeIf { from > 0 && it >= 0 } ?: s.lastIndexOf(query, ignoreCase = true)
        } else {
            s.indexOf(query, from, ignoreCase = true).takeIf { it >= 0 } ?: s.indexOf(query, ignoreCase = true)
        }
    return hit.takeIf { it >= 0 }
}
