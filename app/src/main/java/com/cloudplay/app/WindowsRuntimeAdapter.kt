package com.cloudplay.app

import android.content.Context
import java.io.File

/**
 * Boundary between CloudyPlay's game library and a future embedded Windows engine.
 *
 * This interface intentionally has no fake implementation: until Wine/Box64 and
 * the graphics/runtime assets are integrated, CloudyPlay must report that launch
 * is unavailable instead of pretending an EXE was started.
 */
interface WindowsRuntimeAdapter {
    /**
     * Returns whether a real runtime has completed initialization on this device.
     * Merely finding files on disk must not return true.
     */
    fun isInitialized(context: Context): Boolean

    /**
     * Starts an executable using the initialized runtime.
     *
     * The caller must pass a file from CloudyPlay's private game-imports folder.
     * Implementations must validate the path and must not execute arbitrary shell
     * commands assembled from user-controlled input.
     */
    fun launch(context: Context, executable: File): LaunchResult

    data class LaunchResult(
        val started: Boolean,
        val message: String,
        val processId: Int? = null
    )
}
