package dev.woodos.launcher

import dev.woodos.launcher.core.Wmo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WmoTest {

    @Test
    fun knownCodes() {
        assertEquals("Clear sky", Wmo.describe(0))
        assertEquals("Partly cloudy", Wmo.describe(2))
        assertEquals("Thunderstorm", Wmo.describe(95))
    }

    @Test
    fun unknownCodeFallsBack() {
        assertEquals("Unknown", Wmo.describe(1234))
    }

    @Test
    fun severity() {
        assertTrue(Wmo.isSevere(95))
        assertTrue(Wmo.isSevere(99))
        assertFalse(Wmo.isSevere(0))
        assertFalse(Wmo.isSevere(63))
    }
}
