package com.bellatrixos.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LiveBackgroundTest {
    @Test fun commonAndUncommonFormatsAreLiveBackgrounds() {
        for (name in listOf("clip.mp4", "clip.webm", "clip.mkv", "clip.3g2", "clip.flv", "clip.ts")) {
            assertTrue(name, File(name).isVideoBackground())
            assertTrue(name, File(name).isLiveBackground())
        }
        for (name in listOf("loop.gif", "loop.webp", "loop.apng")) {
            assertTrue(name, File(name).isAnimatedImageBackground())
            assertTrue(name, File(name).isLiveBackground())
        }
    }

    @Test fun stillImagesStayStill() {
        for (name in listOf("still.png", "still.jpg", "still.jpeg", "still.heic")) {
            assertFalse(name, File(name).isLiveBackground())
        }
    }

    @Test fun pickerSharesTheSameExtensionSet() {
        assertTrue("mkv" in BACKGROUND_EXTENSIONS)
        assertTrue("heic" in BACKGROUND_EXTENSIONS)
        assertTrue("apng" in BACKGROUND_EXTENSIONS)
    }
}
