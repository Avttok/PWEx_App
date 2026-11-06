package com.pwexpress.core.perfil

import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.network.HttpResposta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Quem está usando o app: nome, foto, papéis, modo teste e configurações enviadas pelo servidor. */
data class Perfil(
    val id: String,
    val nome: String,
    val fotoUrl: String?,
    val gestor: Boolean,
    val motoboy: Boolean,
    val modoTeste: Boolean,
    val configuracoes: Map<String, JsonElement>,
) {
    /** Conta gestor + motoboy: tem as ferramentas de administrador no app. */
    val admin: Boolean get() = gestor && motoboy

    fun numero(chave: String, padrao: Double): Double =
        (configuracoes[chave] as? JsonPrimitive)?.doubleOrNull ?: padrao

    fun logico(chave: String, padrao: Boolean): Boolean =
        (configuracoes[chave] as? JsonPrimitive)?.booleanOrNull ?: padrao
}

data class Ganhos(val hoje: Double, val semana: Double, val mes: Double, val corridasHoje: Int)

object Perfis {

    private val _perfil = MutableStateFlow<Perfil?>(null)
    val perfil: StateFlow<Perfil?> = _perfil.asStateFlow()

    private val _ganhos = MutableStateFlow<Ganhos?>(null)
    val ganhos: StateFlow<Ganhos?> = _ganhos.asStateFlow()

    /** Busca perfil e ganhos. Falha silenciosa (sem internet mantém o último). */
    suspend fun carregar() {
        if (PwCore.auth.sessao.value == null) return
        runCatching {
            val r = PwCore.rpc.chamar("meu_perfil", JsonObject(emptyMap()))
            if (r.ok) r.json()?.let { _perfil.value = it.paraPerfil() }
            val g = PwCore.rpc.chamar("meus_ganhos", JsonObject(emptyMap()))
            if (g.ok) g.json()?.jsonObject?.let { o ->
                _ganhos.value = Ganhos(
                    hoje = o.num("hoje"), semana = o.num("semana"), mes = o.num("mes"),
                    corridasHoje = o.num("corridas_hoje").toInt(),
                )
            }
        }
    }

    fun limpar() {
        _perfil.value = null
        _ganhos.value = null
    }

    /** Muda nome e/ou foto. A foto vai para o bucket público "avatares/<id>/". */
    suspend fun atualizar(nome: String?, foto: ByteArray?): Result<Unit> = runCatching {
        val atual = _perfil.value ?: error("Perfil ainda não carregado.")
        var url: String? = null
        if (foto != null) {
            val caminho = "${atual.id}/foto-${System.currentTimeMillis()}.jpg"
            val up = PwCore.rpc.enviarArquivo("avatares", caminho, foto, "image/jpeg")
            if (!up.ok) error("Não foi possível enviar a foto (${up.codigo}).")
            url = "${PwConfig.supabaseUrl}/storage/v1/object/public/avatares/$caminho"
        }
        val r = PwCore.rpc.chamar("atualizar_meu_perfil", buildJsonObject {
            nome?.let { put("p_nome", it) }
            url?.let { put("p_foto_url", it) }
        })
        if (!r.ok) error(if (r.corpo.contains("nome_invalido")) "O nome deve ter entre 2 e 60 letras." else "Erro ao salvar (${r.codigo}).")
        r.json()?.let { _perfil.value = it.paraPerfil() }
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Liga/desliga o modo teste (só contas gestor + motoboy). */
    suspend fun definirModoTeste(ligado: Boolean): Result<Unit> = runCatching {
        val r = PwCore.rpc.chamar("definir_modo_teste", buildJsonObject { put("p_ligado", ligado) })
        if (!r.ok) error(
            when {
                r.corpo.contains("conclua_a_corrida_antes") -> "Conclua a corrida (ou a oferta) antes de trocar o modo."
                r.corpo.contains("apenas_gestor") -> "Só gestores podem usar o modo teste."
                else -> "Erro (${r.codigo})."
            },
        )
        carregar()
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    // ------------------------------------------------------------------ util

    private fun HttpResposta.json(): JsonElement? =
        corpo.takeIf { it.isNotBlank() && it != "null" }?.let { Json.parseToJsonElement(it) }?.takeIf { it !is JsonNull }

    private fun JsonObject.num(k: String): Double = (this[k] as? JsonPrimitive)?.doubleOrNull ?: 0.0

    private fun JsonElement.paraPerfil(): Perfil {
        val o = jsonObject
        fun txt(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        fun bool(k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: false
        return Perfil(
            id = txt("id").orEmpty(),
            nome = txt("nome").orEmpty(),
            fotoUrl = txt("foto_url"),
            gestor = bool("gestor"),
            motoboy = bool("motoboy") || (o["papeis"]?.jsonArray?.any { it.jsonPrimitive.content == "motoboy" } ?: false),
            modoTeste = bool("modo_teste"),
            configuracoes = (o["configuracoes"] as? JsonObject)?.toMap() ?: emptyMap(),
        )
    }
}
