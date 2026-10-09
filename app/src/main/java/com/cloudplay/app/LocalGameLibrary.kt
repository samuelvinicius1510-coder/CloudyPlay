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

    fun tryLaunchInEngine(item: String) {
        val reference = item.substringAfter(" | ", "")
        if (reference.startsWith(INTERNAL_PREFIX)) {
            message = "Arquivo copiado para o armazenamento privado do CloudyPlay. A execução interna ainda depende da integração do motor Windows."
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
                    runCatching {
                        val name = context.contentResolver.query(
                            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        } ?: "Arquivo de jogo"
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext !in setOf("exe", "msi", "zip", "7z", "rar", "iso")) {
                            throw IllegalArgumentException("Tipo de arquivo não reconhecido: $name")
                        }
                        val targetDir = File(context.filesDir, "game-imports").apply { mkdirs() }
                        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "game-file.$ext" }
                        val target = File(targetDir, "${System.currentTimeMillis()}_$safeName")
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("O Android não conseguiu abrir o arquivo selecionado.")
                        input.use { source -> target.outputStream().buffered().use(source::copyTo) }
                        name to target.name
                    }
                }
                importing = false
                result.onSuccess { (name, storedName) ->
                    val entry = "$name | $INTERNAL_PREFIX$storedName"
                    saveFiles(files.filterNot { it.substringAfter(" | ", "") == "$INTERNAL_PREFIX$storedName" } + entry)
                    message = "$name copiado para o espaço privado do app. Ainda não foi instalado nem executado."
                }.onFailure { error ->
                    message = error.message ?: "Falha ao importar o arquivo."
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("‹ Voltar", modifier = Modifier.clickable(onClick = onBack), fontSize = 16.sp)
        Text("Minha biblioteca", fontSize = 27.sp)
        Text("Os arquivos importados são copiados para o espaço privado do CloudyPlay, preparando-os para um futuro motor integrado.")
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = !importing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (importing) "Importando…" else "＋ Importar arquivo") }
        Text(message, fontSize = 12.sp)
        Text("Arquivos importados: ${files.size}", fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(files) { item ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.substringBefore(" | "))
                        Text(
                            if (item.substringAfter(" | ", "").startsWith(INTERNAL_PREFIX))
                                "Cópia privada • aguardando motor integrado"
                            else "Referência antiga • execução depende do motor instalado",
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { tryLaunchInEngine(item) }) { Text("Executar") }
                        TextButton(onClick = {
                            val ref = item.substringAfter(" | ", "")
                            if (ref.startsWith(INTERNAL_PREFIX)) {
                                File(context.filesDir, "game-imports/${ref.removePrefix(INTERNAL_PREFIX)}").delete()
                            }
                            saveFiles(files.filterNot { it == item })
                        }) { Text("Remover") }
                    }
                }
            }
        }
    }
}
