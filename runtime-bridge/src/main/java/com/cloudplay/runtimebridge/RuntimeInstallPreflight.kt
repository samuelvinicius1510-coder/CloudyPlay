package com.cloudplay.runtimebridge

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Storage/asset preflight before a future RootFS installer runs.
 *
 * This does not extract archives. The upstream installer relies on a Zstandard
 * tar extractor and patch/version semantics; those must be integrated before
 * any archive is unpacked or the runtime can be marked ready.
 */
object RuntimeInstallPreflight {
    // Keep a safety margin for temporary extraction files, patches and prefix data.
    private const val MINIMUM_FREE_BYTES = 2L * 1024L * 1024L * 1024L

    data class Result(
        val readyToInstall: Boolean,
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
            runCatching { context.assets.open(path).use { it.read() >= 0 } }
                .getOrDefault(false).not()
        }
        val available = runCatching {
            StatFs(directory.absolutePath).availableBytes
        }.getOrDefault(0L)

        val enoughSpace = available >= MINIMUM_FREE_BYTES
        val ready = missing.isEmpty() && enoughSpace
        val detail = when {
            missing.isNotEmpty() ->
                "Runtime assets are not packaged in this APK: ${missing.joinToString()}"
            !enoughSpace ->
                "Insufficient private storage. Available: ${available / (1024L * 1024L)} MiB; " +
                    "required safety margin: ${MINIMUM_FREE_BYTES / (1024L * 1024L)} MiB."
            else ->
                "Assets and storage preflight passed. Extraction and native initialization are still not implemented."
        }

        return Result(ready, missing, available, MINIMUM_FREE_BYTES, detail)
    }
}
