package com.cloudplay.app

import android.content.Intent
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
        Text("Importe arquivos locais. A execução via Wine + Box64 ainda precisa ser integrada.")
        Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Importar arquivo") }
        Text(message, fontSize = 12.sp)
        Text("Arquivos importados: " + files.size, fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(files) { item ->
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.substringBefore(" | "))
                        Text("Importado • execução ainda não disponível", fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        files = files.filterNot { it == item }
                        prefs.edit().putString("files", files.joinToString("\n")).apply()
                    }) { Text("Remover") }
                }
            }
        }
    }
}
