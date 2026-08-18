package com.mcai.faketime

import android.os.SystemClock
import com.mcai.faketime.hooks.HookConfig
import com.mcai.faketime.hooks.TimeHooks
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == null) return
        if (Config.isExcludedProcess(lpparam.packageName)) {
            XposedBridge.log("[FakeTime] skipping excluded process: ${lpparam.packageName}")
            return
        }

        val config = HookConfig(lpparam.packageName)
        XposedBridge.log("[FakeTime] loaded into ${lpparam.packageName}, registering hooks")

        TimeHooks.register(config)
    }
}
