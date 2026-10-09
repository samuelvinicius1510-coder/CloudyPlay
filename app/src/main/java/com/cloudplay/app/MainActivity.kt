package com.cloudplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF071019)
private val Panel = Color(0xFF111D29)
private val Green = Color(0xFF21E887)
private val Muted = Color(0xFFAAB6C5)

data class Game(val title: String, val genre: String, val emoji: String, val status: String)

private val demoGames = listOf(
    Game("Forza Horizon", "Corrida", "🏎️", "Servidor demonstrativo"),
    Game("Block World", "Aventura", "🧱", "Sessão de teste"),
    Game("Battle Arena", "Ação", "🎯", "Em breve"),
    Game("Fantasy Quest", "RPG", "🐉", "Em breve"),
    Game("Street Football", "Esportes", "⚽", "Em breve"),
    Game("Space Explorer", "Aventura", "🚀", "Em breve")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CloudPlayApp() }
    }
}

@Composable
fun CloudPlayApp() {
    var search by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Todos") }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    val genres = listOf("Todos", "Ação", "Aventura", "Corrida", "RPG", "Esportes")
    val filtered = demoGames.filter {
        (selectedGenre == "Todos" || it.genre == selectedGenre) &&
        it.title.contains(search, ignoreCase = true)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Green,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            if (selectedGame == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁", color = Green, fontSize = 34.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("CloudyPlay", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Jogue pela nuvem", color = Muted, fontSize = 13.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("●", color = Green)
                            Text(" Demo", color = Muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        Column(
                            Modifier.fillMaxWidth()
                                .background(Panel, RoundedCornerShape(22.dp))
                                .padding(18.dp)
                        ) {
                            Text("SUA PRÓXIMA PARTIDA COMEÇA AQUI", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("Jogos na nuvem.\nSem downloads enormes.", fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Escolha um título e veja os detalhes da sessão.", color = Muted)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { selectedGame = demoGames.first() }) {
                                Text("Explorar sessão  →", color = Bg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Buscar jogos") },
                            placeholder = { Text("Nome ou título...") }
                        )
                    }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            genres.forEach { genre ->
                                FilterChip(
                                    selected = selectedGenre == genre,
                                    onClick = { selectedGenre = genre },
                                    label = { Text(genre) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Catálogo", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Protótipo • títulos demonstrativos", color = Muted, fontSize = 12.sp)
                    }
                    items(filtered) { game ->
                        GameCard(game = game, onClick = { selectedGame = game })
                    }
                    item {
                        Text("CloudyPlay v0.1 • A transmissão real será integrada numa próxima etapa.",
                            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                SessionScreen(game = selectedGame!!, onBack = { selectedGame = null })
            }
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(Color(0xFF203344), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(game.emoji, fontSize = 34.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(game.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(game.genre, color = Muted, fontSize = 13.sp)
            Text(game.status, color = Green, fontSize = 11.sp)
        }
        Text("›", color = Green, fontSize = 30.sp)
    }
}

@Composable
fun SessionScreen(game: Game, onBack: () -> Unit) {
    var requested by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("‹  Voltar", color = Green, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF101C28), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(game.emoji, fontSize = 64.sp)
                Text(game.title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Prévia da sessão", color = Muted)
                Text("●  Aguardando servidor de jogos", color = Green, fontSize = 13.sp)
            }
        }
        Text("CONTROLES (PRÉVIA)", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(82.dp).background(Panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text("✥", fontSize = 34.sp, color = Green)
            }
            Spacer(Modifier.weight(1f))
            listOf("Y", "X", "B", "A").forEach { key ->
                Box(Modifier.size(42.dp).background(Panel, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text(key, color = Green, fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(onClick = { requested = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (requested) "Solicitação registrada (demo)" else "Solicitar sessão de teste", color = Bg)
        }
        if (requested) {
            Text("Você entrou na fila demonstrativa. Quando o backend for conectado, esta ação poderá solicitar uma sessão real.", color = Green, fontSize = 12.sp)
        }
        Text("Modo demonstração: não há servidor conectado e nenhum jogo está sendo transmitido.", color = Muted, fontSize = 11.sp)
    }
}
