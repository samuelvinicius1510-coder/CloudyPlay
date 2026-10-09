package com.cloudplay.app

import android.content.Intent
import android.net.Uri
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

@Composable
fun LocalGameLibrary(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cloudyplay_library", android.content.Context.MODE_PRIVATE) }
    var files by remember { mutableStateOf(prefs.getString("files", "").orEmpty().split("\n").filter { it.isNotBlank() }) }
    var message by remember { mutableStateOf("Importe um instalador ou executável que você possui.") }

    fun tryLaunchInEngine(item: String) {
        val uriText = item.substringAfter(" | ", "")
        val fileUri = runCatching { Uri.parse(uriText) }.getOrNull()
        if (fileUri == null || uriText.isBlank()) {
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

        val handedOff = runCatching {
            context.startActivity(viewIntent)
        }.isSuccess

        if (handedOff) {
            message = "O Android entregou o arquivo ao aplicativo compatível. Isso não confirma que o jogo iniciou."
            return
        }

        val engineIntent = enginePackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (engineIntent != null && runCatching { context.startActivity(engineIntent) }.isSuccess) {
            message = "O motor foi aberto, mas não aceitou o arquivo diretamente. Importe-o dentro do motor e crie um atalho."
        } else {
            message = "Nenhum motor compatível foi encontrado. Instale um motor confiável e importe o arquivo por ele."
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull() ?: "Arquivo de jogo"
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("exe", "msi", "zip", "7z", "rar", "iso")) {
                message = "Tipo de arquivo não reconhecido: " + name
            } else {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val entry = name + " | " + uri.toString()
                files = files.filterNot { it.endsWith(uri.toString()) } + entry
                prefs.edit().putString("files", files.joinToString("\n")).apply()
                message = name + " foi adicionado. Ainda não está instalado nem pronto para executar."
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("‹ Voltar", modifier = Modifier.clickable(onClick = onBack), fontSize = 16.sp)
        Text("Minha biblioteca", fontSize = 27.sp)
        Text("Biblioteca local com tentativa de entrega do arquivo ao motor instalado. A execução direta depende do suporte do motor.")
        Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Importar arquivo") }
        Text(message, fontSize = 12.sp)
        Text("Arquivos importados: " + files.size, fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(files) { item ->
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.substringBefore(" | "))
                        Text("Arquivo local • toque em Executar para tentar abrir no motor", fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { tryLaunchInEngine(item) }) { Text("Executar") }
                    TextButton(onClick = {
                        files = files.filterNot { it == item }
                        prefs.edit().putString("files", files.joinToString("\n")).apply()
                    }) { Text("Remover") }
                    }
                }
            }
        }
    }
}
