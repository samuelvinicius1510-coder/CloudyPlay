package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File

/**
 * Fail-closed boundary for the future native bootstrap.
 *
 * This class intentionally does not call System.loadLibrary or execute Box64:
 * CloudyPlay does not yet package the upstream JNI/native runtime, and loading
 * guessed library names would turn a missing dependency into a crash.
 */
object RuntimeBootstrap {
    enum class State {
        WORKSPACE_MISSING,
        ROOTFS_MISSING,
        ROOTFS_INCOMPLETE,
        NATIVE_BRIDGE_MISSING,
        READY
    }

    data class Status(
        val state: State,
        val canLaunch: Boolean,
        val detail: String
    )

    /**
     * Performs filesystem and packaged-native preflight only. READY is reserved
     * for a future implementation that actually initializes JNI, Box64 and Wine;
     * this preflight never returns READY by itself.
     */
    fun inspect(context: Context): Status {
        val workspaceResult = RuntimeBridge.prepareWorkspace(context)
        val workspace = workspaceResult.directory
            ?: return Status(State.WORKSPACE_MISSING, false, workspaceResult.detail)

        val rootfs = File(workspace, "rootfs")
        if (!rootfs.isDirectory) {
            return Status(
                State.ROOTFS_MISSING,
                false,
                "RootFS is not installed. The runtime archives must be extracted before native startup."
            )
        }

        val requiredPaths = listOf(
            File(rootfs, "usr"),
            File(rootfs, "bin"),
            File(rootfs, "opt")
        )
        val missing = requiredPaths.filterNot { it.isDirectory }
        if (missing.isNotEmpty()) {
            return Status(
                State.ROOTFS_INCOMPLETE,
                false,
                "RootFS is incomplete; missing directories: " +
                    missing.joinToString { it.name }
            )
        }

        // Do not infer native readiness from a filename containing "wine" or
        // "box64". Native entry points and their ABI must be explicitly verified.
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val requiredNative = listOf("libwinlator.so", "libbox64.so")
        val missingNative = requiredNative.filterNot { File(nativeDir, it).isFile }
        if (missingNative.isNotEmpty()) {
            return Status(
                State.NATIVE_BRIDGE_MISSING,
                false,
                "Native bootstrap is not integrated. Missing verified libraries: " +
                    missingNative.joinToString()
            )
        }

        // Even if files happen to exist, only a real JNI initialization handshake
        // may transition this component to READY.
        return Status(
            State.NATIVE_BRIDGE_MISSING,
            false,
            "Native libraries were found, but no verified JNI initialization handshake is implemented."
        )
    }
}
