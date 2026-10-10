# Windows runtime integration spike — CloudyPlay

## Decision

Do not copy the whole Winlator app or ship unverified native binaries as if they were a Gradle dependency. Wine, Box64, root filesystem, Android JNI glue and graphics translation are a coordinated runtime, not a single library. The upstream Winlator project is the reference implementation; any reuse must preserve notices and comply with each component's license.

Reference project: https://github.com/brunodev85/winlator
Upstream overview: https://github.com/winebox64/winlator

## First integration milestone

This milestone creates a clear runtime boundary inside CloudyPlay so the UI cannot claim that a file launched when no engine exists.

- Runtime status: `NOT_INSTALLED`, `PREPARING`, `READY`, `RUNNING`, `FAILED`.
- Imported executable files are stored in the app's private `filesDir/game-imports` directory.
- A future native runtime adapter must explicitly register its availability and accept an app-private path.
- Do not invoke shell commands or load arbitrary native libraries from imported game files.
- Preserve Supabase admin login and never embed private Supabase service-role credentials.

## Device compatibility gate

The target Samsung Galaxy M22 uses a Mali GPU. Do not enable Turnip-only paths by default; validate the actual Vulkan/OpenGL capabilities and a Mali-compatible translation route on-device before promising 3D games. First runtime test should be a small Windows x86/x64 utility or a simple 2D app, not a demanding game.

## Packaging

APK size may grow substantially, but first decide which ABI and runtime assets are required. Prefer a separate downloadable runtime asset or Android App Bundle splits if licensing and architecture allow. A 1–2 GB APK is not itself evidence of a working runtime.

## Exit criteria

1. CI builds the Android app.
2. A documented, reproducible build produces the native runtime assets for arm64-v8a.
3. Runtime initializes on an actual M22 without root.
4. A test executable starts and exits with observable status/logs.
5. Only after that: connect the library's Execute action to the runtime adapter.
