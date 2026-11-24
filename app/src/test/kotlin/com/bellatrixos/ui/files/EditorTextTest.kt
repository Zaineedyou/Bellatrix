package com.bellatrixos.ui.files

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextTest {
    @Test fun latin1BytesSurviveARoundTrip() {
        val bytes = byteArrayOf(0x41, 0x00, 0x7f, 0x80.toByte(), 0xff.toByte())
        val file = java.io.File.createTempFile("bellatrix-raw", ".bin")
        try {
            file.writeBytes(bytes)
            val decoded = readEditorFile(file)
            assertTrue(decoded is EditorFile.Text)
            decoded as EditorFile.Text
            assertTrue(decoded.raw)
            assertArrayEquals(bytes, encodeEditorText(decoded.text, decoded.crlf, decoded.raw))
        } finally {
            file.delete()
        }
    }

    @Test fun textKeepsItsLineEndings() {
        assertEquals("a\nb", readEditorTextFor("a\r\nb").text)
        assertArrayEquals("a\r\nb".toByteArray(), encodeEditorText("a\nb", crlf = true))
    }

    private fun readEditorTextFor(content: String): EditorFile.Text {
        val file = java.io.File.createTempFile("bellatrix-text", ".txt")
        try {
            file.writeBytes(content.toByteArray())
            return readEditorFile(file) as EditorFile.Text
        } finally {
            file.delete()
        }
    }
}
