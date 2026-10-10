package com.cloudplay.app

import android.content.Context
import java.io.File

/**
 * Honest status boundary for the future embedded Windows runtime.
 *
 * Detecting files is not the same as initializing Wine/Box64. This app does
 * not yet ship or initialize an embedded runtime, so this inspector never
 * reports READY or RUNNING.
 */
object WindowsRuntime {
    enum class State {
        NOT_AVAILABLE,
        ASSETS_MISSING,
        FILES_DETECTED_NOT_INITIALIZED
    }

    data class Status(
        val state: State,
        val detail: String
    )

    fun inspect(context: Context): Status {
        val nativeDir = context.applicationInfo.nativeLibraryDir?.let(::File)
        val hasArm64Runtime = nativeDir?.listFiles()?.any {
            it.name.contains("box64", ignoreCase = true) ||
                it.name.contains("wine", ignoreCase = true)
        } == true
        val runtimeRoot = File(context.filesDir, "windows-runtime")
        val hasRootFs = File(runtimeRoot, "rootfs").isDirectory

        return when {
            hasArm64Runtime && hasRootFs -> Status(
                State.FILES_DETECTED_NOT_INITIALIZED,
                "Arquivos encontrados, mas o motor ainda não foi inicializado nem testado. A execução não está disponível."
            )
            !hasArm64Runtime && !hasRootFs -> Status(
                State.NOT_AVAILABLE,
                "O CloudyPlay ainda não inclui os binários nativos Wine/Box64 nem o root filesystem necessários."
            )
            else -> Status(
                State.ASSETS_MISSING,
                "A instalação do runtime está incompleta: faltam binários nativos ou root filesystem."
            )
        }
    }
}
