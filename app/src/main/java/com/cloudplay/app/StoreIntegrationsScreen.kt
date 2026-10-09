package com.cloudplay.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val StorePanel = androidx.compose.ui.graphics.Color(0xFF111D29)
private val StoreGreen = androidx.compose.ui.graphics.Color(0xFF21E887)
private val StoreMuted = androidx.compose.ui.graphics.Color(0xFFAAB6C5)

@Composable
fun StoreIntegrationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val stores = listOf(
        Triple("Steam", "https://store.steampowered.com/login/", "Login oficial da Steam"),
        Triple("Epic Games", "https://www.epicgames.com/id/login", "Login oficial da Epic Games"),
        Triple("GOG", "https://login.gog.com/", "Login oficial da GOG")
    )
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("‹ Voltar", color = StoreGreen, modifier = Modifier.clickable(onClick = onBack))
        Text("Lojas de jogos", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Abra as páginas oficiais para entrar nas lojas. Esta etapa ainda não importa automaticamente as bibliotecas nem salva sessões de login dentro do CloudyPlay.", color = StoreMuted, fontSize = 13.sp)
        stores.forEach { store ->
            Card(colors = CardDefaults.cardColors(containerColor = StorePanel)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(store.first, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(store.third, color = StoreMuted, fontSize = 12.sp)
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(store.second))) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Abrir login oficial", color = androidx.compose.ui.graphics.Color(0xFF071019)) }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = StorePanel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("O que falta para vincular de verdade", color = StoreGreen, fontWeight = FontWeight.Bold)
                Text("Steam: fluxo OpenID e consulta de biblioteca conforme as permissões disponíveis. Epic e GOG: verificar os métodos oficiais disponíveis para aplicativos terceiros. Não digite senhas em telas falsas nem compartilhe cookies de sessão.", color = StoreMuted, fontSize = 12.sp)
            }
        }
    }
}
