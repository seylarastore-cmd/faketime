package com.mcai.faketime

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeEngineTest {

    @Test
    fun `wall clock is shifted by offset`() {
        TimeEngine.offsetMillis = 5 * 86_400_000L // +5 days
        val real = 1_700_000_000_000L
        assertEquals(real + 5 * 86_400_000L, TimeEngine.fakeCurrentTimeMillis(real))
    }

    @Test
    fun `elapsed clock is shifted by same offset`() {
        TimeEngine.offsetMillis = 3_600_000L
        val elapsed = 123_456_789L
        assertEquals(123_456_789L + 3_600_000L, TimeEngine.fakeElapsedMillis(elapsed))
    }

    @Test
    fun `wall minus elapsed stays equal to real boot constant`() {
        val offsets = listOf(0L, 1_000L, -2_000L, 30 * 86_400_000L, -30 * 86_400_000L)
        val realWall = 1_700_000_000_000L
        val realElapsed = 40_000_000L
        val realBoot = realWall - realElapsed

        for (offset in offsets) {
            TimeEngine.offsetMillis = offset
            val fakeWall = TimeEngine.fakeCurrentTimeMillis(realWall)
            val fakeElapsed = TimeEngine.fakeElapsedMillis(realElapsed)
            assertEquals(realBoot, TimeEngine.bootTimeWallMinusElapsed(fakeWall, fakeElapsed))
        }
    }

    @Test
    fun `durations are preserved under constant offset`() {
        TimeEngine.offsetMillis = 86_400_000L
        // Two readings 10s apart in real time must differ by exactly 10s in fake time.
        val a = TimeEngine.fakeElapsedMillis(1_000L)
        val b = TimeEngine.fakeElapsedMillis(11_000L)
        assertEquals(10_000L, b - a)
    }

    @Test
    fun `zero offset returns real values`() {
        TimeEngine.offsetMillis = 0L
        val real = System.currentTimeMillis()
        assertEquals(real, TimeEngine.fakeCurrentTimeMillis(real))
    }
}
