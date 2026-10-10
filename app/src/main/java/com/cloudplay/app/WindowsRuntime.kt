package com.cloudplay.app

import android.content.Context
import java.io.File

/**
 * Boundary for the future embedded Windows runtime.
 *
 * This class deliberately reports NOT_AVAILABLE until the native runtime and
 * its licensed root filesystem are packaged and initialized. It must never
 * report success merely because an executable was imported.
 */
object WindowsRuntime {
    enum class State {
        NOT_AVAILABLE,
        ASSETS_MISSING,
        READY
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
                State.READY,
                "Arquivos do runtime detectados. A inicialização real ainda precisa ser validada."
            )
            !hasArm64Runtime && !hasRootFs -> Status(
                State.NOT_AVAILABLE,
                "O CloudyPlay ainda não inclui binários nativos Wine/Box64 nem root filesystem."
            )
            else -> Status(
                State.ASSETS_MISSING,
                "A instalação do runtime está incompleta: faltam binários nativos ou root filesystem."
            )
        }
    }
}
