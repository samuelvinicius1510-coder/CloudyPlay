# Runtime bridge module

The `:runtime-bridge` Android library is the module boundary for eventual embedded Winlator integration. It is deliberately not a wrapper around the upstream APK.

Current behavior:
- Uses Android library packaging with a unique namespace.
- Limits native ABI packaging to `arm64-v8a`, matching the initial Winlator baseline.
- Reports missing native components or rootfs honestly.
- Does not load unknown JNI libraries or launch arbitrary executables.

Next work before enabling Wine/Box64:
1. Review Winlator and third-party component licenses/notices.
2. Adapt the upstream native build and JNI initialization to this module.
3. Package verified runtime assets, install them into private storage, and validate checksums.
4. Implement initialization and lifecycle/cleanup handling.
5. Test on an ARM64 device and select a graphics path compatible with its GPU.

## Runtime archive staging milestone

- `RuntimeAssetStager` copies the five pinned Winlator archive assets from APK assets into `filesDir/windows-runtime/staged`.
- Writes use temporary files followed by rename and reject empty files and paths that escape the private staging directory.
- The diagnostics screen exposes this operation and reports missing assets.
- This is staging only: it does not decompress `.tzst`, install rootfs patches, create a Wine prefix, load Box64, or launch Windows executables.
- The current APK does not yet include these archives, so staging is expected to report them missing until the upstream assets are integrated with license/notice review and packaging-size checks.
