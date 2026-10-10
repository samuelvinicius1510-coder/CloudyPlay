package com.cloudplay.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val INTERNAL_PREFIX = "internal:"
private val SUPPORTED_EXTENSIONS = setOf("exe", "msi", "zip", "7z", "rar", "iso")

private fun readableSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(java.util.Locale.getDefault(), "%.2f GB", gb)
}

@Composable
fun LocalGameLibrary(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cloudyplay_library", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(prefs.getString("files", "").orEmpty().split("\n").filter { it.isNotBlank() }) }
    var message by remember { mutableStateOf("Importe um instalador ou executável que você possui.") }
    var importing by remember { mutableStateOf(false) }

    fun saveFiles(updated: List<String>) {
        files = updated
        prefs.edit().putString("files", updated.joinToString("\n")).apply()
    }

    fun importedFile(item: String): File? {
        val reference = item.substringAfter(" | ", "")
        if (!reference.startsWith(INTERNAL_PREFIX)) return null
        return File(context.filesDir, "game-imports/${reference.removePrefix(INTERNAL_PREFIX)}")
    }

    fun tryLaunchInEngine(item: String) {
        val reference = item.substringAfter(" | ", "")
        if (reference.startsWith(INTERNAL_PREFIX)) {
            val file = importedFile(item)
            message = when {
                file == null || !file.isFile -> "O arquivo não foi encontrado no armazenamento do app. Remova-o e importe novamente."
                else -> "Arquivo presente (${readableSize(file.length())}). A execução ainda depende da integração de um motor Windows."
            }
            return
        }

        val fileUri = runCatching { Uri.parse(reference) }.getOrNull()
        if (fileUri == null || reference.isBlank()) {
            message = "Não consegui ler a referência deste arquivo. Remova e importe novamente."
            return
        }

        val packageNames = listOf("com.winlator", "com.winlator.cmod")
        val enginePackage = packageNames.firstOrNull { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (enginePackage != null) setPackage(enginePackage)
        }
        if (runCatching { context.startActivity(viewIntent) }.isSuccess) {
            message = "O Android entregou o arquivo ao aplicativo compatível. Isso não confirma que o jogo iniciou."
            return
        }

        val engineIntent = enginePackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (engineIntent != null && runCatching { context.startActivity(engineIntent) }.isSuccess) {
            message = "O motor foi aberto, mas não aceitou o arquivo diretamente. Importe-o dentro do motor."
        } else {
            message = "Nenhum motor compatível foi encontrado. O CloudyPlay ainda não inclui um motor Windows próprio."
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                importing = true
                message = "Copiando arquivo para o armazenamento privado do CloudyPlay…"
                val result = withContext(Dispatchers.IO) {
                    var partialFile: File? = null
                    try {
                        val name = context.contentResolver.query(
                            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        } ?: "Arquivo de jogo"
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext !in SUPPORTED_EXTENSIONS) {
                            throw IllegalArgumentException("Tipo não suportado: $name. Escolha EXE, MSI, ZIP, 7Z, RAR ou ISO.")
                        }
                        val targetDir = File(context.filesDir, "game-imports")
                        if (!targetDir.exists() && !targetDir.mkdirs()) {
                            throw IOException("Não foi possível criar a pasta de importação.")
                        }
                        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "game-file.$ext" }
                        partialFile = File(targetDir, "${System.currentTimeMillis()}_$safeName")
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("O Android não conseguiu abrir o arquivo selecionado.")
                        input.use { source ->
                            partialFile!!.outputStream().buffered().use { destination ->
                                source.copyTo(destination)
                            }
                        }
                        if (partialFile!!.length() == 0L) {
                            throw IOException("O arquivo importado está vazio.")
                        }
                        Triple(name, partialFile!!.name, partialFile!!.length())
                    } catch (error: Exception) {
                        partialFile?.delete()
                        throw error
                    }
                }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(it) }
                )
                importing = false
                result.onSuccess { (name, storedName, size) ->
                    val entry = "$name | $INTERNAL_PREFIX$storedName"
                    saveFiles(files.filterNot { it.substringAfter(" | ", "") == "$INTERNAL_PREFIX$storedName" } + entry)
                    message = "$name importado (${readableSize(size)}). Foi copiado, mas ainda não foi instalado nem executado."
                }.onFailure { error ->
                    message = error.message ?: "Falha ao importar o arquivo."
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("‹ Voltar", modifier = Modifier.clickable(onClick = onBack), fontSize = 16.sp)
        Text("Minha biblioteca", fontSize = 27.sp)
        Text("Os arquivos são copiados para o armazenamento privado do CloudyPlay. O espaço usado depende do tamanho dos arquivos importados.")
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = !importing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (importing) "Importando…" else "＋ Importar arquivo") }
        Text(message, fontSize = 12.sp)
        Text("Arquivos importados: ${files.size}", fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(files) { item ->
                val localFile = importedFile(item)
                val isInternal = item.substringAfter(" | ", "").startsWith(INTERNAL_PREFIX)
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.substringBefore(" | "))
                        Text(
                            when {
                                isInternal && localFile?.isFile == true ->
                                    "Cópia privada • ${readableSize(localFile.length())} • aguardando motor"
                                isInternal -> "Arquivo ausente • importe novamente"
                                else -> "Referência antiga • depende de um motor instalado"
                            },
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { tryLaunchInEngine(item) }) { Text("Executar") }
                        TextButton(onClick = {
                            localFile?.delete()
                            saveFiles(files.filterNot { it == item })
                            message = "Item removido da biblioteca."
                        }) { Text("Remover") }
                    }
                }
            }
        }
    }
}
