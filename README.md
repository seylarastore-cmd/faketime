# FakeTime

An LSPosed (Zygisk) module that applies a **whole-device static time offset**, so every app on a rooted Android 9–15 device reads the same shifted date/time through the standard clock APIs — while staying internally consistent so timers and durations keep working.

Built as: **LSPosed hook engine + companion configuration app** in one APK.

## What it does

- Hooks `System.currentTimeMillis()`, `android.os.SystemClock.*`, and `java.time.Clock.*` in every app process.
- Adds the **same** offset `Δ` to both the wall clock and the monotonic elapsed clock, preserving `wall − elapsed`. Durations measured via `elapsedRealtime()`/`uptimeMillis()` stay exact, so apps don't see drifts and timers don't misfire.
- Companion app: set offset in days/hours/minutes (or negative for going backward), master on/off switch, and per-app "force real time" toggles.

## Honest limitations (read this)

No app-side method is 100% undetectable. This module cannot intercept:

- **NTP / network time** — apps that fetch time from their own server or an SNTP server will see the real time.
- **GPS time**.
- **Server `Date:` headers**.
- **Play Integrity / SafetyNet** attestations.

The static-offset model is the *most* consistent approach (nothing drifts), but it is not magic. Use it for testing and personal use.

## Requirements

- Rooted device with **Zygisk** + **LSPosed** (Zygisk version), Android 9–15.
- The module must be **enabled and scoped** in LSPosed.

## Install

1. Build the APK (GitHub Actions produces `app-debug.apk` — see below) and install it:
   ```bash
   adb install -r app-debug.apk
   ```
2. Open **LSPosed Manager → Modules**, enable **FakeTime**.
3. **Scope is the #1 reason it "does nothing"**: LSPosed only injects the module into processes you explicitly scope. To fake time for an app, that app MUST be checked in the module's scope list inside LSPosed Manager (Modules → FakeTime → scope icon). "System Framework" alone does NOT cover arbitrary apps.
   - Pick the app you want to test (e.g. a calendar/file manager) and enable FakeTime for it.
   - For whole-device: check System Framework **plus every app** you care about.
4. Tap **Reboot** in LSPosed (or `adb reboot`) — scope changes require a reboot.
5. Open the **FakeTime** app, set an offset (e.g. +2 days), hit **Apply**.
6. **Now force-stop and reopen the target app**, then look at its dates.

> Important — what "works" means: FakeTime does NOT change the device's real clock (Settings → Date & Time stays correct). It fakes time only **inside the processes LSPosed scopes to it**. Check the clock in the *target app*, not in Settings.

## Verifying it actually works (5-minute checklist)

1. **Is the module loaded?** Open LSPosed Manager → Logs, filter `FakeTime`. You should see lines like:
   ```
   [FakeTime] loaded into com.example.target, registering hooks
   [FakeTime] hooked System.currentTimeMillis
   ```
   If you see `skipping excluded process` for your target app, you scoped a system-critical app — pick a normal app instead.
   If you see nothing at all, the module isn't enabled/scoped, or you haven't rebooted.

2. **Is the offset read?** With offset +2 days applied, in the target app:
   - `System.currentTimeMillis()` → today + 2 days
   - `Date()`, `Calendar`, `Instant.now()`, `LocalDateTime.now()` → shifted
   - Elapsed timers still fire on correct real durations

3. **Per-App tab**: lists ALL apps now (including system apps). Toggle "Real time" for any app you want exempted.

> Hard safety exclusions (never hooked) are built in: `system_server`, `SystemUI`, LSPosed manager, and the FakeTime app itself. This prevents boot/sync breakage. Per-app "force real time" overrides in the Per-App tab act at runtime.

## Building locally

Requires JDK 17 + Android SDK:

```bash
./gradlew :app:assembleDebug
```

## Building on GitHub (recommended)

1. Create a new repo on GitHub.
2. Push this project:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin git@github.com:<you>/<repo>.git
   git push -u origin main
   ```
3. The included `.github/workflows/build.yml` automatically builds `app-debug.apk` on every push to `main`. Download it from the **Actions** tab → workflow run → **Artifacts**.
4. (Optional) Tag a release `git tag v1.0 && git push origin v1.0` to also produce a release build.

## Project layout

```
app/
  src/main/assets/xposed_init          # module entry point for LSPosed
  src/main/java/com/mcai/faketime/
    HookEntry.kt                       # load-package entry
    Config.kt                          # prefs schema + safety exclusions
    TimeEngine.kt                      # pure offset math (JVM-testable)
    hooks/HookConfig.kt                # cross-process config reader
    hooks/TimeHooks.kt                 # hook registrations for all time APIs
    ui/                                # companion app (tabs, offset, per-app)
  src/main/res/                        # layouts, strings, icons
.github/workflows/build.yml            # CI APK builder
```

## How the time model works

- Real wall time `R(t)`, real monotonic elapsed `E(t)`.
- Choose offset `Δ`. Hooks return `wall = R(t) + Δ` and `elapsed = E(t) + Δ`.
- `wall − elapsed` stays equal to the real boot-time constant → duration measurements remain correct, nothing drifts.
- `System.nanoTime()` is left untouched: since the offset is constant, nanoTime-based durations agree with elapsed-based durations.

## Disclaimer

For testing and personal use on your own device. Faking time to bypass app restrictions may violate the target app's Terms of Service. Use at your own risk.
