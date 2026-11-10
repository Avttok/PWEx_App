package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
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
    val pedidoPedido by GestorNav.pedidoAberto.collectAsState()
    LaunchedEffect(pedidoPedido) { pedidoPedido?.let { aberto = it; GestorNav.pedidoAberto.value = null } }

    var busca by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableIntStateOf(0) }
    val tipos: List<Pair<String, (PedidoG) -> Boolean>> = listOf("Todos" to { _ -> true }, "Agora" to { p -> !p.planejado }, "Planejados" to { p -> p.planejado },
        "Urgentes" to { p -> p.urgente }, "Aguardando pagamento" to { p -> p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" })
    // motoboy de cada pedido: pelas rotas que levam os endereços dele
    fun motoboys(p: PedidoG): String = dados.corridas.filter { cor -> cor.teste == p.teste && cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } && cor.criadaEm >= p.criadoEm - 60_000 }
        .mapNotNull { cor -> cor.motoboyId?.let { dados.membro(it)?.nome } ?: cor.motoboyNomeArquivado }.joinToString(" ")
    val todos = dados.pedidos.filter { it.teste == teste }.filter { p ->
        tipos[tipo].second(p) && (busca.isBlank() || combinaBusca(busca, p.numeroExibicao, p.numero.toString(), dados.empresa(p.empresaId)?.nome, p.clienteNome, p.observacoes,
            p.tipoPedido, StatusPedido.rotulo[p.status], p.pontos.joinToString(" ") { "${it.endereco} ${it.contatoNome ?: ""}" }, p.itens.joinToString(" ") { it.descricao }, motoboys(p)))
    }
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
        CampoBusca(busca, { busca = it }, "Buscar nº, cliente, endereço, motoboy…")
        PwFiltros(listOf("Revisar" to listas[0].size, "Em andamento" to listas[1].size, "Concluídos" to 0, "Cancelados" to 0), filtro, { filtro = it })
        PwFiltros(tipos.map { it.first to 0 }, tipo, { tipo = it })
        when {
            !dados.carregado -> PwCarregando()
            lista.isEmpty() -> PwVazio(if (busca.isNotBlank() || tipo > 0) "Nenhum pedido encontrado." else if (filtro == 0) "Nenhum pedido esperando revisão." else "Nenhum pedido aqui.", icone = PwIcones.ClipboardList)
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
            Spacer(Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Share2, "Mandar no chat", {
                com.pwexpress.core.chat.Chat.compartilhar(com.pwexpress.core.chat.Anexo("pedido", p.id, "Pedido ${p.numeroExibicao}", nomeCliente(p, empresa))); onFechar()
            }, tamanho = 34.dp)
        }
        if (p.planejado && p.quando != null) Text("Para ${dataHora(p.quando)}", color = c.azul, fontWeight = FontWeight.SemiBold)

        var focoP by remember(p.id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
        MapaPedidoG(p, foco = focoP)
        DetalhesPedidoG(p) { la, lo -> focoP = com.pwexpress.core.mapa.FocoMapa(la, lo) }

        PwCartao(destaque = true) {
            p.orcamento?.let { OrcamentoG(it) } ?: TextoDica("Sem orçamento.")
            Spacer(Modifier.size(8.dp))
            Text("Calculado: ${moedaBr(p.valorCalculado)}" + (if (p.valorAjuste != 0.0) " · ajuste ${if (p.valorAjuste > 0) "+" else "−"}${moedaBr(kotlin.math.abs(p.valorAjuste))}" else "") +
                (p.ajusteMotivo?.takeIf { p.valorAjuste != 0.0 }?.let { " ($it)" } ?: "") + " · Dev ${moedaBr(p.valorDev)} · " +
                when { p.cobranca == "credito" -> if (p.faturaId != null) "já na fatura" else "entra na próxima fatura"; p.pagamentoStatus == "pago" -> "pago (${p.pagamentoMetodo ?: ""})"; else -> "pagamento antecipado pendente" },
                color = c.texto2, fontSize = 13.sp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.pwexpress.core.designsystem.PwRotulo(if (corrida.status == "montada") "Rota montada: solte para os motoboys" else "Despacho")
                Spacer(Modifier.width(8.dp))
                com.pwexpress.core.designsystem.PwDica("Como soltar a rota", "Quadro: todos veem e o primeiro que pegar leva. Melhor primeiro: só o mais indicado recebe a oferta; se não aceitar, vai para o quadro. Cascata: a oferta passa de um em um, do mais perto ao mais longe. Direto: o escolhido recebe a oferta.")
            }
            corrida.sugeridoMotoboyId?.let { TextoDica("Melhor agora: ${dados.membro(it)?.nome ?: "motoboy"}") }
            if (corrida.modoDespacho == "quadro" && corrida.status != "montada") TextoDica("No quadro${corrida.quadroDesde?.let { " desde ${hora(it)}" } ?: ""}: todos veem, o primeiro que pegar leva.")
            Espaco(8)
            PwBotaoOuro("Quadro (todos veem)", onClick = { acao { Gestao.liberarRota(corrida.id, "quadro") } }, icone = PwIcones.Users, habilitado = !ocupado)
            if (corrida.agendadaPara != null) TextoDica("Agendada para ${dataHora(corrida.agendadaPara)}: quem aceitar a oferta fica com a reserva (não começa agora).")
            run {
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
            PwBotaoVidro("Enviar oferta para o escolhido", onClick = { moto?.let { m -> acao { Gestao.liberarRota(corrida.id, null, m) } } }, icone = PwIcones.Send, modifier = Modifier.fillMaxWidth())
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
    val limitado = s("limitado") == "true"
    val avisoTeto = "Limitado a ${moedaBr(n("teto"))}: o motoboy nunca leva mais que a rota paga (valor da rota menos a parte do Dev)."
    if (s("modo") == "percentual") {
        val origem = when (s("dev_origem")) { "total" -> " · Dev sai antes de dividir"; "proporcional" -> " · Dev sai das duas partes"; "gestor" -> " · Dev sai da parte do gestor"; else -> "" }
        Linha("Percentual: ${numeroBr(n("pct") ?: 0.0)}% da rota$origem", moedaBr(n("total")), true)
        if (limitado) TextoDica(avisoTeto)
        return
    }
    val dist = (calc["distancias_km"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() }.orEmpty()
    val regra = (REGRA[s("regra")] ?: s("regra").orEmpty()) + if (s("regra") == "percentil") " ${s("percentil")}%" else ""
    Linha("Km de referência ($regra)", "${numeroBr(n("km_referencia") ?: 0.0)} km × ${moedaBr(n("valor_km"))}")
    if (dist.isNotEmpty()) TextoDica("Entregas: ${dist.joinToString(", ") { numeroBr(it) }} km · ${if (s("distancia") == "acumulada") "acumulado na rota" else "coleta → entrega"}")
    if ((n("entregas_extras") ?: 0.0) > 0) Linha("Entregas a mais", "${n("entregas_extras")!!.toInt()} × ${moedaBr(n("valor_entrega_extra"))}")
    if ((n("coletas_extras") ?: 0.0) > 0 && (n("valor_coleta_extra") ?: 0.0) > 0) Linha("Coletas a mais", "${n("coletas_extras")!!.toInt()} × ${moedaBr(n("valor_coleta_extra"))}")
    if ((n("fixo") ?: 0.0) > 0) Linha("Fixo por rota", moedaBr(n("fixo")))
    if ((n("minimo") ?: 0.0) > 0) Linha("Mínimo", moedaBr(n("minimo")))
    if (limitado && n("calculado") != null) Linha("Pelo cálculo", moedaBr(n("calculado")))
    Linha(if (limitado) "Fica (teto da rota)" else "Calculado", moedaBr(n("total")), true)
    if (limitado) TextoDica(avisoTeto)
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


/** Mapa do pedido: coleta e entregas, o caminho da rota (quando já tem) e o motoboy ao vivo. */
@Composable
internal fun MapaPedidoG(p: PedidoG, altura: androidx.compose.ui.unit.Dp = 220.dp, foco: com.pwexpress.core.mapa.FocoMapa? = null) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val pontos = p.pontos.filter { it.lat != null && it.lng != null }
    if (pontos.isEmpty()) return
    val pinos = pontos.map { com.pwexpress.core.mapa.PinoMapa(it.lat!!, it.lng!!, it.coleta, false, it.status == "concluido") }
    // rota que leva este pedido (mesmos endereços, criada depois dele)
    val rota = dados.corridas.firstOrNull { cor -> cor.teste == p.teste && cor.status != "cancelada" && cor.criadaEm >= p.criadoEm - 60_000 &&
        cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } }
    val moto = rota?.motoboyId?.let { id -> dados.posicao(id)?.takeIf { it.lat != null && it.lng != null }?.let { pos ->
        com.pwexpress.core.mapa.MarcadorMapa(id, pos.lat!!, pos.lng!!, corHex(c.ouro), dados.membro(id)?.nome?.substringBefore(" ") ?: "Motoboy", foto = dados.membro(id)?.foto, prioridade = 2) } }
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val trazer = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(foco) { if (foco != null) trazer.bringIntoView() }
    Box(Modifier.fillMaxWidth().height(altura).clip(forma).border(1.dp, c.borda, forma).bringIntoViewRequester(trazer)) {
        val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(rota?.takeIf { it.motoboyId != null }?.id, rota?.ativa == true)
        val posMoto = moto?.takeIf { rota?.ativa == true }?.let { it.lat to it.lng }
        com.pwexpress.core.mapa.PwMapa(paradas = pinos, rota = rota?.rota, minhaPosicao = null, marcadores = listOfNotNull(moto),
            modifier = Modifier.fillMaxSize(), enquadrar = "${p.id}-${pinos.size}-${moto != null}", focar = foco,
            trilhas = if (feito.size >= 2) listOf(com.pwexpress.core.mapa.TrilhaMapa(feito + listOfNotNull(posMoto))) else emptyList(),
            restanteDe = posMoto)
        if (rota != null) Row(Modifier.align(Alignment.TopStart).padding(10.dp)) {
            PwSelo("Rota ${rota.numeroExibicao}" + (rota.motoboyId?.let { " · ${dados.membro(it)?.nome?.substringBefore(" ") ?: ""}" } ?: ""), c.ouro, icone = PwIcones.Route)
        }
    }
}


/** Pontos numerados (como no mapa) com contato, instruções, itens de cada um e a entrega feita; itens com categoria e medidas; rotas. */
@Composable
private fun DetalhesPedidoG(p: PedidoG, onFocar: (Double, Double) -> Unit = { _, _ -> }) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var categorias by remember { mutableStateOf<List<com.pwexpress.feature.gestor.data.CategoriaG>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { categorias = runCatching { Gestao.catalogoPedido().second }.getOrDefault(emptyList()) }
    PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Pontos · toque para ver no mapa")
        Spacer(Modifier.size(6.dp))
        p.pontos.forEachIndexed { i, pt ->
            val itens = p.itens.filter { it.entregaId == pt.id || it.coletaId == pt.id }
            Row(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(enabled = pt.lat != null) { onFocar(pt.lat!!, pt.lng!!) }.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (pt.coleta) c.ouro else c.texto), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = if (pt.coleta) c.sobreOuro else c.fundo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text((if (pt.coleta) "Coleta" else "Entrega") + if (pt.status == "concluido") " · feita" else "", color = if (pt.coleta) c.ouro else c.texto2,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(pt.endereco, color = c.texto, fontSize = 14.sp)
                    listOfNotNull(pt.contatoNome, pt.contatoTelefone, pt.instrucoes).takeIf { it.isNotEmpty() }?.let { TextoDica(it.joinToString(" · ")) }
                    if (itens.isNotEmpty()) TextoDica(itens.joinToString(" · ") { "${it.quantidade}× ${it.descricao}" })
                    pt.concluidoEm?.let { TextoDica("✓ ${dataHora(it)}" + (pt.recebedorNome?.let { r -> " · recebido por $r" } ?: "")) }
                    SelosPontoG(pt.exigeFoto, pt.exigeNome, pt.exigeAssinatura, pt.exigeCodigo, de = pt.janelaDe, ate = pt.janelaAte)
                }
            }
        }
        if (p.distanciaM != null) TextoDica("Distância estimada: ${km(p.distanciaM)}")
    }
    if (p.itens.isNotEmpty()) PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Itens")
        p.itens.forEach { it2 ->
            val k = categorias.firstOrNull { it.id == it2.categoriaId }
            Column(Modifier.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Package2, null, tint = if (k?.perigoso == true) c.vermelho else c.ouro, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${it2.quantidade}× ${it2.descricao}", color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (k?.perigoso == true) PwSelo("Perigoso", c.vermelho)
                }
                TextoDica(listOfNotNull(k?.nome ?: "Sem categoria", it2.pesoKg?.let { "${numeroBr(it)} kg" },
                    it2.comprimento?.let { "${numeroBr(it)}×${numeroBr(it2.largura ?: 0.0)}×${numeroBr(it2.altura ?: 0.0)} cm" },
                    it2.valorDeclarado?.let { "declarado ${moedaBr(it)}" }, it2.observacoes).joinToString(" · "))
            }
        }
    }
    p.observacoes?.let { o -> PwCartao { com.pwexpress.core.designsystem.PwRotulo("Observações"); Text(o, color = c.texto, fontSize = 14.sp) } }
    // rotas que levam este pedido
    val rotas = dados.corridas.filter { cor -> cor.teste == p.teste && cor.criadaEm >= p.criadoEm - 60_000 && cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } }
    if (rotas.isNotEmpty()) PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Rotas")
        rotas.forEach { r ->
            TextoDica("Rota ${r.numeroExibicao} · ${com.pwexpress.feature.gestor.data.Status.rotulo[r.status] ?: r.status}" +
                (r.motoboyId?.let { " · ${dados.membro(it)?.nome ?: ""}" } ?: "") + " · ${r.entregas} entrega${if (r.entregas == 1) "" else "s"}")
        }
    }
}

/** Como o valor foi calculado (as mesmas linhas do site). */
@Composable
internal fun OrcamentoG(o: kotlinx.serialization.json.JsonObject) {
    val c = Pw.cores
    fun num(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
    fun txt(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    com.pwexpress.core.designsystem.PwRotulo("Como chegou no valor")
    Spacer(Modifier.size(4.dp))
    (o["linhas"] as? kotlinx.serialization.json.JsonArray)?.forEach { l ->
        val x = l as? kotlinx.serialization.json.JsonObject ?: return@forEach
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(txt(x["rotulo"]).orEmpty(), color = c.texto, fontSize = 14.sp)
                txt(x["detalhe"])?.let { Text(it, color = c.texto3, fontSize = 12.sp) }
            }
            Text(moedaBr(num(x["valor"])), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    (o["percentuais"] as? kotlinx.serialization.json.JsonArray)?.forEach { l ->
        val x = l as? kotlinx.serialization.json.JsonObject ?: return@forEach
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(txt(x["rotulo"]).orEmpty(), color = c.texto2, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("+${numeroBr(num(x["pct"]) ?: 0.0)}%", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    num(o["minimo"])?.takeIf { it > 0 }?.let { m ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text("Mínimo", color = c.texto2, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(moedaBr(m), color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(c.borda))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(moedaBr(num(o["total"])), color = c.ouro, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
    val vol = num(o["volumes"])?.toInt() ?: 0
    TextoDica("$vol volume${if (vol == 1) "" else "s"} · peso real ${numeroBr(num(o["peso_real_kg"]) ?: 0.0)} kg · cubado ${numeroBr(num(o["peso_cubado_kg"]) ?: 0.0)} kg" +
        (if (txt(o["perigoso"]) == "true") " · perigoso" else ""))
}
