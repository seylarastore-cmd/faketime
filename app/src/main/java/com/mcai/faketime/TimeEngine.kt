package com.mcai.faketime

/**
 * Pure offset engine. No Android dependencies so it can be unit tested on JVM.
 *
 * Model: choose a static offset delta. We add the SAME delta to both the wall
 * clock and the monotonic elapsed clock. That keeps `wall - elapsed` equal to
 * the real boot-time constant, so every duration/timer measured through
 * elapsedRealtime()/uptimeMillis() stays exact and consistent.
 *
 * System.nanoTime() is intentionally left untouched: durations measured via
 * nanoTime remain real, and because the offset is a constant, that matches
 * the (also exact) elapsed durations. Nothing drifts.
 */
object TimeEngine {

    @Volatile
    var offsetMillis: Long = 0L

    /** Wall-clock read for a target process. */
    fun fakeCurrentTimeMillis(real: Long): Long = real + offsetMillis

    /** Elapsed/uptime read for a target process. Same +delta keeps wall-elapsed stable. */
    fun fakeElapsedMillis(real: Long): Long = real + offsetMillis

    /** Elapsed nanos read. delta converted to nanos. */
    fun fakeElapsedNanos(realNanos: Long): Long = realNanos + offsetMillis * 1_000_000L

    /**
     * Consistency invariant used by tests:
     * for any fixed offset, wall-elapsed must stay equal to the real boot-time constant.
     */
    fun bootTimeWallMinusElapsed(wall: Long, elapsed: Long): Long = wall - elapsed
}
