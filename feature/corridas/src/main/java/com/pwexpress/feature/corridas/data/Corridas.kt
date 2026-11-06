package com.pwexpress.feature.corridas.data

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.pwexpress.core.PwCore
import com.pwexpress.core.network.HttpResposta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Estado das corridas no app: oferta pendente e corrida ativa.
 * Não conhece o rastreamento: o módulo :app liga uma coisa à outra.
 */
object Corridas {

    private lateinit var app: Context
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _oferta = MutableStateFlow<Oferta?>(null)
    val oferta: StateFlow<Oferta?> = _oferta.asStateFlow()

    private val _ativa = MutableStateFlow<Corrida?>(null)
    val ativa: StateFlow<Corrida?> = _ativa.asStateFlow()

    /** true depois da primeira consulta bem-sucedida ao servidor. */
    private val _carregou = MutableStateFlow(false)
    val carregou: StateFlow<Boolean> = _carregou.asStateFlow()

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
    }

    private val prefs get() = app.getSharedPreferences("pwex_corridas", Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ sincronização

    /** Busca oferta pendente e corrida ativa no servidor. */
    fun sincronizar() {
        escopo.launch { sincronizarAgora() }
    }

    suspend fun sincronizarAgora(): Unit = mutex.withLock {
        if (PwCore.auth.sessao.value == null) return@withLock
        runCatching {
            val a = PwCore.rpc.chamar("minha_corrida_ativa", JsonObject(emptyMap()))
            if (a.ok) {
                _ativa.value = a.json()?.paraCorrida()
                _carregou.value = true
            }
            val o = PwCore.rpc.chamar("oferta_pendente", JsonObject(emptyMap()))
            if (o.ok) _oferta.value = o.json()?.paraOferta()
            enviarTokenPendente()
        }
        Unit
    }

    fun limpar() {
        _oferta.value = null
        _ativa.value = null
        _carregou.value = false
    }

    // ------------------------------------------------------------------ oferta

    suspend fun responder(aceitar: Boolean): Result<Unit> {
        val oferta = _oferta.value ?: return Result.failure(Exception("Oferta não encontrada."))
        return runCatching {
            val r = PwCore.rpc.chamar("responder_oferta", buildJsonObject {
                put("p_oferta", oferta.id)
                put("p_aceitar", aceitar)
            })
            _oferta.value = null
            if (!r.ok) throw Exception(traduzir(r))
            if (aceitar) _ativa.value = r.json()?.paraCorrida()
            Unit
        }.onFailure { if (it is java.io.IOException) _oferta.value = oferta }
            .recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet. Tente de novo.") else e }
    }

    /** O prazo da oferta acabou no celular. */
    fun descartarOferta(id: String) {
        if (_oferta.value?.id == id) _oferta.value = null
    }

    // ------------------------------------------------------------------ etapas

    suspend fun avancar(paradaId: String, acao: String, dados: JsonObject = JsonObject(emptyMap())): Result<Unit> =
        runCatching {
            val r = PwCore.rpc.chamar("avancar_parada", buildJsonObject {
                put("p_parada", paradaId)
                put("p_acao", acao)
                put("p_dados", dados)
            })
            if (!r.ok) throw Exception(traduzir(r))
            _ativa.value = r.json()?.paraCorrida() // null = corrida concluída
            Unit
        }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet. Tente de novo.") else e }

    /** Envia foto/assinatura e devolve o caminho no Storage. */
    suspend fun enviarComprovante(corridaId: String, paradaId: String, nome: String, bytes: ByteArray, tipo: String): Result<String> =
        runCatching {
            val caminho = "$corridaId/$paradaId-$nome-${System.currentTimeMillis()}.${if (tipo == "image/png") "png" else "jpg"}"
            val r = PwCore.rpc.enviarArquivo("comprovantes", caminho, bytes, tipo)
            if (!r.ok) throw Exception("Falha ao enviar $nome (${r.codigo}).")
            caminho
        }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet para enviar o comprovante.") else e }

    // ------------------------------------------------------------------ token do Firebase

    fun atualizarTokenPush() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { registrarToken(it) }
        }
    }

    fun registrarToken(token: String) {
        prefs.edit { putString("token_pendente", token) }
        escopo.launch { enviarTokenPendente() }
    }

    private suspend fun enviarTokenPendente() {
        val token = prefs.getString("token_pendente", null) ?: return
        if (PwCore.auth.sessao.value == null) return
        runCatching {
            val r = PwCore.rpc.chamar("registrar_fcm_token", buildJsonObject { put("p_token", token) })
            if (r.ok) prefs.edit { remove("token_pendente") }
        }
    }

    /** Ao entrar com outro usuário, o token precisa ir para o novo motoboy. */
    fun aoEntrar() {
        atualizarTokenPush()
        sincronizar()
    }

    // ------------------------------------------------------------------ util

    private fun HttpResposta.json() =
        corpo.takeIf { it.isNotBlank() && it != "null" }?.let { Json.parseToJsonElement(it) }?.takeIf { it !is JsonNull }

    private val mensagens = mapOf(
        "oferta_expirou" to "O tempo da oferta acabou.",
        "oferta_nao_encontrada" to "Essa oferta não está mais disponível.",
        "voce_ja_tem_corrida_ativa" to "Você já tem uma corrida em andamento.",
        "conclua_a_parada_anterior" to "Conclua a parada anterior primeiro.",
        "corrida_nao_esta_ativa" to "Essa corrida não está mais ativa (pode ter sido cancelada).",
        "informe_o_nome_de_quem_recebeu" to "Informe o nome de quem recebeu.",
        "codigo_incorreto" to "Código de entrega incorreto.",
        "falta_a_foto" to "Tire a foto do comprovante.",
        "falta_a_assinatura" to "Colete a assinatura.",
    )

    private fun traduzir(r: HttpResposta): String {
        val msg = runCatching { Json.parseToJsonElement(r.corpo).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
        return mensagens[msg] ?: msg ?: "Erro no servidor (${r.codigo})."
    }
}
