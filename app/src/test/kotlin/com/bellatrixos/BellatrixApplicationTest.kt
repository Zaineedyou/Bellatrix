package com.bellatrixos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BellatrixApplicationTest {
    @Test fun theProcessNamedAfterThePackageIsTheMainOne() {
        assertTrue(isMainProcess("com.bellatrixos", "com.bellatrixos"))
    }

    @Test fun geckoChildProcessesAreNotTheMainOne() {
        // The services GeckoView declares with android:process: content, GPU, socket, media decoding and the rest.
        for (suffix in listOf(":tab0", ":tab1", ":gpu", ":socket", ":rdd", ":utility", ":media", ":gmplugin", ":crashhelper")) {
            assertFalse(suffix, isMainProcess("com.bellatrixos$suffix", "com.bellatrixos"))
        }
    }

    @Test fun anUnknownNameKeepsTheLauncherWorking() {
        assertTrue(isMainProcess(null, "com.bellatrixos"))
    }
}
