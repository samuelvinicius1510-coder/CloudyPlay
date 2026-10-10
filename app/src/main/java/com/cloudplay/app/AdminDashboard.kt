package com.cloudplay.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

private const val ADMIN_PROJECT_URL = "https://yslociopvotmlvgimaww.supabase.co"
private const val ADMIN_PUBLISHABLE_KEY = "sb_publishable_zIgd9ClzlOiqoKhdbiFMPA_mVxZM91-"

private data class AdminApiResult(val ok: Boolean, val message: String, val body: String = "")

private fun adminRequest(path: String, token: String, method: String, body: JSONObject? = null): AdminApiResult {
    return try {
        val connection = (URL("$ADMIN_PROJECT_URL/rest/v1/$path").openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", ADMIN_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Prefer", "return=representation")
        if (body != null) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        AdminApiResult(code in 200..299, if (code in 200..299) "Salvo com sucesso." else parseAdminError(response, code), response)
    } catch (e: Exception) {
        AdminApiResult(false, "Falha de conexão: ${e.message ?: "verifique a internet"}")
    }
}

private fun adminRpc(path: String, token: String, body: JSONObject): AdminApiResult {
    return try {
        val connection = (URL("$ADMIN_PROJECT_URL/rest/v1/rpc/$path").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", ADMIN_PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        AdminApiResult(code in 200..299, if (code in 200..299) "Operação concluída. Saldo atual: $response" else parseAdminError(response, code), response)
    } catch (e: Exception) {
        AdminApiResult(false, "Falha de conexão: ${e.message ?: "verifique a internet"}")
    }
}

private fun parseAdminError(body: String, code: Int): String = try {
    val json = JSONObject(body)
    json.optString("message", json.optString("msg", "Erro HTTP $code"))
} catch (_: Exception) { "Erro HTTP $code. Confira a migração e as permissões do Supabase." }

@Composable
fun AdminDashboard(accessToken: String, userId: String, onBack: () -> Unit, onSignOut: () -> Unit) {
    var targetUser by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var gameTitle by remember { mutableStateOf("") }
    var gameGenre by remember { mutableStateOf("Aventura") }
    var steamAppId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Painel conectado. Execute a migração SQL antes de usar as operações.") }
    var busy by remember { mutableStateOf(false) }

    fun runAdminOperation(block: () -> AdminApiResult) {
        if (busy) return
        busy = true
        status = "Processando..."
        Thread {
            val result = block()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                status = result.message
                if (result.ok) {
                    announcementTitle = if (result.message.startsWith("Salvo")) "" else announcementTitle
                }
            }
        }.start()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("‹ Voltar ao CloudyPlay", color = Color(0xFF21E887), modifier = Modifier.clickable(onClick = onBack))
        Text("Painel administrativo", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Sessão autenticada • proprietário verificado", color = Color(0xFF21E887), fontSize = 12.sp)
        Text("ID da sua conta: $userId", color = Color(0xFFAAB6C5), fontSize = 11.sp)

        AdminSection("💳 Créditos de usuário") {
            OutlinedTextField(targetUser, { targetUser = it.trim() }, Modifier.fillMaxWidth(), label = { Text("UUID do usuário Supabase") }, singleLine = true)
            OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Quantidade (positiva para adicionar, negativa para remover)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Motivo / referência") }, singleLine = true)
            Button(
                onClick = {
                    val delta = amount.toLongOrNull()
                    if (targetUser.isBlank() || delta == null || delta == 0L) status = "Informe um UUID e uma quantidade diferente de zero."
                    else runAdminOperation {
                        adminRpc("admin_adjust_credits", accessToken, JSONObject()
                            .put("p_target_user_id", targetUser)
                            .put("p_delta", delta)
                            .put("p_reason", reason))
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Aplicar alteração de créditos") }
            Text("A alteração é feita no banco, impede saldo negativo e cria registro de auditoria.", color = Color(0xFFAAB6C5), fontSize = 11.sp)
        }

        AdminSection("📢 Publicar aviso na tela inicial") {
            OutlinedTextField(announcementTitle, { announcementTitle = it }, Modifier.fillMaxWidth(), label = { Text("Título do aviso") }, singleLine = true)
            OutlinedTextField(announcementBody, { announcementBody = it }, Modifier.fillMaxWidth(), label = { Text("Mensagem") }, minLines = 3)
            Button(onClick = {
                if (announcementTitle.isBlank() || announcementBody.isBlank()) status = "Preencha o título e a mensagem."
                else runAdminOperation {
                    adminRequest("public_announcements", accessToken, "POST",
                        JSONObject().put("title", announcementTitle).put("body", announcementBody).put("active", true).put("created_by", userId))
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Publicar aviso") }
            Text("Os avisos ativos ficam disponíveis pela API para a tela inicial. A tela inicial ainda precisa ser conectada a essa tabela.", color = Color(0xFFAAB6C5), fontSize = 11.sp)
        }

        AdminSection("🎮 Adicionar jogo ao catálogo") {
            OutlinedTextField(gameTitle, { gameTitle = it }, Modifier.fillMaxWidth(), label = { Text("Nome do jogo") }, singleLine = true)
            OutlinedTextField(gameGenre, { gameGenre = it }, Modifier.fillMaxWidth(), label = { Text("Gênero") }, singleLine = true)
            OutlinedTextField(steamAppId, { steamAppId = it }, Modifier.fillMaxWidth(), label = { Text("Steam App ID (opcional)") }, singleLine = true)
            Button(onClick = {
                if (gameTitle.isBlank()) status = "Informe o nome do jogo."
                else runAdminOperation {
                    val payload = JSONObject().put("title", gameTitle).put("genre", gameGenre.ifBlank { "Outros" })
                        .put("active", true).put("created_by", userId)
                    if (steamAppId.isNotBlank()) payload.put("steam_app_id", steamAppId)
                    adminRequest("game_catalog", accessToken, "POST", payload)
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Adicionar jogo") }
            Text("Isso cadastra um título no catálogo; não baixa o jogo da Steam nem garante que ele execute no Android.", color = Color(0xFFAAB6C5), fontSize = 11.sp)
        }

        Surface(color = Color(0xFF111D29), shape = MaterialTheme.shapes.medium) {
            Text(status, Modifier.fillMaxWidth().padding(14.dp), color = if (status.contains("sucesso") || status.contains("Saldo atual") || status.contains("concluída")) Color(0xFF21E887) else Color(0xFFFFC66D), fontSize = 12.sp)
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sair do painel") }
    }
}

@Composable
private fun AdminSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111D29))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            content()
        })
    }
}

