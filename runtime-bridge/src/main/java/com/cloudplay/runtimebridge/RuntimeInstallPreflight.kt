package com.cloudplay.runtimebridge

import android.content.Context
import android.os.StatFs

/**
 * Checks prerequisites for a future RootFS installation.
 *
 * Passing this check does NOT mean the runtime can be installed or launched:
 * archive extraction, patch/version handling, and native initialization are
 * deliberately not implemented here.
 */
object RuntimeInstallPreflight {
    // Safety margin for temporary extraction files, patches and prefix data.
    private const val MINIMUM_FREE_BYTES = 2L * 1024L * 1024L * 1024L

    data class Result(
        val preflightPassed: Boolean,
        val missingAssets: List<String>,
        val availableBytes: Long,
        val requiredBytes: Long,
        val detail: String
    )

    private val requiredAssets = listOf(
        "winlator-runtime/rootfs.tzst",
        "winlator-runtime/rootfs_patches.tzst",
        "winlator-runtime/pulseaudio.tzst",
        "winlator-runtime/container_pattern.tzst",
        "winlator-runtime/box64/box64-0.4.4.tzst"
    )

    fun inspect(context: Context): Result {
        val workspace = RuntimeBridge.prepareWorkspace(context)
        val directory = workspace.directory
        if (!workspace.prepared || directory == null) {
            return Result(false, requiredAssets, 0L, MINIMUM_FREE_BYTES, workspace.detail)
        }

        val missing = requiredAssets.filter { path ->
            runCatching {
                context.assets.open(path).use { stream -> stream.read() >= 0 }
            }.getOrDefault(false).not()
        }
        val available = runCatching {
            StatFs(directory.absolutePath).availableBytes
        }.getOrDefault(0L)

        val enoughSpace = available >= MINIMUM_FREE_BYTES
        val passed = missing.isEmpty() && enoughSpace
        val detail = when {
            missing.isNotEmpty() ->
                "Runtime assets are not packaged in this APK: ${missing.joinToString()}"
            !enoughSpace ->
                "Insufficient private storage. Available: ${available / (1024L * 1024L)} MiB; " +
                    "required safety margin: ${MINIMUM_FREE_BYTES / (1024L * 1024L)} MiB."
            else ->
                "Prerequisite check passed only. RootFS extraction, patch/version handling, and native initialization are not implemented."
        }

        return Result(passed, missing, available, MINIMUM_FREE_BYTES, detail)
    }
}
