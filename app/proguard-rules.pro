# Keep LSPosed/Xposed API classes
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep module entry point for xposed_init
-keep class com.mcai.faketime.HookEntry { *; }
