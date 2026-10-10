package com.cloudplay.app

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Fail-closed boundary for the future embedded Windows runtime.
 * Prepares private directories, but does not pretend Wine/Box64 is initialized.
 */
class EmbeddedWindowsRuntimeAdapter : WindowsRuntimeAdapter {

    data class WorkspaceResult(val directory: File?, val message: String)

    override fun isInitialized(context: Context): Boolean = false

    fun prepareWorkspace(context: Context): WorkspaceResult {
        val root = File(context.filesDir, RUNTIME_DIR_NAME)
        return try {
            if (!root.exists() && !root.mkdirs()) {
                return WorkspaceResult(null, "Não foi possível criar a pasta privada do runtime.")
            }
            val canonicalRoot = root.canonicalFile
            for (name in listOf("rootfs", "prefixes", "temp", "logs")) {
                val dir = File(canonicalRoot, name)
                if (!dir.exists() && !dir.mkdirs()) {
                    return WorkspaceResult(null, "Não foi possível preparar a pasta $name.")
                }
                if (!dir.canonicalPath.startsWith(canonicalRoot.path + File.separator)) {
                    return WorkspaceResult(null, "O caminho do runtime não passou na validação de segurança.")
                }
            }
            WorkspaceResult(canonicalRoot, "Pastas privadas preparadas. Os componentes nativos ainda não foram instalados.")
        } catch (_: IOException) {
            WorkspaceResult(null, "Falha ao validar o armazenamento privado do runtime.")
        } catch (_: SecurityException) {
            WorkspaceResult(null, "O Android bloqueou o acesso ao armazenamento privado do runtime.")
        }
    }

    override fun launch(context: Context, executable: File): WindowsRuntimeAdapter.LaunchResult {
        val importsRoot = File(context.filesDir, "game-imports").canonicalFile
        val selected = try {
            executable.canonicalFile
        } catch (_: IOException) {
            return unavailable("Não foi possível validar o caminho do jogo.")
        }
        if (!selected.path.startsWith(importsRoot.path + File.separator)) {
            return unavailable("Por segurança, só é permitido executar arquivos importados pela biblioteca privada do CloudyPlay.")
        }
        if (!selected.isFile || !selected.extension.equals("exe", ignoreCase = true)) {
            return unavailable("Selecione um arquivo .exe válido da biblioteca.")
        }
        return unavailable("Arquivo validado. O runtime Wine/Box64 ainda não foi integrado e inicializado neste APK.")
    }

    private fun unavailable(message: String) =
        WindowsRuntimeAdapter.LaunchResult(started = false, message = message)

    companion object {
        const val RUNTIME_DIR_NAME = "windows-runtime"
    }
}
