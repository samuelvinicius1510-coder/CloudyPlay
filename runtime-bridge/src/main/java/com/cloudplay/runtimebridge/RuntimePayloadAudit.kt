package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File

/**
 * Audits the payload CloudyPlay actually packages.
 * This does not download, extract, or execute third-party components.
 * Asset/library presence alone must never be treated as runtime readiness.
 */
object RuntimePayloadAudit {
    data class Report(
        val packagedAssets: List<String>,
        val missingAssets: List<String>,
        val packagedNativeLibraries: List<String>
    ) {
        // Presence checks are not enough to declare a runtime bootable.
        // Native JNI entry points, rootfs installation, and graphics support
        // still need to be integrated and exercised on a physical device.
        val readyForBootstrap: Boolean
            get() = false
    }

    // Names follow the pinned Winlator upstream asset layout.
    private val requiredAssets = listOf(
        "winlator-runtime/rootfs.tzst",
        "winlator-runtime/rootfs_patches.tzst",
        "winlator-runtime/pulseaudio.tzst",
        "winlator-runtime/container_pattern.tzst",
        "winlator-runtime/box64/box64-0.4.4.tzst"
    )

    fun inspect(context: Context): Report {
        val foundAssets = requiredAssets.filter { asset ->
            runCatching { context.assets.open(asset).use { true } }.getOrDefault(false)
        }
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLibraries = nativeDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.equals("so", ignoreCase = true) }
            .map { it.name }
            .sorted()

        return Report(
            packagedAssets = foundAssets,
            missingAssets = requiredAssets - foundAssets.toSet(),
            packagedNativeLibraries = nativeLibraries
        )
    }
}
