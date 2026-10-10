package com.cloudplay.runtimebridge

import android.content.Context
import java.io.File

/**
 * Minimal boundary for integrating Winlator's native runtime into CloudyPlay.
 *
 * This module intentionally does not bundle or load third-party native libraries
 * yet. It checks prerequisites and fails closed until the reviewed Wine/Box64
 * binaries and rootfs have been integrated.
 */
object RuntimeBridge {
    enum class State { MISSING_NATIVE_COMPONENTS, MISSING_ROOTFS, NOT_INITIALIZED }

    data class Inspection(val state: State, val detail: String)

    fun inspect(context: Context): Inspection {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val hasNativeComponents = nativeDir.listFiles().orEmpty().any {
            it.name.contains("box64", true) || it.name.contains("wine", true)
        }
        if (!hasNativeComponents) {
            return Inspection(State.MISSING_NATIVE_COMPONENTS, "Wine/Box64 ARM64 libraries are not bundled.")
        }
        val root = File(context.filesDir, "windows-runtime/rootfs")
        if (!root.isDirectory || root.list().isNullOrEmpty()) {
            return Inspection(State.MISSING_ROOTFS, "The private Wine root filesystem is missing or empty.")
        }
        return Inspection(State.NOT_INITIALIZED, "Runtime files exist, but native initialization is not implemented.")
    }
}
