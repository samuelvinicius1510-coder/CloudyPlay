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
