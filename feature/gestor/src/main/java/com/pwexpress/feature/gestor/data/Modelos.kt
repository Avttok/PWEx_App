package com.pwexpress.feature.gestor.data

import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import kotlinx.serialization.json.JsonObject

/** Data/hora ISO do servidor → milissegundos. */
fun iso(s: String?): Long? = s?.let {
    runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }
        .recoverCatching { _ -> java.time.OffsetDateTime.parse(it.replace(" ", "T")).toInstant().toEpochMilli() }
        .getOrNull()
}

object Status {
    const val AGENDADA = "agendada"
    const val RESERVADA = "reservada"
    const val AGUARDANDO = "aguardando_motoboy"
    const val SEM_MOTOBOY = "sem_motoboy"
    const val ACEITA = "aceita"
    const val EM_ANDAMENTO = "em_andamento"
    const val CONCLUIDA = "concluida"
    const val CANCELADA = "cancelada"
    val ATIVAS = setOf(AGUARDANDO, SEM_MOTOBOY, ACEITA, EM_ANDAMENTO)
    val PLANEJADAS = setOf(AGENDADA, RESERVADA)
    val rotulo = mapOf(
        AGENDADA to "Planejada · livre", RESERVADA to "Planejada · reservada", AGUARDANDO to "Procurando motoboy",
        SEM_MOTOBOY to "Sem motoboy", ACEITA to "Aceita", EM_ANDAMENTO to "Em andamento", CONCLUIDA to "Concluída", CANCELADA to "Cancelada",
    )
}

data class RefG(val nome: String, val distanciaM: Int?, val tipo: String?, val lat: Double? = null, val lng: Double? = null)

data class ParadaG(
    val id: String, val ordem: Int, val coleta: Boolean, val endereco: String, val lat: Double, val lng: Double,
    val contatoNome: String?, val contatoTelefone: String?, val instrucoes: String?, val status: String,
    val concluidaEm: Long?, val recebedorNome: String?, val fotoPath: String?, val assinaturaPath: String?,
    val distanciaChegadaM: Int?, val localIgnorado: Boolean, val referencias: List<RefG>,
)

data class CorridaG(
    val id: String, val numero: Long, val teste: Boolean, val status: String,
    val urgente: Boolean = false,
    val empresaId: String?, val clienteNome: String?, val criadaPor: String?,
    val motoboyId: String?, val motoboyNomeArquivado: String?,
    val valor: Double?, val valorBase: Double?, val fatorEntrega: Double, val fatorColeta: Double,
    val valorMotoboy: Double?, val valorGestao: Double?, val repassePct: Double?,
    val distanciaPlanejadaM: Int?, val duracaoS: Int?, val distanciaRealM: Int?, val rota: String?,
    val observacoes: String?, val exigirNoLocal: Boolean, val raioChegadaM: Int,
    val exigeNome: Boolean, val exigeFoto: Boolean, val exigeAssinatura: Boolean, val exigeCodigo: Boolean, val codigoEntrega: String?,
    val criadaEm: Long, val concluidaEm: Long?, val agendadaPara: Long?, val reservadaEm: Long?, val serieId: String?, val atrasada: Boolean,
    val paradas: List<ParadaG>,
) {
    val numeroExibicao get() = if (teste) "T$numero" else "#$numero"
    val ativa get() = status in Status.ATIVAS
    val planejada get() = status in Status.PLANEJADAS
}

data class OfertaG(val id: String, val corridaId: String, val motoboyId: String, val distanciaM: Int?, val expiraEm: Long)

data class MembroG(
    val id: String, val email: String, val nome: String, val foto: String?, val papeis: List<String>,
    val status: String?, val modoTeste: Boolean, val repassePct: Double?, val bloqueado: Boolean, val ultimoAcesso: Long?,
) {
    val gestor get() = "gestor" in papeis
    val motoboy get() = "motoboy" in papeis
    val dev get() = "dev" in papeis
}

data class PosicaoG(val id: String, val status: String, val lat: Double?, val lng: Double?, val posicaoEm: Long?, val modoTeste: Boolean, val bateria: Int?, val precisaoM: Double?, val economia: Boolean = false)

/** Ponto da trilha (últimas 12 h). */
data class PontoTrilha(val sessao: String?, val status: String, val lat: Double, val lng: Double)

data class EmpresaG(
    val id: String, val nome: String, val cnpj: String?, val razaoSocial: String?, val nomeFantasia: String?, val situacao: String?,
    val telefone: String?, val email: String?, val endereco: String?, val lat: Double?, val lng: Double?, val instrucoesColeta: String?,
    val modalidade: String, val valorKm: Double?, val valorFixo: Double?, val clienteVeAoVivo: Boolean, val observacoes: String?,
)

data class EventoG(val tipo: String, val dados: JsonObject, val criadoEm: Long)

data class ItemRegistro(
    val quando: Long, val tipo: String, val dados: JsonObject, val fez: Boolean, val numero: Long?, val teste: Boolean,
    val ganho: Double?, val valor: Double?, val cliente: String?,
)

// ------------------------------------------------------------------ JSON → modelos

fun JsonObject.paraParadaG() = ParadaG(
    id = str("id")!!, ordem = int("ordem") ?: 0, coleta = str("tipo") != "entrega", endereco = str("endereco").orEmpty(),
    lat = dbl("lat") ?: 0.0, lng = dbl("lng") ?: 0.0, contatoNome = str("contato_nome"), contatoTelefone = str("contato_telefone"),
    instrucoes = str("instrucoes"), status = str("status") ?: "pendente", concluidaEm = iso(str("concluida_em")),
    recebedorNome = str("recebedor_nome"), fotoPath = str("foto_path"), assinaturaPath = str("assinatura_path"),
    distanciaChegadaM = int("distancia_chegada_m"), localIgnorado = bool("local_ignorado"),
    referencias = arr("referencias").mapNotNull { r -> r.str("nome")?.let { RefG(it, r.int("distancia_m"), r.str("tipo"), r.dbl("lat"), r.dbl("lng")) } },
)

fun JsonObject.paraCorridaG() = CorridaG(
    id = str("id")!!, numero = long("numero") ?: 0, teste = bool("teste"), status = str("status").orEmpty(),
    urgente = bool("urgente"),
    empresaId = str("empresa_id"), clienteNome = str("cliente_nome"), criadaPor = str("criada_por"),
    motoboyId = str("motoboy_id"), motoboyNomeArquivado = str("motoboy_nome_arquivado"),
    valor = dbl("valor"), valorBase = dbl("valor_base"), fatorEntrega = dbl("fator_entrega") ?: 0.0, fatorColeta = dbl("fator_coleta") ?: 0.0,
    valorMotoboy = dbl("valor_motoboy"), valorGestao = dbl("valor_gestao"), repassePct = dbl("repasse_pct"),
    distanciaPlanejadaM = int("distancia_planejada_m"), duracaoS = int("duracao_planejada_s"), distanciaRealM = int("distancia_real_m"),
    rota = str("rota_planejada"), observacoes = str("observacoes"), exigirNoLocal = bool("exigir_no_local"), raioChegadaM = int("raio_chegada_m") ?: 150,
    exigeNome = bool("exige_nome"), exigeFoto = bool("exige_foto"), exigeAssinatura = bool("exige_assinatura"), exigeCodigo = bool("exige_codigo"),
    codigoEntrega = str("codigo_entrega"), criadaEm = iso(str("criada_em")) ?: 0, concluidaEm = iso(str("concluida_em")),
    agendadaPara = iso(str("agendada_para")), reservadaEm = iso(str("reservada_em")), serieId = str("serie_id"),
    atrasada = str("atraso_avisado_em") != null,
    paradas = arr("paradas").map { it.paraParadaG() }.sortedBy { it.ordem },
)

fun JsonObject.paraOfertaG() = OfertaG(str("id")!!, str("corrida_id")!!, str("motoboy_id")!!, int("distancia_m"), iso(str("expira_em")) ?: 0)

fun JsonObject.paraMembroG() = MembroG(
    id = str("id")!!, email = str("email").orEmpty(), nome = str("nome").orEmpty().ifBlank { str("email").orEmpty().substringBefore("@") },
    foto = str("foto_url"), papeis = strs("papeis"), status = str("status"), modoTeste = bool("modo_teste"),
    repassePct = dbl("repasse_pct"), bloqueado = bool("bloqueado"), ultimoAcesso = iso(str("ultimo_acesso")),
)

fun JsonObject.paraPosicaoG() = PosicaoG(str("id")!!, str("status") ?: "offline", dbl("lat"), dbl("lng"), iso(str("posicao_em")), bool("modo_teste"), int("bateria"), dbl("precisao_m"), bool("modo_economia"))

fun JsonObject.paraEmpresaG() = EmpresaG(
    id = str("id")!!, nome = str("nome").orEmpty(), cnpj = str("cnpj"), razaoSocial = str("razao_social"), nomeFantasia = str("nome_fantasia"),
    situacao = str("situacao"), telefone = str("telefone"), email = str("email"), endereco = str("endereco"), lat = dbl("lat"), lng = dbl("lng"),
    instrucoesColeta = str("instrucoes_coleta"), modalidade = str("modalidade_preco") ?: "por_km", valorKm = dbl("valor_km"), valorFixo = dbl("valor_fixo"),
    clienteVeAoVivo = bool("cliente_ve_ao_vivo"), observacoes = str("observacoes"),
)

fun JsonObject.paraItemRegistro() = ItemRegistro(
    quando = iso(str("quando")) ?: 0, tipo = str("tipo").orEmpty(), dados = obj("dados") ?: JsonObject(emptyMap()), fez = bool("fez"),
    numero = long("numero"), teste = bool("teste"), ganho = dbl("ganho"), valor = dbl("valor"), cliente = str("cliente"),
)
