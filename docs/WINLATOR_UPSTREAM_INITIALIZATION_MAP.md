# Upstream initialization map for CloudyPlay

Reference source:
- Winlator root: https://github.com/brunodev85/winlator/tree/b6b2259158cf38d06067c34430d840d56b46d220
- Android app submodule: https://github.com/brunodev85/winlator-app/tree/a030f552f452158a2db64fdb32b490fa19c0b48d
- App entry point: `app/src/main/java/com/winlator/MainActivity.java`
- Container configuration: `app/src/main/java/com/winlator/container/ContainerManager.java`
- Root filesystem installer: `app/src/main/java/com/winlator/xenvironment/RootFSInstaller.java`
- Root filesystem model: `app/src/main/java/com/winlator/xenvironment/RootFS.java`
- Wine utilities: `app/src/main/java/com/winlator/core/WineUtils.java`
- Native source and JNI: `app/src/main/cpp` and `app/src/main/jniLibs/arm64-v8a`
- Root filesystem assets: `app/src/main/assets/rootfs.tzst` and `rootfs_patches.tzst`

## Integration decision

Do not copy the upstream APK into CloudyPlay or simply add its application module as another app module. That would build a separate Android application, not embed its runtime. A real embedded integration needs a narrow library boundary that separates runtime/container setup from upstream activities, preferences, Android resources, and application-specific services.

## Safe milestones

1. Keep upstream source pinned to the revisions above and retain its LICENSE and third-party notices.
2. Trace which portions of MainActivity and ContainerManager are UI-only and which can be extracted into a runtime service.
3. Identify native initialization dependencies and JNI entry points before changing Gradle/module structure.
4. Build a dedicated runtime library/module with a non-conflicting manifest and package/resource namespace.
5. Install rootfs and components only into CloudyPlay's private files directory, with storage checks, integrity checks, cancellation and clear error reporting.
6. Implement actual initialization and report READY only after native setup succeeds.
7. Validate on physical ARM64 hardware. This must include a graphics backend compatible with the target GPU; do not assume Turnip works on Mali.
8. Connect the game launch action only after initialization succeeds and validate executable paths remain inside the private import directory.

## Current limitation

CloudyPlay's adapter is intentionally fail-closed and does not launch an EXE yet. The reference APK and inventory workflows prove the upstream project can be built in CI; they do not prove that its runtime has been embedded or works on a particular phone.
