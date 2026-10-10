package com.cloudplay.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EnginePanel = Color(0xFF111D29)
private val EngineGreen = Color(0xFF21E887)
private val EngineMuted = Color(0xFFAAB6C5)

@Composable
fun EngineIntegrationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val runtimeStatus = remember { WindowsRuntime.inspect(context) }
    val candidatePackages = listOf("com.winlator", "com.winlator.cmod")
    val installedPackage = remember {
        candidatePackages.firstOrNull { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }
    }
    var message by remember {
        mutableStateOf(
            if (installedPackage != null) "Motor externo detectado: $installedPackage"
            else "Nenhum motor externo conhecido foi detectado."
        )
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("‹ Voltar", color = EngineGreen, modifier = Modifier.clickable(onClick = onBack))
        Text("Motor de execução PC", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "O objetivo é executar jogos localmente no Android, sem PC ou streaming. O motor precisa de Wine, Box64, bibliotecas ARM64, um root filesystem e uma camada gráfica compatível.",
            color = EngineMuted,
            fontSize = 13.sp
        )

        Card(colors = CardDefaults.cardColors(containerColor = EnginePanel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Motor integrado CloudyPlay", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    when (runtimeStatus.state) {
                        WindowsRuntime.State.NOT_AVAILABLE -> "Estado: ainda não instalado no APK"
                        WindowsRuntime.State.ASSETS_MISSING -> "Estado: arquivos do motor incompletos"
                        WindowsRuntime.State.FILES_DETECTED_NOT_INITIALIZED -> "Estado: arquivos detectados, motor não inicializado"
                    },
                    color = EngineGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(runtimeStatus.detail, color = EngineMuted, fontSize = 12.sp)
                Text(
                    "Detectar arquivos não prova que o motor funciona. O CloudyPlay ainda não executa programas Windows localmente.",
                    color = EngineMuted,
                    fontSize = 12.sp
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = EnginePanel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Winlator externo (temporário)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (installedPackage != null) "Possível instalação encontrada: $installedPackage"
                    else "Nenhum pacote Winlator conhecido foi encontrado.",
                    color = EngineMuted,
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val launchIntent = installedPackage?.let {
                            context.packageManager.getLaunchIntentForPackage(it)
                        }
                        if (launchIntent != null) {
                            runCatching { context.startActivity(launchIntent) }
                                .onSuccess { message = "Winlator aberto. Esse caminho ainda é externo ao CloudyPlay." }
                                .onFailure { message = "Não foi possível abrir o motor instalado." }
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/brunodev85/winlator/releases")))
                            }.onSuccess {
                                message = "Página oficial aberta. A compatibilidade com a GPU Mali do M22 não é garantida."
                            }.onFailure { message = "Não foi possível abrir a página oficial." }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (installedPackage != null) "Abrir motor externo" else "Ver projeto de referência")
                }
                Text(message, color = EngineGreen, fontSize = 12.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = EnginePanel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Próximos bloqueios técnicos", color = EngineGreen, fontWeight = FontWeight.Bold)
                Text("• Compilar/empacotar componentes nativos para arm64-v8a com licenças e avisos preservados.", color = EngineMuted, fontSize = 12.sp)
                Text("• Preparar o root filesystem e a inicialização JNI do runtime.", color = EngineMuted, fontSize = 12.sp)
                Text("• Validar a rota gráfica no Galaxy M22 com GPU Mali antes de prometer jogos 3D.", color = EngineMuted, fontSize = 12.sp)
                Text("• Só então ligar o botão Executar da biblioteca ao motor e testar um executável simples.", color = EngineMuted, fontSize = 12.sp)
            }
        }
    }
}
