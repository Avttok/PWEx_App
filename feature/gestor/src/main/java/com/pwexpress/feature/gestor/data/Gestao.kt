package com.pwexpress.feature.gestor.data

import com.pwexpress.core.PwCore
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.exigirOk
import com.pwexpress.core.json.int
import com.pwexpress.core.json.lista
import com.pwexpress.core.json.objeto
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import com.pwexpress.core.network.ErroServidor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.time.Instant

/**
 * Tudo o que o gestor faz no site, para o app: mesmas tabelas, mesmas funções do banco e as
 * mesmas rotas do servidor (criar conta, CNPJ…). As regras de segurança ficam no banco.
 */
object Gestao {

    data class Dados(
        val corridas: List<CorridaG> = emptyList(),
        val ofertas: List<OfertaG> = emptyList(),
        val equipe: List<MembroG> = emptyList(),
        val posicoes: List<PosicaoG> = emptyList(),
        val empresas: List<EmpresaG> = emptyList(),
        val favoritos: Map<String, List<String>> = emptyMap(),
        val config: Map<String, JsonElement> = emptyMap(),
        /** Trilhas das últimas 12 h por motoboy (como no mapa do site). */
        val trilhas: Map<String, List<PontoTrilha>> = emptyMap(),
        /** Logins de cliente por cadastro (empresa_id → contas). */
        val acessosCliente: Map<String, List<MembroG>> = emptyMap(),
        val carregado: Boolean = false,
    ) {
        fun membro(id: String?) = equipe.firstOrNull { it.id == id }
        fun empresa(id: String?) = empresas.firstOrNull { it.id == id }
        fun posicao(id: String?) = posicoes.firstOrNull { it.id == id }
        fun cfgNum(k: String, padrao: Double) = (config[k] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: padrao
        fun cfgBool(k: String, padrao: Boolean) = (config[k] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: padrao
        fun cliente(c: CorridaG): String = c.empresaId?.let { empresa(it)?.nome ?: "Cliente" } ?: c.clienteNome?.let { "$it (avulso)" } ?: "Avulso"
        fun nomeMotoboy(c: CorridaG): String? = c.motoboyId?.let { membro(it)?.nome } ?: c.motoboyNomeArquivado?.let { "$it (conta excluída)" }
    }

    private val _dados = MutableStateFlow(Dados())
    val dados: StateFlow<Dados> = _dados.asStateFlow()
    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()
    private val mutex = Mutex()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private var voltas = 0

    /** Recarrega tudo (o app chama a cada poucos segundos enquanto o painel está aberto). */
    suspend fun atualizar() = mutex.withLock {
        runCatching {
            val desde = Instant.now().minusSeconds(30L * 86400).toString()
            coroutineScope {
                val rpc = PwCore.rpc
                val corridas = async { rpc.rest("GET", "/rest/v1/corridas?select=*,paradas(*)&or=(criada_em.gte.${enc(desde)},agendada_para.gte.${enc(desde)})&order=criada_em.desc&limit=400").exigirOk().lista() }
                val ofertas = async { rpc.rest("GET", "/rest/v1/ofertas?select=id,corrida_id,motoboy_id,status,distancia_m,expira_em&status=eq.pendente").exigirOk().lista() }
                val equipe = async { rpc.chamar("listar_equipe", JsonObject(emptyMap())).exigirOk().lista() }
                val pos = async { rpc.rest("GET", "/rest/v1/motoboys?select=id,status,lat,lng,posicao_em,modo_teste,modo_economia,bateria,precisao_m").exigirOk().lista() }
                // trilhas pesam mais: a cada 5 voltas (~30 s)
                val trilha = if (voltas++ % 5 == 0 || !_dados.value.carregado) async {
                    val desde12h = Instant.now().minusSeconds(12L * 3600).toString()
                    runCatching {
                        rpc.rest("GET", "/rest/v1/localizacoes?select=motoboy_id,sessao_id,status,lat,lng,registrado_em,corrida_id&registrado_em=gte.${enc(desde12h)}&order=registrado_em.asc&limit=8000").exigirOk().lista()
                            .mapNotNull { o -> val la = o.dbl("lat"); val lo = o.dbl("lng"); if (la == null || lo == null) null else o.str("motoboy_id")!! to PontoTrilha(o.str("sessao_id"), o.str("status") ?: "", la, lo, iso(o.str("registrado_em")) ?: 0, o.str("corrida_id")) }
                            .groupBy({ it.first }, { it.second })
                    }.getOrNull()
                } else null
                val empresas = async { rpc.rest("GET", "/rest/v1/empresas?select=*&order=nome").exigirOk().lista() }
                val fav = async { rpc.rest("GET", "/rest/v1/empresa_favoritos?select=empresa_id,motoboy_id,ordem&order=ordem").exigirOk().lista() }
                val cfg = async { rpc.rest("GET", "/rest/v1/configuracoes?select=chave,valor").exigirOk().lista() }
                val vinculos = async { runCatching { rpc.rest("GET", "/rest/v1/perfis?select=id,empresa_id&empresa_id=not.is.null").exigirOk().lista().associate { it.str("id")!! to it.str("empresa_id")!! } }.getOrDefault(emptyMap()) }
                val todos = equipe.await().map { it.paraMembroG() }
                val vinc = vinculos.await()
                _dados.value = Dados(
                    corridas = corridas.await().map { it.paraCorridaG() },
                    ofertas = ofertas.await().map { it.paraOfertaG() },
                    equipe = todos.filterNot { it.soCliente },
                    acessosCliente = todos.filter { it.soCliente }.groupBy { vinc[it.id] ?: "" },
                    posicoes = pos.await().map { it.paraPosicaoG() },
                    empresas = empresas.await().map { it.paraEmpresaG() },
                    favoritos = fav.await().groupBy({ it.str("empresa_id")!! }, { it.str("motoboy_id")!! }),
                    config = cfg.await().associate { it.str("chave")!! to (it["valor"] ?: JsonNull) },
                    trilhas = trilha?.await() ?: _dados.value.trilhas,
                    carregado = true,
                )
            }
            _erro.value = null
        }.onFailure { _erro.value = mensagem(it) }
        Unit
    }

    fun limpar() { _dados.value = Dados() }

    // ------------------------------------------------------------------ corridas

    suspend fun criarCorrida(dados: JsonObject, datas: List<Long> = emptyList()): Result<String> = acao {
        val sb = PwCore.rpc
        val r = when {
            datas.isEmpty() -> sb.chamar("criar_corrida", buildJsonObject { put("dados", dados) })
            datas.size == 1 -> sb.chamar("criar_corrida", buildJsonObject {
                put("dados", JsonObject(dados + ("agendada_para" to JsonPrimitive(Instant.ofEpochMilli(datas[0]).toString()))))
            })
            else -> sb.chamar("criar_serie", buildJsonObject {
                put("dados", dados)
                put("p_datas", buildJsonArray { datas.forEach { add(JsonPrimitive(Instant.ofEpochMilli(it).toString())) } })
            })
        }.exigirOk(::traduzir)
        val corpo = r.corpo.trim()
        (if (corpo.startsWith("[")) corpo.removePrefix("[").substringBefore(",").substringBefore("]") else corpo).trim('"')
    }

    suspend fun cancelar(id: String) = acao { PwCore.rpc.chamar("cancelar_corrida", buildJsonObject { put("p_corrida", id); put("p_motivo", "Cancelada pelo gestor") }).exigirOk(::traduzir); Unit }
    /** Pagamento recebido por fora (PIX/cartão conferido pelo gestor): libera a corrida para o despacho. */
    suspend fun confirmarPagamento(id: String, metodo: String) = acao {
        PwCore.rpc.chamar("confirmar_pagamento", buildJsonObject { put("p_corrida", id); put("p_metodo", metodo) }).exigirOk(::traduzir); Unit
    }

    /** Chaves PIX cadastradas (usuário → "chave · titular"). */
    suspend fun chavesPix(): Map<String, String> = runCatching {
        PwCore.rpc.rest("GET", "/rest/v1/dados_recebimento?select=usuario_id,chave,titular").exigirOk().lista()
            .associate { it.str("usuario_id")!! to "${it.str("chave")} · ${it.str("titular")}" }
    }.getOrDefault(emptyMap())

    suspend fun redespachar(id: String, motoboy: String?) = acao {
        PwCore.rpc.chamar("redespachar", buildJsonObject { put("p_corrida", id); put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull) }).exigirOk(::traduzir); Unit
    }

    suspend fun gerenciarPlanejada(id: String, acao: String, motoboy: String? = null, quando: Long? = null) = acao {
        PwCore.rpc.chamar("gerenciar_planejada", buildJsonObject {
            put("p_corrida", id); put("p_acao", acao)
            put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_quando", quando?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    /** Apaga de vez (senha conferida no banco). Devolve o número de tentativas restantes em caso de senha errada. */
    suspend fun excluirCorridas(ids: List<String>, senha: String, motivo: String?): Result<Unit> = acao {
        val r = PwCore.rpc.chamar("excluir_corridas", buildJsonObject {
            put("p_corridas", buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } }); put("p_senha", senha); put("p_motivo", motivo ?: "")
        }).exigirOk(::traduzir).objeto()
        r?.str("erro")?.let { e ->
            val rest = r.int("restantes")
            throw ErroServidor(e, if (e == "senha_incorreta" && rest != null) "Senha incorreta. Restam $rest tentativa${if (rest == 1) "" else "s"}." else traduzir(e) ?: e)
        }
        val arquivos = r?.strs("arquivos").orEmpty()
        if (arquivos.isNotEmpty()) runCatching { PwCore.rpc.removerArquivos("comprovantes", arquivos) }
        Unit
    }

    suspend fun eventos(corridaId: String): List<EventoG> = runCatching {
        PwCore.rpc.rest("GET", "/rest/v1/eventos_corrida?select=tipo,dados,criado_em&corrida_id=eq.$corridaId&order=criado_em").exigirOk().lista()
            .map { EventoG(it.str("tipo").orEmpty(), (it["dados"] as? JsonObject) ?: JsonObject(emptyMap()), iso(it.str("criado_em")) ?: 0) }
    }.getOrDefault(emptyList())

    suspend fun linkComprovante(caminho: String): String? = runCatching { PwCore.rpc.linkAssinado("comprovantes", caminho) }.getOrNull()

    suspend fun clientesAvulsos(): List<String> = runCatching {
        PwCore.rpc.chamar("clientes_avulsos", JsonObject(emptyMap())).exigirOk().lista().mapNotNull { it.str("nome") }
    }.getOrDefault(emptyList())

    suspend fun concluidas(desdeMs: Long, teste: Boolean): List<CorridaG> = runCatching {
        PwCore.rpc.rest("GET", "/rest/v1/corridas?select=*&status=eq.concluida&teste=eq.$teste&concluida_em=gte.${enc(Instant.ofEpochMilli(desdeMs).toString())}&order=concluida_em.desc")
            .exigirOk().lista().map { it.paraCorridaG() }
    }.getOrDefault(emptyList())

    // ------------------------------------------------------------------ empresas

    suspend fun salvarEmpresa(id: String?, linha: JsonObject, favoritos: List<String>): Result<String> = acao {
        val r = if (id == null) PwCore.rpc.rest("POST", "/rest/v1/empresas", linha.toString(), "return=representation")
                else PwCore.rpc.rest("PATCH", "/rest/v1/empresas?id=eq.$id", linha.toString(), "return=representation")
        r.exigirOk { if (it.contains("duplicate") || it == "23505") "Já existe empresa com esse CNPJ." else null }
        val novo = r.lista().firstOrNull()?.str("id") ?: id ?: error("sem id")
        PwCore.rpc.rest("DELETE", "/rest/v1/empresa_favoritos?empresa_id=eq.$novo").exigirOk()
        if (favoritos.isNotEmpty()) {
            val corpo = JsonArray(favoritos.mapIndexed { i, m -> buildJsonObject { put("empresa_id", novo); put("motoboy_id", m); put("ordem", i) } })
            PwCore.rpc.rest("POST", "/rest/v1/empresa_favoritos", corpo.toString()).exigirOk()
        }
        novo
    }

    suspend fun excluirEmpresa(id: String) = acao { PwCore.rpc.rest("DELETE", "/rest/v1/empresas?id=eq.$id").exigirOk(); Unit }

    data class DadosCnpj(val razaoSocial: String?, val nomeFantasia: String?, val situacao: String?, val telefone: String?, val email: String?, val endereco: String?)

    suspend fun consultarCnpj(cnpj: String): Result<DadosCnpj> = acao {
        val o = PwCore.rpc.site("/api/cnpj/$cnpj", metodo = "GET").exigirOk(::traduzir).objeto() ?: error("vazio")
        val e = o["endereco"] as? JsonObject
        val endereco = e?.let {
            listOfNotNull(listOfNotNull(it.str("logradouro"), it.str("numero")).joinToString(", ").ifBlank { null }, it.str("bairro"),
                listOfNotNull(it.str("cidade"), it.str("uf")).joinToString(" - ").ifBlank { null }).joinToString(", ")
        }
        DadosCnpj(o.str("razaoSocial"), o.str("nomeFantasia"), o.str("situacao"), o.strs("telefones").firstOrNull(), o.strs("emails").firstOrNull(), endereco)
    }

    // ------------------------------------------------------------------ equipe

    suspend fun registro(id: String): Pair<JsonObject, List<ItemRegistro>> = acao {
        val o = PwCore.rpc.chamar("registro_membro", buildJsonObject { put("p_usuario", id); put("p_limite", 500) }).exigirOk(::traduzir).objeto() ?: error("vazio")
        ((o["resumo"] as? JsonObject) ?: JsonObject(emptyMap())) to o.arr("itens").map { it.paraItemRegistro() }
    }.getOrThrow()

    suspend fun editarMembro(id: String, nome: String, papeis: List<String>, repasse: Double?) = acao {
        PwCore.rpc.chamar("editar_membro", buildJsonObject {
            put("p_usuario", id); put("p_nome", nome)
            put("p_papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
            put("p_repasse", repasse?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun criarConta(nome: String, email: String, senha: String, papeis: List<String>, repasse: Double?, empresaId: String? = null): Result<String> = acao {
        PwCore.rpc.site("/api/equipe/criar", buildJsonObject {
            put("nome", nome); put("email", email); put("senha", senha)
            empresaId?.let { put("empresa_id", it) }
            put("papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
            put("repasse", repasse?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir).objeto()?.str("id") ?: ""
    }

    suspend fun redefinirSenha(id: String, senha: String) = acao { PwCore.rpc.site("/api/equipe/senha", buildJsonObject { put("id", id); put("senha", senha) }).exigirOk(::traduzir); Unit }
    suspend fun bloquear(id: String, bloquear: Boolean) = acao { PwCore.rpc.site("/api/equipe/bloquear", buildJsonObject { put("id", id); put("bloquear", bloquear) }).exigirOk(::traduzir); Unit }

    suspend fun excluirConta(id: String, senha: String, motivo: String?): Result<Unit> = acao {
        val r = PwCore.rpc.site("/api/equipe/excluir", buildJsonObject { put("id", id); put("senha", senha); put("motivo", motivo ?: "") })
        if (!r.ok) {
            val o = r.objeto()
            val e = o?.str("erro") ?: "falha"
            val rest = o?.int("restantes")
            throw ErroServidor(e, if (e == "senha_incorreta" && rest != null) "Senha incorreta. Restam $rest tentativa${if (rest == 1) "" else "s"}." else traduzir(e) ?: e)
        }
        Unit
    }

    // ------------------------------------------------------------------ configurações e perfil

    suspend fun salvarConfig(chave: String, valor: JsonElement) = acao {
        PwCore.rpc.rest("PATCH", "/rest/v1/configuracoes?chave=eq.${enc(chave)}", buildJsonObject { put("valor", valor) }.toString()).exigirOk(::traduzir)
        _dados.value = _dados.value.copy(config = _dados.value.config + (chave to valor))
        Unit
    }

    // ------------------------------------------------------------------ Dev

    data class ModuloDev(val chave: String, val nome: String, val descricao: String?, val grupo: String, val ligado: Boolean, val papeis: List<String>)

    suspend fun modulos(): Result<List<ModuloDev>> = acao {
        PwCore.rpc.rest("GET", "/rest/v1/modulos?select=*&order=ordem").exigirOk(::traduzir).lista().map {
            ModuloDev(it.str("chave")!!, it.str("nome").orEmpty(), it.str("descricao"), it.str("grupo") ?: "geral", it.bool("ligado"), it.strs("papeis"))
        }
    }

    suspend fun definirModulo(chave: String, ligado: Boolean, papeis: List<String>) = acao {
        PwCore.rpc.chamar("definir_modulo", buildJsonObject {
            put("p_chave", chave); put("p_ligado", ligado)
            put("p_papeis", if (papeis.isEmpty()) JsonNull else buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
        }).exigirOk(::traduzir); Unit
    }

    /** O sistema "nu": contagens, aparelhos e o registro de exclusões. */
    suspend fun painelDev(): Result<JsonObject> = acao { PwCore.rpc.chamar("painel_dev", JsonObject(emptyMap())).exigirOk(::traduzir).objeto() ?: JsonObject(emptyMap()) }

    // ------------------------------------------------------------------ util

    private suspend fun <T> acao(bloco: suspend () -> T): Result<T> = runCatching { bloco() }.recoverCatching { throw Exception(mensagem(it)) }

    private fun mensagem(e: Throwable): String = when (e) {
        is java.io.IOException -> "Sem conexão com o servidor."
        is ErroServidor -> e.message ?: e.codigo
        else -> e.message ?: "Erro desconhecido."
    }

    private val erros = mapOf(
        "apenas_gestor" to "Apenas gestores podem fazer isso.",
        "corrida_precisa_de_2_paradas" to "A corrida precisa de pelo menos uma coleta e uma entrega.",
        "primeira_parada_deve_ser_coleta" to "A primeira parada precisa ser uma coleta.",
        "escolha_o_motoboy" to "Escolha o motoboy.",
        "corrida_nao_esta_sem_motoboy" to "Essa corrida não está aguardando novo despacho.",
        "motoboy_fora_do_modo_teste" to "Esse motoboy não está com o modo teste ligado.",
        "motoboy_em_modo_teste" to "Esse motoboy está em modo teste: só recebe corridas de teste.",
        "horario_muito_proximo" to "Esse horário está perto demais. Planeje com mais antecedência ou crie para agora.",
        "datas_demais" to "No máximo 60 corridas por repetição.",
        "corrida_nao_esta_planejada" to "Essa corrida não está mais planejada.",
        "senha_incorreta" to "Senha incorreta.",
        "muitas_tentativas" to "Muitas senhas erradas. Ficou bloqueado por 15 minutos.",
        "cancele_as_corridas_em_andamento" to "Há corrida aceita, em andamento ou reservada. Cancele antes de apagar.",
        "servidor_sem_chave" to "Falta configurar a chave secreta do Supabase na Vercel (SUPABASE_SECRET_KEY).",
        "email_invalido" to "E-mail inválido.",
        "nome_invalido" to "O nome precisa ter entre 2 e 60 letras.",
        "senha_curta" to "A senha precisa ter pelo menos 8 caracteres.",
        "sem_papel" to "Marque pelo menos um papel.",
        "percentual_invalido" to "O repasse precisa ficar entre 0 e 100%.",
        "email_existe" to "Já existe uma conta com esse e-mail.",
        "nao_exclua_a_si_mesmo" to "Você não pode excluir a sua própria conta.",
        "nao_bloqueie_a_si_mesmo" to "Você não pode bloquear a sua própria conta.",
        "nao_remova_seu_proprio_acesso" to "Você não pode tirar seu próprio acesso de gestor.",
        "conta_protegida" to "Contas Dev só o Dev altera.",
        "apenas_dev" to "Só o Dev pode fazer isso.",
        "modulo_desligado" to "Essa função está desligada pelo Dev.",
        "membro_em_corrida" to "Essa pessoa está com uma corrida em andamento. Espere terminar antes.",
        "usuario_nao_encontrado" to "Conta não encontrada.",
        "nao_encontrado" to "CNPJ não encontrado na Receita.",
    )

    fun traduzir(codigo: String): String? = erros[codigo]
}

/** Acesso rápido a valores de configuração com padrão. */
fun Map<String, JsonElement>.numero(k: String, padrao: Double) = (this[k] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: padrao
fun Map<String, JsonElement>.logico(k: String, padrao: Boolean) = (this[k] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: padrao

