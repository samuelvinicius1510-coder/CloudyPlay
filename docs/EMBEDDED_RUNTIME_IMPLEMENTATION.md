# Embedded Winlator integration boundary

## Current state

- The official Winlator source at `b6b2259158cf38d06067c34430d840d56b46d220` builds in the separate reference workflow.
- CloudyPlay has a `WindowsRuntimeAdapter` contract and a fail-closed `EmbeddedWindowsRuntimeAdapter`.
- The adapter validates that a selected executable is a real `.exe` inside CloudyPlay's private `filesDir/game-imports` folder.
- It deliberately returns `started=false`: no Wine/Box64 process is launched until the runtime is adapted and packaged.
- This keeps the catalog/admin app functional and prevents a false success status or unsafe shell invocation.

## Why the upstream APK cannot simply be merged

Winlator is an Android application with its own manifest, application ID, resources, JNI libraries, native C/C++ build, and large runtime assets. Treating the APK as a library would not embed its engine. A real in-process integration requires a supported boundary around runtime initialization, native libraries and graphics/audio setup.

## Next integration gates

1. Inspect `Winlator-runtime-inventory` and the upstream APK artifacts from GitHub Actions.
2. Review upstream and bundled component licenses/notices before redistributing code, native libraries, or assets.
3. Map the upstream app's initialization flow and native dependencies; pin all source revisions.
4. Design a dedicated Android library/runtime module with a non-conflicting manifest and JNI loading strategy.
5. Package runtime assets into app-private storage with integrity checks and clear storage-space errors.
6. Initialize the runtime on-device and report truthful state/errors.
7. Test a harmless Windows executable first, then assess each game's graphics/API compatibility.

Do not mark the runtime READY merely because files exist. Do not run arbitrary shell commands assembled from a game path. The Galaxy M22 uses Mali graphics, so Adreno-oriented Turnip support must not be assumed.
