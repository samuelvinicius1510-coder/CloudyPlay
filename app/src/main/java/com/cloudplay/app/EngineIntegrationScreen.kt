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

/**
 * First-stage bridge to a separately installed Windows compatibility engine.
 * This does not bundle Wine/Box64 and intentionally does not claim to launch EXEs.
 */
@Composable
fun EngineIntegrationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val candidatePackages = listOf("com.winlator", "com.winlator.cmod")
    val installedPackage = remember {
        candidatePackages.firstOrNull { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }
    }
    var message by remember {
        mutableStateOf(
            if (installedPackage != null) "Motor detectado: $installedPackage"
            else "Nenhum pacote Winlator conhecido foi detectado neste aparelho."
        )
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("‹ Voltar", color = EngineGreen, modifier = Modifier.clickable(onClick = onBack))
        Text("Motor de execução PC", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Primeira etapa: conectar o CloudyPlay a um motor externo de compatibilidade. Wine + Box64 não é uma dependência Android simples; precisa de bibliotecas nativas e uma camada gráfica compatível.",
            color = EngineMuted,
            fontSize = 13.sp
        )

        Card(colors = CardDefaults.cardColors(containerColor = EnginePanel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Winlator", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (installedPackage != null) "Possível instalação encontrada."
                    else "O motor ainda não foi detectado pelo CloudyPlay.",
                    color = EngineMuted
                )
                Button(
                    onClick = {
                        val launchIntent = installedPackage?.let {
                            context.packageManager.getLaunchIntentForPackage(it)
                        }
                        if (launchIntent != null) {
                            runCatching { context.startActivity(launchIntent) }
                                .onSuccess { message = "Winlator aberto. Importe e configure os jogos dentro do próprio motor." }
                                .onFailure { message = "Não foi possível abrir o motor instalado." }
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/brunodev85/winlator/releases")))
                            }.onSuccess {
                                message = "Abri a página oficial de versões. Instale apenas se confiar na origem e confira a compatibilidade do seu aparelho."
                            }.onFailure { message = "Não foi possível abrir a página oficial." }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (installedPackage != null) "Abrir motor instalado" else "Ver página oficial do motor")
                }
                Text(message, color = EngineGreen, fontSize = 12.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = EnginePanel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado real da integração", color = EngineGreen, fontWeight = FontWeight.Bold)
                Text("• O CloudyPlay ainda não contém Wine, Box64 ou bibliotecas gráficas embutidas.", color = EngineMuted, fontSize = 12.sp)
                Text("• Este botão abre o aplicativo externo, se detectado; caso contrário, abre a página oficial.", color = EngineMuted, fontSize = 12.sp)
                Text("• Ainda não transfere um .EXE do catálogo para o motor nem inicia jogos automaticamente.", color = EngineMuted, fontSize = 12.sp)
                Text("• O Galaxy M22 usa GPU Mali; desempenho e compatibilidade gráfica precisam ser testados no aparelho.", color = EngineMuted, fontSize = 12.sp)
            }
        }
    }
}
