package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val CONTAGENS = listOf(
    "contas" to "Contas", "motoboys_online" to "Online", "corridas_ativas" to "Ativas", "corridas_reais" to "Corridas reais",
    "corridas_teste" to "De teste", "pings_24h" to "Pings 24 h", "pings_total" to "Pings no banco", "corridas_apagadas" to "Apagadas",
    "contas_excluidas" to "Contas excluídas",
)
private val PAPEIS = listOf(Triple("gestor", "Gestores", PwIcones.ShieldCheck), Triple("motoboy", "Motoboys", PwIcones.Bike), Triple("cliente", "Clientes", PwIcones.UserRound))

/** Console do Dev (igual ao do site): módulos liga/desliga e quem usa, sistema, aparelhos e registro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevG(onVoltar: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var aba by remember { mutableIntStateOf(0) }
    var modulos by remember { mutableStateOf<List<Gestao.ModuloDev>?>(null) }
    var painel by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }

    suspend fun carregar() {
        Gestao.modulos().onSuccess { modulos = it }.onFailure { erro = it.message }
        Gestao.painelDev().onSuccess { painel = it }.onFailure { erro = it.message }
    }
    LaunchedEffect(Unit) { carregar() }

    fun salvar(m: Gestao.ModuloDev, ligado: Boolean, papeis: List<String>) {
        modulos = modulos?.map { if (it.chave == m.chave) it.copy(ligado = ligado, papeis = papeis) else it }
        escopo.launch {
            Gestao.definirModulo(m.chave, ligado, papeis)
                .onSuccess { PwToast.mostrar("${m.nome}: ${if (ligado) "ligado" else "desligado"}"); Perfis.carregar() }
                .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO); carregar() }
        }
    }

    PwPagina {
        Cabecalho("Console Dev", onVoltar, subtitulo = "O sistema inteiro, sem filtro")
        PwFiltros(listOf("Módulos" to 0, "Sistema" to 0, "Aparelhos" to 0, "Registro" to 0, "Versão do app" to 0), aba, { aba = it })
        PwErro(erro)
        if (aba == 4) { VersaoAppG(); return@PwPagina }
        when (aba) {
            0 -> {
                val l = modulos ?: run { PwCarregando(); return@PwPagina }
                TextoDica("Desligado, some para todos (menos para você). “Quem usa” limita a cargos.")
                l.groupBy { it.grupo }.forEach { (g, ms) ->
                    PwRotulo(g)
                    ms.forEach { m ->
                        PwCartao {
                            PwInterruptor(m.nome, m.ligado, { salvar(m, it, m.papeis) }, descricao = m.descricao)
                            if (m.ligado) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PwPilula("Todos", PwIcones.Users, { salvar(m, true, emptyList()) }, ligada = m.papeis.isEmpty())
                                    PAPEIS.forEach { (p, rot, ic) ->
                                        val ativo = p in m.papeis
                                        PwPilula(rot, ic, { salvar(m, true, if (ativo) m.papeis - p else m.papeis + p) }, ligada = ativo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val cont = painel?.obj("contagens") ?: run { PwCarregando(); return@PwPagina }
                CONTAGENS.chunked(2).forEach { par ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        par.forEach { (k, r) -> PwNumero(r, "%,d".format(BR, cont.long(k) ?: 0L), Modifier.weight(1f)) }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                ZerarPedidosG(onFeito = { escopo.launch { carregar() } })
            }
            2 -> {
                val ap = painel?.arr("aparelhos") ?: run { PwCarregando(); return@PwPagina }
                if (ap.isEmpty()) PwVazio("Nenhum aparelho registrado ainda.", icone = PwIcones.Smartphone)
                ap.forEach { a ->
                    PwCartao {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.str("nome") ?: "—", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(a.str("versao_app") ?: "—", color = c.texto3, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Detalhe(PwIcones.Radio, rotuloMotoboy[a.str("status")] ?: "Offline")
                            Detalhe(PwIcones.Timer, haQuanto(com.pwexpress.feature.gestor.data.iso(a.str("posicao_em"))))
                            a.int("bateria")?.let { Detalhe(PwIcones.BatteryMedium, "$it%") }
                            a.dbl("precisao_m")?.let { Detalhe(PwIcones.Crosshair, "± ${it.toInt()} m") }
                        }
                        if (a.bool("modo_teste") || a.bool("modo_economia")) Row(Modifier.padding(top = 4.dp)) {
                            if (a.bool("modo_teste")) Detalhe(PwIcones.FlaskConical, "teste", c.violeta)
                            if (a.bool("modo_economia")) { Spacer(Modifier.width(12.dp)); Detalhe(PwIcones.BatteryLow, "economia") }
                        }
                    }
                }
            }
            else -> {
                val ex = painel?.arr("exclusoes") ?: run { PwCarregando(); return@PwPagina }
                if (ex.isEmpty()) PwVazio("Nada apagado até agora.", icone = PwIcones.History)
                ex.forEach { x ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${if (x.str("tipo") == "conta") "Conta" else "Corrida"} ${x.str("rotulo") ?: ""}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica(listOfNotNull(x.str("por")?.let { "por $it" }, com.pwexpress.feature.gestor.data.iso(x.str("quando"))?.let { dataHora(it) }, x.str("motivo")?.let { "“$it”" }).joinToString(" · "))
                    }
                }
            }
        }
    }
}


/**
 * Versão do app que deve rodar (igual ao site): uma build escolhida ou "sempre a mais nova".
 * Com "forçar" ligado, qualquer outra versão trava na tela de atualização e o servidor recusa as chamadas dela.
 */
@Composable
private fun VersaoAppG() {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    var builds by remember { mutableStateOf<List<Pair<Int, String>>?>(null) }
    var emUso by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var digitada by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    fun cfg(k: String) = (dados.config[k] as? kotlinx.serialization.json.JsonPrimitive)?.content
    val exigida = cfg("app_versao_exigida")?.toDoubleOrNull()?.toInt() ?: 0
    val forcar = cfg("app_forcar_versao") != "false"
    val devLivre = cfg("app_dev_livre") != "false"
    LaunchedEffect(Unit) {
        emUso = (Gestao.rpcJson("versoes_em_uso").getOrNull() as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        builds = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val req = okhttp3.Request.Builder().url("https://api.github.com/repos/${com.pwexpress.core.config.PwConfig.githubRepo}/releases?per_page=40")
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "PWExpress-app").build()
                com.pwexpress.core.PwCore.http.client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) error("GitHub ${r.code}")
                    (kotlinx.serialization.json.Json.parseToJsonElement(r.body!!.string()) as kotlinx.serialization.json.JsonArray).mapNotNull { e ->
                        val o = e as? JsonObject ?: return@mapNotNull null
                        val tag = (o["tag_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                        val b = tag.removePrefix("build-").toIntOrNull()?.takeIf { tag.startsWith("build-") } ?: return@mapNotNull null
                        b to ((o["published_at"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.take(10).orEmpty())
                    }.sortedByDescending { it.first }
                }
            }.onFailure { erro = "Lista do GitHub indisponível (${it.message}). Digite a build." }.getOrDefault(emptyList())
        }
    }
    fun salvar(k: String, v: kotlinx.serialization.json.JsonElement) = escopo.launch {
        Gestao.salvarConfig(k, v).onSuccess { Gestao.atualizar(); PwToast.mostrar("Salvo.", TipoAviso.OK) }.onFailure { erro = it.message }
    }
    PwCartao(destaque = true) {
        PwRotulo("Rodando agora")
        Text(if (exigida > 0) "Build $exigida" else "Sempre a mais nova", style = androidx.compose.ui.text.TextStyle(fontFamily = com.pwexpress.core.designsystem.ArchivoLargo,
            fontWeight = FontWeight.Black, fontSize = 26.sp), color = c.texto)
        TextoDica("Este celular: build ${com.pwexpress.core.update.Atualizador.versaoInstalada}")
    }
    com.pwexpress.core.designsystem.PwInterruptor("Forçar a versão", forcar, { salvar("app_forcar_versao", kotlinx.serialization.json.JsonPrimitive(it)) },
        descricao = if (forcar) "Outra versão trava na tela de atualização e o servidor recusa as chamadas dela." else "Só avisa que há versão nova.")
    com.pwexpress.core.designsystem.PwInterruptor("Dev pode usar qualquer versão", devLivre, { salvar("app_dev_livre", kotlinx.serialization.json.JsonPrimitive(it)) },
        descricao = "Para testar uma build antes de liberar para todos.")
    com.pwexpress.core.designsystem.PwBotaoVidro("Sempre a mais nova", onClick = { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(0)) },
        icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
    PwErro(erro)
    PwRotulo("Escolher uma build")
    when (val l = builds) {
        null -> PwCarregando()
        else -> l.forEach { (b, data) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (b == exigida) c.ouro.copy(alpha = 0.14f) else c.vidroSutil)
                .border(1.dp, if (b == exigida) c.ouro else c.borda, RoundedCornerShape(14.dp)).clickable { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(b)) }
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Build $b", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(data, color = c.texto3, fontSize = 12.sp)
                if (b == exigida) { Spacer(Modifier.width(8.dp)); Icon(PwIcones.Check, null, tint = c.ouro, modifier = Modifier.size(18.dp)) }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.pwexpress.core.designsystem.PwCampo(digitada, { digitada = it.filter(Char::isDigit) }, "Build", teclado = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
        com.pwexpress.core.designsystem.PwBotaoVidro("Usar", onClick = { digitada.toIntOrNull()?.let { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(it)) } })
    }
    if (emUso.isNotEmpty()) {
        PwRotulo("Quem está em qual versão")
        emUso.forEach { m ->
            val v = (m["versao"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text((m["nome"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(), color = c.texto, modifier = Modifier.weight(1f))
                Text(v?.let { "build $it" } ?: "antiga", color = if (exigida > 0 && v != exigida) c.vermelho else c.texto2, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


/** Zera os pedidos (teste, reais ou ambos) e a numeração volta ao 1. */
@Composable
private fun ZerarPedidosG(onFeito: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var alvo by remember { mutableIntStateOf(0) }
    var confirmar by remember { mutableStateOf("") }
    var rodando by remember { mutableStateOf(false) }
    val alvos = listOf("teste" to "Só teste", "reais" to "Só reais", "ambos" to "Ambos")
    PwCartao(cor = c.vermelho.copy(alpha = 0.5f)) {
        com.pwexpress.core.designsystem.PwRotulo("Zerar pedidos")
        TextoDica("Apaga os pedidos escolhidos, as rotas deles e as faturas que ficarem vazias. A numeração volta ao 1. As rotas apagadas ficam copiadas no registro de exclusões. Não dá para desfazer.")
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwSegmentado(alvos.map { it.second to null }, alvo, { alvo = it })
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwCampo(confirmar, { confirmar = it.uppercase() }, "Digite ZERAR para confirmar")
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwBotaoPerigo(if (rodando) "Zerando…" else "Zerar ${alvos[alvo].second.lowercase()}", onClick = {
            if (confirmar != "ZERAR" || rodando) return@PwBotaoPerigo
            rodando = true
            escopo.launch {
                Gestao.rpcJson("resetar_pedidos", kotlinx.serialization.json.buildJsonObject {
                    put("p_alvo", kotlinx.serialization.json.JsonPrimitive(alvos[alvo].first)); put("p_confirmar", kotlinx.serialization.json.JsonPrimitive("ZERAR"))
                }).onSuccess { j ->
                    val o = j as? JsonObject
                    PwToast.mostrar("${o?.long("pedidos") ?: 0} pedidos, ${o?.long("rotas") ?: 0} rotas e ${o?.long("faturas") ?: 0} faturas apagados. O próximo pedido é o nº 1.", TipoAviso.OK, "Pedidos zerados")
                    confirmar = ""; Gestao.atualizar(); onFeito()
                }.onFailure { PwToast.mostrar(if (it.message.orEmpty().contains("resetar_pedidos")) "Rode a migração 20261110000005_reset_pedidos.sql." else it.message ?: "Erro", TipoAviso.ERRO) }
                rodando = false
            }
        }, cheio = true, modifier = Modifier.fillMaxWidth())
    }
}
