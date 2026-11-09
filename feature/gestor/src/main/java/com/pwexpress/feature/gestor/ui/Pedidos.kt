package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.clickable
import com.pwexpress.core.json.str
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PedidoG
import com.pwexpress.feature.gestor.data.StatusPedido
import kotlinx.coroutines.launch

/**
 * Pedidos (do site do cliente e do gestor). Por padrão todo pedido novo passa por aqui:
 * o gestor aprova (vai para o roteirizador), ajusta o valor ou recusa.
 */
@Composable
fun PedidosG(onNovo: () -> Unit = {}) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    val todos = dados.pedidos.filter { it.teste == teste }
    val listas = listOf(
        todos.filter { it.status == "recebido" }.sortedWith(compareByDescending<PedidoG> { it.urgente }.thenBy { it.quando ?: it.criadoEm }),
        todos.filter { it.status in setOf("aprovado", "roteirizado", "em_rota") },
        todos.filter { it.status == "concluido" },
        todos.filter { it.status in setOf("cancelado", "recusado") },
    )
    val lista = listas[filtro]

    PwPagina {
        PwCabeca(PwIcones.ClipboardList, "Pedidos", if (listas[0].isEmpty()) "Nada esperando revisão" else "${listas[0].size} esperando você") {
            PwBotaoIcone(PwIcones.Sparkles, "Montar rotas agora", {
                if (!ocupado) { ocupado = true; escopo.launch { Gestao.roteirizarAgora().onSuccess { PwToast.mostrar("Rotas montadas com os pedidos aprovados.", TipoAviso.OK) }.onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; Gestao.atualizar(); ocupado = false } }
            }, tamanho = 38.dp)
            PwBotaoIcone(PwIcones.Plus, "Novo pedido", onNovo, ativo = true, tamanho = 38.dp)
        }
        PwFiltros(listOf("Revisar" to listas[0].size, "Em andamento" to listas[1].size, "Concluídos" to 0, "Cancelados" to 0), filtro, { filtro = it })
        when {
            !dados.carregado -> PwCarregando()
            lista.isEmpty() -> PwVazio(if (filtro == 0) "Nenhum pedido esperando revisão." else "Nenhum pedido aqui.", icone = PwIcones.ClipboardList)
            else -> lista.forEach { p -> CartaoPedido(p, dados.empresa(p.empresaId)?.nome) { aberto = p.id } }
        }
    }
    aberto?.let { id -> dados.pedidos.firstOrNull { it.id == id }?.let { JanelaPedido(it, dados.empresa(it.empresaId)?.nome) { aberto = null } } }
}

private fun nomeCliente(p: PedidoG, empresa: String?) = empresa ?: p.clienteNome?.let { "$it (avulso)" } ?: "Avulso"

@Composable
private fun CartaoPedido(p: PedidoG, empresa: String?, onClick: () -> Unit) {
    val c = Pw.cores
    PwCartao(destaque = p.status == "recebido", modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.numeroExibicao, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            // express e planejado com o mesmo peso
            PwSelo(if (p.planejado) "Planejado · ${p.quando?.let { dataHora(it) } ?: "—"}" else "Agora", if (p.planejado) c.azul else c.ouro, icone = if (p.planejado) PwIcones.CalendarClock else PwIcones.Zap)
            Spacer(Modifier.weight(1f))
            Text(moedaBr(p.valor), color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.size(4.dp))
        Text(nomeCliente(p, empresa), color = c.texto2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val entregas = p.pontos.count { !it.coleta }
        Text(
            listOfNotNull(
                "${p.pontos.count { it.coleta }} coleta${if (p.pontos.count { it.coleta } == 1) "" else "s"} · $entregas entrega${if (entregas == 1) "" else "s"}",
                km(p.distanciaM).takeIf { p.distanciaM != null },
                "${p.itens.sumOf { it.quantidade }} ite${if (p.itens.sumOf { it.quantidade } == 1) "m" else "ns"}".takeIf { p.itens.isNotEmpty() },
                StatusPedido.rotulo[p.status],
            ).joinToString(" · "),
            color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        val esperaPag = p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" && p.status !in setOf("cancelado", "recusado", "concluido")
        if (p.urgente || esperaPag) {
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
                if (esperaPag) PwSelo("Aguardando pagamento", androidx.compose.ui.graphics.Color(0xFF38BDF8), icone = PwIcones.Wallet)
            }
        }
    }
}

@Composable
private fun JanelaPedido(p: PedidoG, empresa: String?, onFechar: () -> Unit) {
    val c = Pw.cores
    var valor by remember(p.id) { mutableStateOf("%.2f".format(BR, p.valor ?: p.valorCalculado ?: 0.0)) }
    var motivo by remember(p.id) { mutableStateOf(p.ajusteMotivo.orEmpty()) }
    var recusar by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    fun rodar(msg: String, fechar: Boolean = true, bloco: suspend () -> Result<Unit>) {
        ocupado = true
        escopo.launch {
            bloco().onSuccess { erro = null; PwToast.mostrar(msg, TipoAviso.OK); Gestao.atualizar(); if (fechar) onFechar() }.onFailure { erro = it.message }
            ocupado = false
        }
    }
    val valorNum = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
    val mudou = valorNum != null && kotlin.math.abs(valorNum - (p.valor ?: 0.0)) > 0.004

    PwTelaCheia("Pedido ${p.numeroExibicao}", onFechar, subtitulo = "${nomeCliente(p, empresa)} · ${dataHora(p.criadoEm)}") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwSelo(StatusPedido.rotulo[p.status] ?: p.status, if (p.status == "recebido") c.ouro else c.texto3)
            PwSelo(if (p.planejado) "Planejado" else "Agora", if (p.planejado) c.azul else c.ouro)
            if (p.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
            PwSelo(if (p.cobranca == "credito") "Crédito (fatura)" else "Pré-pago", c.texto2)
        }
        if (p.planejado && p.quando != null) Text("Para ${dataHora(p.quando)}", color = c.azul, fontWeight = FontWeight.SemiBold)

        PwCartao {
            p.pontos.forEach { pt ->
                LinhaEndereco(if (pt.coleta) PwIcones.ArrowUpFromLine else PwIcones.ArrowDownToLine, pt.endereco + (pt.contatoNome?.let { " · $it" } ?: ""), if (pt.coleta) c.ouro else c.texto)
            }
            if (p.distanciaM != null) TextoDica("Distância estimada: ${km(p.distanciaM)}")
        }
        if (p.itens.isNotEmpty()) PwCartao {
            p.itens.forEach { i ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(PwIcones.Package2, null, tint = c.texto3, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${i.quantidade}× ${i.descricao}" + (i.pesoKg?.let { " · ${numeroBr(it)} kg" } ?: ""), color = c.texto, fontSize = 13.5.sp)
                }
            }
        }
        p.observacoes?.let { TextoDica("Obs.: $it") }

        PwCartao(destaque = true) {
            Text("Calculado: ${moedaBr(p.valorCalculado)}" + if (p.valorAjuste != 0.0) " · ajuste ${if (p.valorAjuste > 0) "+" else "−"}${moedaBr(kotlin.math.abs(p.valorAjuste))}" else "", color = c.texto2, fontSize = 13.sp)
            Spacer(Modifier.size(6.dp))
            PwCampo(valor, { valor = it }, "Valor do pedido", teclado = KeyboardType.Decimal, prefixo = "R$ ")
            if (mudou || p.valorAjuste != 0.0) PwCampo(motivo, { motivo = it }, "Motivo da mudança", dica = "Ex.: cliente fixo, rota longa, desconto combinado")
            if (mudou && p.status != "recebido") {
                Spacer(Modifier.size(6.dp))
                PwBotaoVidro("Salvar valor", onClick = { rodar("Valor ajustado.", fechar = false) { Gestao.revisarPedido(p.id, "ajustar", valorNum, motivo.ifBlank { null }) } }, icone = PwIcones.Check, modifier = Modifier.fillMaxWidth())
            }
            if (p.status in setOf("recebido", "aprovado")) {
                Spacer(Modifier.size(6.dp))
                Text("Recalcular com as regras de agora", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.clickable { rodar("Recalculado.", fechar = false) { Gestao.revisarPedido(p.id, "recalcular") } }.padding(vertical = 6.dp))
            }
        }
        if (p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" && p.status !in setOf("cancelado", "recusado", "concluido")) {
            PwCartao(cor = androidx.compose.ui.graphics.Color(0xFF38BDF8).copy(alpha = 0.6f)) {
                com.pwexpress.core.designsystem.PwRotulo("Cliente pré-pago · aguardando pagamento")
                TextoDica((if (p.status == "recebido") "Depois de aprovado, o pedido" else "O pedido") + " só vira rota quando o pagamento for confirmado. " +
                    "Ou ligue “Montar rota sem esperar o pagamento” nas regras deste pedido ou do cliente.")
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("PIX recebido", onClick = { rodar("Pagamento confirmado: o pedido segue para as rotas.", fechar = false) { Gestao.confirmarPagamentoPedido(p.id, "pix") } },
                        icone = PwIcones.Check, modifier = Modifier.weight(1f))
                    PwBotaoVidro("Cartão", onClick = { rodar("Pagamento confirmado: o pedido segue para as rotas.", fechar = false) { Gestao.confirmarPagamentoPedido(p.id, "cartao") } },
                        icone = PwIcones.Wallet, modifier = Modifier.weight(1f))
                }
            }
        }
        if (p.status !in setOf("recebido", "cancelado", "recusado", "concluido")) SituacaoRotaG(p.id, "${p.status}|${p.pagamentoStatus}|${p.regras}")
        PwErro(erro)
        p.recusaMotivo?.let { TextoDica("Motivo da recusa: $it") }

        if (p.status == "recebido") {
            PwBotaoOuro(if (ocupado) "Aprovando…" else "Aprovar" + if (mudou) " com ${moedaBr(valorNum)}" else "", onClick = {
                rodar(if (p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente") "Aprovado. Cliente pré-pago: a rota sai quando o pagamento for confirmado." else "Aprovado: vai para as rotas.") { Gestao.revisarPedido(p.id, "aprovar", if (mudou) valorNum else null, motivo.ifBlank { null }) }
            }, icone = PwIcones.Check, habilitado = !ocupado)
        }
        if (p.status in setOf("recebido", "aprovado")) {
            if (!recusar) PwBotaoPerigo("Recusar pedido", onClick = { recusar = true }, modifier = Modifier.fillMaxWidth())
            else {
                PwCampo(motivo, { motivo = it }, "Por que recusar?", dica = "O cliente vê esse motivo")
                PwBotaoPerigo("Confirmar recusa", onClick = { rodar("Pedido recusado.") { Gestao.revisarPedido(p.id, "recusar", null, motivo.ifBlank { null }) } }, cheio = true, modifier = Modifier.fillMaxWidth())
            }
        }
        if (p.status in setOf("recebido", "aprovado")) {
            var regras by remember { mutableStateOf(false) }
            PwBotaoVidro("Regras deste pedido" + if (p.regras.isNotEmpty()) " (${p.regras.size})" else "", onClick = { regras = true }, icone = PwIcones.Layers, modifier = Modifier.fillMaxWidth())
            if (regras) RegrasPedidoG(p.id, p.regras, onFechar = { regras = false })
        }
        if (p.status in setOf("aprovado", "roteirizado", "em_rota")) {
            PwBotaoVidro("Voltar para revisão", onClick = {
                rodar("Pedido voltou para revisão: as rotas dele foram desfeitas.", fechar = false) { Gestao.revisarPedido(p.id, "voltar_revisao", null, motivo.ifBlank { null }) }
            }, icone = PwIcones.History, modifier = Modifier.fillMaxWidth())
            TextoDica("Só enquanto nenhuma coleta ou entrega dele foi feita. Serve para refazer as rotas com regras novas.")
        }
        Spacer(Modifier.size(12.dp))
    }
}

/** Peças das rotas automáticas no detalhe: soltar para os motoboys e o pagamento do motoboy. */
@Composable
internal fun BlocoRotaG(corrida: com.pwexpress.feature.gestor.data.CorridaG, ocupado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val aberta = corrida.motoboyId == null && corrida.status in setOf("montada", "agendada", "aguardando_motoboy", "sem_motoboy")
    if (aberta) {
        var moto by remember { mutableStateOf<String?>(null) }
        PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
            com.pwexpress.core.designsystem.PwRotulo(if (corrida.status == "montada") "Rota montada: solte para os motoboys" else "Despacho")
            corrida.sugeridoMotoboyId?.let { TextoDica("Melhor agora: ${dados.membro(it)?.nome ?: "motoboy"}") }
            if (corrida.modoDespacho == "quadro" && corrida.status != "montada") TextoDica("No quadro${corrida.quadroDesde?.let { " desde ${hora(it)}" } ?: ""}: todos veem, o primeiro que pegar leva.")
            Espaco(8)
            PwBotaoOuro("Quadro (todos veem)", onClick = { acao { Gestao.liberarRota(corrida.id, "quadro") } }, icone = PwIcones.Users, habilitado = !ocupado)
            if (corrida.agendadaPara == null) {
                Espaco(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("Melhor primeiro", onClick = { acao { Gestao.liberarRota(corrida.id, "melhor_primeiro") } }, icone = PwIcones.Crown, modifier = Modifier.weight(1f))
                    PwBotaoVidro("Cascata", onClick = { acao { Gestao.liberarRota(corrida.id, "cascata") } }, icone = PwIcones.Shuffle, modifier = Modifier.weight(1f))
                }
            }
            Espaco(8)
            com.pwexpress.core.designsystem.PwSelecao(
                "Entregar direto para", dados.equipe.filter { it.motoboy && it.modoTeste == corrida.teste }.map { it.id to ((if (it.id == corrida.sugeridoMotoboyId) "★ " else "") + it.nome) }, moto, { moto = it },
            )
            Espaco(6)
            PwBotaoVidro("Enviar para o escolhido", onClick = { moto?.let { m -> acao { Gestao.liberarRota(corrida.id, null, m) } } }, icone = PwIcones.Send, modifier = Modifier.fillMaxWidth())
        }
    }

    if (aberta && corrida.entregas > 0) {
        val outras = dados.corridas.filter { it.id != corrida.id && it.teste == corrida.teste && it.motoboyId == null && it.status in setOf("montada", "agendada", "aguardando_motoboy", "sem_motoboy") }
        PwCartao {
            com.pwexpress.core.designsystem.PwRotulo("Mover entregas")
            TextoDica("Separe uma entrega numa rota nova ou junte com outra rota aberta.")
            corrida.paradas.filter { !it.coleta }.forEach { p ->
                Espaco(6)
                Text("${p.ordem + 1}. ${p.endereco.substringBefore(",")}", color = Pw.cores.texto, fontSize = 13.5.sp, maxLines = 1)
                com.pwexpress.core.designsystem.PwSelecao("", listOf<Pair<String?, String>>(null to "Para uma rota nova") + outras.map { it.id to "Para a rota ${it.numeroExibicao}" }, "—",
                    { destino -> acao { Gestao.moverEntrega(p.id, destino) } }, vazio = "Mover…")
            }
        }
    }

    val calc = corrida.pagCalculo ?: return
    var ajuste by remember(corrida.id) { mutableStateOf(numeroBr(corrida.pagAjuste)) }
    var motivo by remember(corrida.id) { mutableStateOf(corrida.pagAjusteMotivo.orEmpty()) }
    PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Pagamento do motoboy" + if (corrida.motoboyId == null) " (prévia)" else "")
        Espaco(4)
        LinhasCalculo(calc)
        if (corrida.motoboyId != null) {
            Espaco(8)
            PwCampo(ajuste, { ajuste = it }, "Ajuste (+/−)", teclado = KeyboardType.Text, prefixo = "R$ ")
            PwCampo(motivo, { motivo = it }, "Motivo", dica = "Ex.: chuva, espera longa, rota mais curta")
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoVidro("Salvar ajuste", onClick = { acao { Gestao.ajustarPagamento(corrida.id, ajuste.replace(",", ".").toDoubleOrNull() ?: 0.0, motivo.ifBlank { null }) } }, icone = PwIcones.Check, modifier = Modifier.weight(1f))
                PwBotaoVidro("Recalcular", onClick = { acao { Gestao.recalcularPagamento(corrida.id) } }, icone = PwIcones.RefreshCw, modifier = Modifier.weight(1f))
            }
            Espaco(6)
            Text("A pagar: ${moedaBr(corrida.valorMotoboy)}", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private val REGRA = mapOf(
    "mediana" to "entrega do meio", "percentil" to "percentil", "media" to "média das entregas",
    "mais_longe" to "entrega mais longe", "mais_perto" to "entrega mais perto", "total_rota" to "rota inteira",
)

@Composable
private fun LinhasCalculo(calc: kotlinx.serialization.json.JsonObject) {
    val c = Pw.cores
    fun n(k: String) = (calc[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
    fun s(k: String) = (calc[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    @Composable fun Linha(a: String, b: String, forte: Boolean = false) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(a, color = if (forte) c.texto else c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(b, color = c.texto, fontSize = 13.sp, fontWeight = if (forte) FontWeight.Bold else FontWeight.SemiBold)
        }
    }
    if (s("modo") == "percentual") { Linha("Percentual: ${numeroBr(n("pct") ?: 0.0)}% da rota", moedaBr(n("total")), true); return }
    val dist = (calc["distancias_km"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() }.orEmpty()
    val regra = (REGRA[s("regra")] ?: s("regra").orEmpty()) + if (s("regra") == "percentil") " ${s("percentil")}%" else ""
    Linha("Km de referência ($regra)", "${numeroBr(n("km_referencia") ?: 0.0)} km × ${moedaBr(n("valor_km"))}")
    if (dist.isNotEmpty()) TextoDica("Entregas: ${dist.joinToString(", ") { numeroBr(it) }} km · ${if (s("distancia") == "acumulada") "acumulado na rota" else "coleta → entrega"}")
    if ((n("entregas_extras") ?: 0.0) > 0) Linha("Entregas a mais", "${n("entregas_extras")!!.toInt()} × ${moedaBr(n("valor_entrega_extra"))}")
    if ((n("coletas_extras") ?: 0.0) > 0 && (n("valor_coleta_extra") ?: 0.0) > 0) Linha("Coletas a mais", "${n("coletas_extras")!!.toInt()} × ${moedaBr(n("valor_coleta_extra"))}")
    if ((n("fixo") ?: 0.0) > 0) Linha("Fixo por rota", moedaBr(n("fixo")))
    if ((n("minimo") ?: 0.0) > 0) Linha("Mínimo", moedaBr(n("minimo")))
    Linha("Calculado", moedaBr(n("total")), true)
}


/** Por que o pedido ainda não virou rota (ou em que pé a rota está), com "Montar rota agora". */
@Composable
private fun SituacaoRotaG(pedidoId: String, chave: String) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var codigo by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var rodando by remember { mutableStateOf(false) }
    fun ler(r: Result<kotlinx.serialization.json.JsonElement>) {
        r.onSuccess { j -> val o = j as? kotlinx.serialization.json.JsonObject; codigo = o?.str("codigo"); motivo = o?.str("motivo") }
         .onFailure { e -> codigo = "erro"; motivo = if (e.message.orEmpty().contains("situacao_rota") || e.message.orEmpty().contains("roteirizar_pedido"))
             "Rode a migração 20261110000004_situacao_rota.sql para ver aqui por que o pedido ainda não virou rota." else "Erro: ${e.message}" }
    }
    androidx.compose.runtime.LaunchedEffect(pedidoId, chave) {
        while (true) {
            ler(Gestao.rpcJson("situacao_rota", kotlinx.serialization.json.buildJsonObject { put("p_pedido", kotlinx.serialization.json.JsonPrimitive(pedidoId)) }))
            kotlinx.coroutines.delay(20_000)
        }
    }
    val m = motivo ?: return
    val semRota = codigo?.startsWith("rota_") != true
    PwCartao(cor = if (semRota) androidx.compose.ui.graphics.Color(0xFFFB923C).copy(alpha = 0.6f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Route, null, tint = if (semRota) androidx.compose.ui.graphics.Color(0xFFFB923C) else c.ouro, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(m, color = c.texto, fontSize = 13.5.sp)
        }
        if (semRota && !m.startsWith("Rode a migração")) {
            Spacer(Modifier.size(8.dp))
            PwBotaoVidro(if (rodando) "Montando…" else "Montar rota agora", onClick = {
                rodando = true
                escopo.launch {
                    ler(Gestao.rpcJson("roteirizar_pedido", kotlinx.serialization.json.buildJsonObject { put("p_pedido", kotlinx.serialization.json.JsonPrimitive(pedidoId)) }))
                    rodando = false
                }
            }, icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
        }
    }
}
