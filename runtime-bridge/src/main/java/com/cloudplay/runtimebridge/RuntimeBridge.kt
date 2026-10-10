package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Boundary for integrating Winlator's native runtime into CloudyPlay.
 * This bridge prepares private workspace directories only; it never claims
 * Wine/Box64 is installed or initialized until native bootstrap is implemented.
 */
object RuntimeBridge {
    enum class State { MISSING_NATIVE_COMPONENTS, MISSING_ROOTFS, NOT_INITIALIZED }

    data class Inspection(val state: State, val detail: String)
    data class WorkspaceResult(val directory: File?, val prepared: Boolean, val detail: String)

    /**
     * Creates a canonical workspace inside the app-private files directory.
     * No archives are extracted and no executable/native code is launched here.
     */
    fun prepareWorkspace(context: Context): WorkspaceResult {
        return try {
            val privateRoot = context.filesDir.canonicalFile
            val root = File(privateRoot, RUNTIME_DIRECTORY).canonicalFile
            if (!root.path.startsWith(privateRoot.path + File.separator)) {
                return WorkspaceResult(null, false, "Runtime path escaped the app-private directory.")
            }

            val directories = listOf(
                root,
                File(root, "rootfs"),
                File(root, "prefixes"),
                File(root, "temp"),
                File(root, "logs")
            )
            directories.forEach { directory ->
                if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                    throw IOException("Could not create directory: " + directory.name)
                }
                val canonical = directory.canonicalFile
                if (canonical != root && !canonical.path.startsWith(root.path + File.separator)) {
                    throw IOException("Unexpected path outside runtime workspace")
                }
            }

            WorkspaceResult(
                root,
                true,
                "Private workspace prepared. Wine/Box64 payload and native bootstrap are still missing."
            )
        } catch (error: IOException) {
            WorkspaceResult(null, false, "Could not prepare runtime workspace: " + (error.message ?: "I/O error"))
        } catch (error: SecurityException) {
            WorkspaceResult(null, false, "Android denied access to the private runtime workspace.")
        }
    }

    fun inspect(context: Context): Inspection {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val hasNativeComponents = nativeDir.listFiles().orEmpty().any {
            it.name.contains("box64", true) || it.name.contains("wine", true)
        }
        if (!hasNativeComponents) {
            return Inspection(
                State.MISSING_NATIVE_COMPONENTS,
                "Wine/Box64 runtime components are not bundled in this APK."
            )
        }

        val root = File(context.filesDir, RUNTIME_DIRECTORY + "/rootfs")
        if (!root.isDirectory || root.list().isNullOrEmpty()) {
            return Inspection(State.MISSING_ROOTFS, "The private Wine root filesystem is missing or empty.")
        }

        return Inspection(
            State.NOT_INITIALIZED,
            "Files were detected, but native initialization has not been implemented or verified."
        )
    }

    private const val RUNTIME_DIRECTORY = "windows-runtime"
}
