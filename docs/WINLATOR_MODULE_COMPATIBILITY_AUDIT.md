# Winlator module compatibility audit

Audited upstream source: [brunodev85/winlator-app](https://github.com/brunodev85/winlator-app), pinned commit `a030f552f452158a2db64fdb32b490fa19c0b48d`.

## Findings

The upstream project is a complete Android application module, not a reusable runtime dependency. Its Gradle module uses namespace/application ID `com.winlator`, Android Gradle Plugin 8.4.2 in its own build, NDK 24.0.8215888, CMake 3.22.1, and ARM64 native build filters. It also has a large `app/src/main/assets` tree containing a compressed root filesystem, Box64 assets, Wine/runtime components, graphics drivers, and configuration files, alongside JNI libraries and native CMake source.

## Integration decision

Do not add the upstream application as a second application module and assume it is embedded: that would still be a separate installable app, not a runtime inside CloudyPlay. Do not copy its multi-gigabyte runtime assets into the current APK before build reproducibility, license notices, package/resource conflicts, and device graphics compatibility are resolved.

## Ordered next steps

1. Keep the upstream commit pinned and record the license/notice files for all upstream and submodule components.
2. Build the upstream app unchanged in CI as a baseline, including recursive submodules, to prove its native toolchain and dependencies can be built.
3. Inventory the native libraries and runtime assets actually emitted by that build; identify which are required for ARM64 and which graphics paths can work on Mali.
4. Design a runtime module boundary that can coexist with CloudyPlay's Compose app, avoiding a second launcher/application ID and resolving manifest/resource/JNI collisions.
5. Only after the baseline and packaging audit pass, adapt the module and connect a launch request to a real runtime API.

## Current status

CloudyPlay has an adapter interface contract, but no Winlator native engine is embedded and no Windows executable can yet be launched by CloudyPlay. The existing debug build verifies the app shell, not Windows-game execution.
