package com.cloudplay.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var showAdmin by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var showStores by remember { mutableStateOf(false) }
    var showEngine by remember { mutableStateOf(false) }
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
            when {
                showAdmin -> AdminPanel(onBack = { showAdmin = false })
                showLibrary -> LocalGameLibrary(onBack = { showLibrary = false })
                showStores -> StoreIntegrationsScreen(onBack = { showStores = false })
                showEngine -> EngineIntegrationScreen(onBack = { showEngine = false })
                selectedGame != null -> SessionScreen(game = selectedGame!!, onBack = { selectedGame = null })
                else -> {
                    val homeListState = rememberLazyListState()
                    val homeScope = rememberCoroutineScope()
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val viewportHeight = maxHeight
                        LazyColumn(
                            state = homeListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☁", color = Green, fontSize = 34.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("CloudyPlay", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Jogue pela nuvem", color = Muted, fontSize = 13.sp)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showEngine = true }, modifier = Modifier.weight(1f)) {
                                    Text("Motor PC", color = Green, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                TextButton(onClick = { showStores = true }, modifier = Modifier.weight(1f)) {
                                    Text("Lojas", color = Green, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                TextButton(onClick = { showLibrary = true }, modifier = Modifier.weight(1f)) {
                                    Text("Biblioteca", color = Green, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                TextButton(onClick = { showAdmin = true }, modifier = Modifier.weight(1f)) {
                                    Text("Admin", color = Green, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
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
                            Button(onClick = { showLibrary = true }) {
                                Text("Abrir minha biblioteca  →", color = Bg, fontWeight = FontWeight.Bold)
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
                        Text("CloudyPlay v0.2 • A transmissão real será integrada numa próxima etapa.",
                            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                    }
                        }
                        val visibleCount = homeListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        val totalCount = homeListState.layoutInfo.totalItemsCount.coerceAtLeast(1)
                        val thumbHeight = (maxHeight * visibleCount / totalCount).coerceIn(36.dp, maxHeight)
                        val maxScrollIndex = (totalCount - visibleCount).coerceAtLeast(1)
                        val thumbTravel = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
                        val thumbY = thumbTravel * (homeListState.firstVisibleItemIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(18.dp)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                Modifier.fillMaxHeight().width(3.dp)
                                    .background(Color(0xFF263746), RoundedCornerShape(4.dp))
                            )
                            Box(
                                Modifier
                                    .offset(y = thumbY)
                                    .width(8.dp)
                                    .height(thumbHeight)
                                    .background(Green, RoundedCornerShape(8.dp))
                                    .pointerInput(totalCount, viewportHeight) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val trackPx = size.height.toFloat()
                                            val thumbPx = thumbHeight.toPx()
                                            val availablePx = (trackPx - thumbPx).coerceAtLeast(1f)
                                            val currentFraction = (homeListState.firstVisibleItemIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                                            val nextFraction = (currentFraction + dragAmount.y / availablePx).coerceIn(0f, 1f)
                                            homeScope.launch {
                                                homeListState.scrollToItem((nextFraction * maxScrollIndex).toInt())
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanel(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var authorized by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var sessionToken by remember { mutableStateOf("") }
    var adminUserId by remember { mutableStateOf("") }

    // Publishable/anon key is intended for client apps. Never put a service_role/secret key here.
    val projectUrl = "https://yslociopvotmlvgimaww.supabase.co"
    val publishableKey = "sb_publishable_zIgd9ClzlOiqoKhdbiFMPA_mVxZM91-"

    fun signInAndCheckOwner() {
        if (email.isBlank() || password.isBlank() || busy) return
        busy = true
        status = "Verificando sua conta..."
        Thread {
            var resultMessage = "Não foi possível verificar a conta."
            var isOwner = false
            var authenticatedToken = ""
            var authenticatedUserId = ""
            try {
                val authConnection = (URL("$projectUrl/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection)
                authConnection.requestMethod = "POST"
                authConnection.connectTimeout = 15000
                authConnection.readTimeout = 15000
                authConnection.setRequestProperty("Content-Type", "application/json")
                authConnection.setRequestProperty("apikey", publishableKey)
                authConnection.doOutput = true
                val payload = JSONObject().put("email", email.trim()).put("password", password).toString()
                OutputStreamWriter(authConnection.outputStream, Charsets.UTF_8).use { it.write(payload) }
                val authCode = authConnection.responseCode
                val authStream = if (authCode in 200..299) authConnection.inputStream else authConnection.errorStream
                val authBody = authStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                authConnection.disconnect()

                if (authCode !in 200..299) {
                    resultMessage = try {
                        JSONObject(authBody).optString("msg",
                            JSONObject(authBody).optString("message", "E-mail ou senha inválidos."))
                    } catch (_: Exception) { "E-mail ou senha inválidos." }
                } else {
                    val authJson = JSONObject(authBody)
                    val accessToken = authJson.optString("access_token")
                    val userId = authJson.optJSONObject("user")?.optString("id").orEmpty()
                    authenticatedToken = accessToken
                    authenticatedUserId = userId
                    if (accessToken.isBlank() || userId.isBlank()) {
                        resultMessage = "A resposta de autenticação veio incompleta."
                    } else {
                        val profileUrl = "$projectUrl/rest/v1/admin_profiles?user_id=eq.$userId&select=role"
                        val profileConnection = (URL(profileUrl).openConnection() as HttpURLConnection)
                        profileConnection.requestMethod = "GET"
                        profileConnection.connectTimeout = 15000
                        profileConnection.readTimeout = 15000
                        profileConnection.setRequestProperty("apikey", publishableKey)
                        profileConnection.setRequestProperty("Authorization", "Bearer $accessToken")
                        val profileCode = profileConnection.responseCode
                        val profileStream = if (profileCode in 200..299) profileConnection.inputStream else profileConnection.errorStream
                        val profileBody = profileStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        profileConnection.disconnect()
                        if (profileCode in 200..299) {
                            val profiles = JSONArray(profileBody)
                            val role = if (profiles.length() > 0) profiles.getJSONObject(0).optString("role") else ""
                            isOwner = role == "owner"
                            resultMessage = if (isOwner) "Acesso de proprietária confirmado." else "Sua conta não tem permissão de proprietária."
                        } else {
                            resultMessage = "Não foi possível consultar sua permissão. Confira a tabela admin_profiles e as políticas RLS no Supabase."
                        }
                    }
                }
            } catch (_: Exception) {
                resultMessage = "Falha de conexão. Confira sua internet e as configurações do Supabase."
            }
            Handler(Looper.getMainLooper()).post {
                busy = false
                authorized = isOwner
                status = resultMessage
                if (isOwner) {
                    sessionToken = authenticatedToken
                    adminUserId = authenticatedUserId
                } else {
                    password = ""
                    sessionToken = ""
                    adminUserId = ""
                }
            }
        }.start()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("‹  Voltar ao CloudyPlay", color = Green, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.height(8.dp))
        Text("🛡️ Painel administrativo", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Somente a conta proprietária autorizada pode entrar.", color = Muted)
        if (!authorized) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Entrar como proprietária", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; status = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("E-mail da sua conta Supabase") }
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; status = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Button(
                        onClick = { signInAndCheckOwner() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank() && password.isNotBlank() && !busy
                    ) {
                        Text(if (busy) "Verificando..." else "Entrar", color = Bg)
                    }
                    if (status.isNotBlank()) {
                        Text(status, color = if (status.contains("confirmado")) Green else Color(0xFFFFC66D), fontSize = 12.sp)
                    }
                }
            }
            Text("A autenticação é feita pelo Supabase. O aplicativo consulta seu perfil e só libera esta tela se o cargo retornado for owner.", color = Muted, fontSize = 12.sp)
        } else {
            AdminDashboard(
                accessToken = sessionToken,
                userId = adminUserId,
                onBack = onBack,
                onSignOut = {
                    authorized = false
                    email = ""
                    password = ""
                    status = ""
                    sessionToken = ""
                    adminUserId = ""
                }
            )
        }
    }
}

@Composable
private fun AdminFeature(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 25.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 12.sp)
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
