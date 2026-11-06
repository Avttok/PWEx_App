package com.pwexpress.feature.corridas.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class TipoParada { COLETA, ENTREGA }
enum class StatusParada { PENDENTE, A_CAMINHO, CHEGOU, CONCLUIDA }

data class Parada(
    val id: String,
    val ordem: Int,
    val tipo: TipoParada,
    val endereco: String,
    val lat: Double,
    val lng: Double,
    val contatoNome: String?,
    val contatoTelefone: String?,
    val instrucoes: String?,
    val status: StatusParada,
    /** Até 3 pontos de referência perto do endereço (definidos pelo gestor). */
    val referencias: List<Referencia> = emptyList(),
)

data class Referencia(val nome: String, val distanciaM: Int?, val tipo: String?)

data class Corrida(
    val id: String,
    val numero: Long,
    val status: String,
    val teste: Boolean,
    /** O que o motoboy recebe (nunca o total nem o percentual). */
    val valorMotoboy: Double?,
    val exigirNoLocal: Boolean,
    val raioChegadaM: Int,
    val rotaPlanejada: String?,
    val distanciaM: Int?,
    val duracaoS: Int?,
    val observacoes: String?,
    val exigeFoto: Boolean,
    val exigeAssinatura: Boolean,
    val exigeNome: Boolean,
    val exigeCodigo: Boolean,
    val empresaNome: String?,
    val paradas: List<Parada>,
    // corridas planejadas (hora marcada)
    val agendadaPara: Long? = null,
    val cancelarReservaAte: Long? = null,
    val iniciarAPartirDe: Long? = null,
    val minha: Boolean = false,
) {
    /** Reais "#12", de teste "T12" (sequências separadas). */
    val numeroExibicao: String get() = if (teste) "T$numero" else "#$numero"
    val planejada: Boolean get() = status == "agendada" || status == "reservada"

    /** Parada em que o motoboy está agora (primeira não concluída). */
    val paradaAtual: Parada? get() = paradas.firstOrNull { it.status != StatusParada.CONCLUIDA }
}

data class Oferta(
    val id: String,
    val prazoMillis: Long, // relógio do celular (evita diferença de horário com o servidor)
    val distanciaM: Int?,
    val corrida: Corrida,
)

// ------------------------------------------------------------------ JSON → modelos

private fun JsonObject.texto(k: String): String? =
    this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
private fun JsonObject.inteiro(k: String): Int? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
private fun JsonObject.decimal(k: String): Double? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.doubleOrNull
private fun JsonObject.logico(k: String): Boolean = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.booleanOrNull ?: false

internal fun JsonElement.paraCorrida(): Corrida {
    val o = jsonObject
    return Corrida(
        id = o.texto("id")!!,
        numero = o.texto("numero")?.toLongOrNull() ?: 0,
        status = o.texto("status").orEmpty(),
        teste = o.logico("teste"),
        valorMotoboy = o.decimal("valor_motoboy"),
        exigirNoLocal = o.logico("exigir_no_local"),
        raioChegadaM = o.inteiro("raio_chegada_m") ?: 150,
        rotaPlanejada = o.texto("rota_planejada"),
        distanciaM = o.inteiro("distancia_planejada_m"),
        duracaoS = o.inteiro("duracao_planejada_s"),
        observacoes = o.texto("observacoes"),
        exigeFoto = o.logico("exige_foto"),
        exigeAssinatura = o.logico("exige_assinatura"),
        exigeNome = o.logico("exige_nome"),
        exigeCodigo = o.logico("exige_codigo"),
        empresaNome = o.texto("empresa_nome"),
        paradas = o["paradas"]?.jsonArray?.map { it.paraParada() }.orEmpty().sortedBy { it.ordem },
        agendadaPara = o.texto("agendada_para")?.let { instante(it) },
        cancelarReservaAte = o.texto("cancelar_reserva_ate")?.let { instante(it) },
        iniciarAPartirDe = o.texto("iniciar_a_partir_de")?.let { instante(it) },
        minha = o.logico("minha"),
    )
}

private fun JsonElement.paraParada(): Parada {
    val o = jsonObject
    return Parada(
        id = o.texto("id")!!,
        ordem = o.inteiro("ordem") ?: 0,
        tipo = if (o.texto("tipo") == "entrega") TipoParada.ENTREGA else TipoParada.COLETA,
        endereco = o.texto("endereco").orEmpty(),
        lat = o.decimal("lat") ?: 0.0,
        lng = o.decimal("lng") ?: 0.0,
        contatoNome = o.texto("contato_nome"),
        contatoTelefone = o.texto("contato_telefone"),
        instrucoes = o.texto("instrucoes"),
        status = when (o.texto("status")) {
            "a_caminho" -> StatusParada.A_CAMINHO
            "chegou" -> StatusParada.CHEGOU
            "concluida" -> StatusParada.CONCLUIDA
            else -> StatusParada.PENDENTE
        },
        referencias = (o["referencias"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e ->
            val r = e as? JsonObject ?: return@mapNotNull null
            Referencia(r.texto("nome") ?: return@mapNotNull null, r.inteiro("distancia_m"), r.texto("tipo"))
        }.orEmpty(),
    )
}

internal fun JsonElement.paraOferta(): Oferta {
    val o = jsonObject
    val segundos = o.inteiro("segundos_restantes") ?: 0
    return Oferta(
        id = o.texto("oferta_id")!!,
        prazoMillis = System.currentTimeMillis() + segundos * 1000L,
        distanciaM = o.inteiro("distancia_m"),
        corrida = o.getValue("corrida").paraCorrida(),
    )
}

/** Data/hora do servidor (ISO 8601, com fuso) em milissegundos. */
internal fun instante(iso: String): Long? = runCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
    .recoverCatching { java.time.OffsetDateTime.parse(iso.replace(" ", "T")).toInstant().toEpochMilli() }
    .getOrNull()
