package com.mcai.faketime.hooks

import android.os.SystemClock
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Registers all time-API hooks for one target process.
 *
 * Every callback is defensive: if anything throws, the original value is kept
 * so the host app never crashes because of us.
 *
 * Note: hooks are registered once per process. Each callback asks the
 * per-process [HookConfig] for the live offset, so config changes propagate
 * without restarting target apps.
 */
object TimeHooks {

    private var registered = false

    @Synchronized
    fun register(config: HookConfig) {
        if (registered) return
        registered = true

        try {
            hookSystemCurrentTimeMillis(config)
            hookSystemClock(config)
            hookJavaTime(config)
        } catch (t: Throwable) {
            XposedBridge.log("[FakeTime] hook registration failed: ${t.message}")
        }
    }

    private fun offset(config: HookConfig): Long = config.offsetForProcess()

    /** afterHookedMethod: original already ran, param.result holds the real value. */
    private fun systemTimeHook(config: HookConfig) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                val off = offset(config)
                if (off != 0L) {
                    param.result = (param.result as Long) + off
                }
            } catch (_: Throwable) {}
        }
    }

    private fun elapsedMillisHook(config: HookConfig) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                val off = offset(config)
                if (off != 0L) {
                    param.result = (param.result as Long) + off
                }
            } catch (_: Throwable) {}
        }
    }

    private fun elapsedNanosHook(config: HookConfig) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                val off = offset(config)
                if (off != 0L) {
                    param.result = (param.result as Long) + off * 1_000_000L
                }
            } catch (_: Throwable) {}
        }
    }

    private fun hookSystemCurrentTimeMillis(config: HookConfig) {
        XposedHelpers.findAndHookMethod(
            System::class.java,
            "currentTimeMillis",
            systemTimeHook(config),
        )
    }

    private fun hookSystemClock(config: HookConfig) {
        XposedHelpers.findAndHookMethod(
            SystemClock::class.java,
            "currentTimeMillis",
            systemTimeHook(config),
        )
        XposedHelpers.findAndHookMethod(
            SystemClock::class.java,
            "elapsedRealtime",
            elapsedMillisHook(config),
        )
        XposedHelpers.findAndHookMethod(
            SystemClock::class.java,
            "uptimeMillis",
            elapsedMillisHook(config),
        )
        XposedHelpers.findAndHookMethod(
            SystemClock::class.java,
            "elapsedRealtimeNanos",
            elapsedNanosHook(config),
        )
    }

    private fun hookJavaTime(config: HookConfig) {
        // system(ZoneId) -> keep the caller's zone
        val systemCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val zone = param.args[0] as ZoneId
                    param.result = FakeClock(zone)
                } catch (_: Throwable) {}
            }
        }
        // systemDefaultZone() -> default zone
        val defaultZoneCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    param.result = FakeClock(ZoneId.systemDefault())
                } catch (_: Throwable) {}
            }
        }
        // systemUTC() -> UTC
        val utcCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    param.result = FakeClock(ZoneOffset.UTC)
                } catch (_: Throwable) {}
            }
        }

        XposedHelpers.findAndHookMethod(Clock::class.java, "systemDefaultZone", defaultZoneCallback)
        XposedHelpers.findAndHookMethod(Clock::class.java, "systemUTC", utcCallback)
        XposedHelpers.findAndHookMethod(Clock::class.java, "system", ZoneId::class.java, systemCallback)
    }

    /**
     * A Clock whose instant() is resolved lazily through the (already hooked)
     * System.currentTimeMillis(), so no double-offset is applied.
     */
    private class FakeClock(private val zone: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = FakeClock(zone)

        override fun instant(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())
    }
}
