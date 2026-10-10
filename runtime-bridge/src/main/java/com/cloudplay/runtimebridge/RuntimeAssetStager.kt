package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Stages the pinned Winlator archives from APK assets into app-private storage.
 *
 * This deliberately does not extract .tzst archives or execute native code.
 * Extraction requires the matching upstream decompressor/installer and must
 * be added only after those dependencies are integrated and tested.
 */
object RuntimeAssetStager {
    data class Result(
        val success: Boolean,
        val stagedFiles: List<String>,
        val missingAssets: List<String>,
        val detail: String
    )

    private val requiredAssets = listOf(
        "winlator-runtime/rootfs.tzst",
        "winlator-runtime/rootfs_patches.tzst",
        "winlator-runtime/pulseaudio.tzst",
        "winlator-runtime/container_pattern.tzst",
        "winlator-runtime/box64/box64-0.4.4.tzst"
    )

    /**
     * Copies available runtime archives into filesDir/windows-runtime/staged.
     * Files are written to temporary names first, then renamed into place.
     */
    fun stage(context: Context): Result {
        val workspace = RuntimeBridge.prepareWorkspace(context)
        if (!workspace.prepared || workspace.directory == null) {
            return Result(false, emptyList(), requiredAssets, workspace.detail)
        }

        val root = workspace.directory.canonicalFile
        val staging = File(root, "staged").canonicalFile
        if (!staging.path.startsWith(root.path + File.separator)) {
            return Result(false, emptyList(), requiredAssets, "Staging path escaped the runtime workspace.")
        }

        val available = requiredAssets.filter { path ->
            runCatching { context.assets.open(path).use { true } }.getOrDefault(false)
        }
        val missing = requiredAssets - available.toSet()
        val copied = mutableListOf<String>()

        return try {
            if (!staging.isDirectory && !staging.mkdirs() && !staging.isDirectory) {
                throw IOException("Could not create staging directory.")
            }

            for (assetPath in available) {
                val relative = assetPath.removePrefix("winlator-runtime/")
                val destination = File(staging, relative).canonicalFile
                if (!destination.path.startsWith(staging.path + File.separator)) {
                    throw IOException("Asset path escaped staging directory.")
                }
                val parent = destination.parentFile
                    ?: throw IOException("Asset destination has no parent directory.")
                if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
                    throw IOException("Could not create asset subdirectory.")
                }

                val temporary = File(parent, destination.name + ".partial")
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(temporary).use { output -> input.copyTo(output) }
                }
                if (!temporary.isFile || temporary.length() <= 0L) {
                    temporary.delete()
                    throw IOException("Staged asset is empty: $relative")
                }
                if (destination.exists() && !destination.delete()) {
                    temporary.delete()
                    throw IOException("Could not replace staged asset: $relative")
                }
                if (!temporary.renameTo(destination)) {
                    temporary.delete()
                    throw IOException("Could not finalize staged asset: $relative")
                }
                copied.add(relative)
            }

            val success = missing.isEmpty() && copied.size == requiredAssets.size
            val detail = if (success) {
                "Runtime archives staged privately. They are not extracted and the engine is not initialized."
            } else {
                "Staged ${copied.size} of ${requiredAssets.size} required archives. Missing from APK assets: ${missing.joinToString()}. No extraction or execution was performed."
            }
            Result(success, copied, missing, detail)
        } catch (error: IOException) {
            Result(false, copied, missing, "Could not stage runtime assets: ${error.message ?: "I/O error"}")
        } catch (error: SecurityException) {
            Result(false, copied, missing, "Android denied access while staging runtime assets.")
        }
    }
}
