package com.cloudplay.app

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Fail-closed integration boundary for the future embedded Winlator runtime.
 *
 * This class intentionally does not execute an EXE yet: the upstream Wine/Box64
 * runtime, root filesystem, native libraries and graphics backend have not been
 * legally reviewed and adapted into this APK. It validates the game selection
 * and returns a truthful unavailable result until a real runtime backend exists.
 */
class EmbeddedWindowsRuntimeAdapter : WindowsRuntimeAdapter {

    override fun isInitialized(context: Context): Boolean = false

    override fun launch(context: Context, executable: File): WindowsRuntimeAdapter.LaunchResult {
        val importsRoot = File(context.filesDir, "game-imports").canonicalFile
        val selected = try {
            executable.canonicalFile
        } catch (_: IOException) {
            return unavailable("Não foi possível validar o caminho do jogo.")
        }

        if (!selected.path.startsWith(importsRoot.path + File.separator)) {
            return unavailable("Por segurança, só é permitido executar arquivos importados pela biblioteca do CloudyPlay.")
        }

        if (!selected.isFile || !selected.extension.equals("exe", ignoreCase = true)) {
            return unavailable("Selecione um arquivo .exe válido da biblioteca.")
        }

        // Do not invoke ProcessBuilder, shell commands, or pretend the game started.
        return unavailable(
            "O arquivo foi validado, mas o runtime Windows integrado ainda não está empacotado e inicializado neste APK."
        )
    }

    private fun unavailable(message: String) =
        WindowsRuntimeAdapter.LaunchResult(started = false, message = message)
}
