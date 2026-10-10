package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File

/**
 * Audits only the runtime payload that CloudyPlay itself packages.
 * This does not download, extract, or execute third-party components.
 */
object RuntimePayloadAudit {
    data class Report(
        val packagedAssets: List<String>,
        val missingAssets: List<String>,
        val nativeLibraries: List<String>,
        val missingNativeLibraries: List<String>
    ) {
        val readyForBootstrap: Boolean
            get() = missingAssets.isEmpty() && missingNativeLibraries.isEmpty()
    }

    // Keep this list explicit so a partial payload cannot be mistaken for a complete runtime.
    private val requiredAssets = listOf(
        "winlator-runtime/rootfs.tzst",
        "winlator-runtime/rootfs_patches.tzst",
        "winlator-runtime/pulseaudio.tzst",
        "winlator-runtime/container_pattern.tzst"
    )

    private val requiredLibraries = listOf(
        "libbox64.so",
        "libwinlator.so"
    )

    fun inspect(context: Context): Report {
        val foundAssets = requiredAssets.filter { asset ->
            runCatching { context.assets.open(asset).use { true } }.getOrDefault(false)
        }
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val foundLibraries = nativeDir.listFiles().orEmpty()
            .map { it.name }
            .filter { name -> requiredLibraries.any { it.equals(name, ignoreCase = true) } }

        return Report(
            packagedAssets = foundAssets,
            missingAssets = requiredAssets - foundAssets.toSet(),
            nativeLibraries = foundLibraries,
            missingNativeLibraries = requiredLibraries - foundLibraries.toSet()
        )
    }
}
