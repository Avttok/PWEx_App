package com.pwexpress.feature.gestor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwPonto
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoFlutuante
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinha
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.coordenadaValida
import androidx.compose.ui.graphics.toArgb
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.GRUPOS_CONFIG
import com.pwexpress.feature.gestor.data.Geo
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.ItemRegistro
import com.pwexpress.feature.gestor.data.PARAMETROS
import com.pwexpress.feature.gestor.data.Param
import com.pwexpress.feature.gestor.data.Precos
import com.pwexpress.feature.gestor.data.logico
import com.pwexpress.feature.gestor.data.numero
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// =====================================================================
// Empresas
// =====================================================================

@Composable
fun EmpresasG(onAbrir: (String?) -> Unit) {
    val dados by Gestao.dados.collectAsState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TituloTela("Clientes", if (dados.empresas.size == 1) "1 cliente cadastrado" else "${dados.empresas.size} clientes cadastrados", PwIcones.Building2) }
            if (dados.carregado && dados.empresas.isEmpty()) item { PwVazio("Nenhum cliente cadastrado ainda.", icone = PwIcones.Building2) }
            items(dados.empresas, key = { it.id }) { e ->
                PwCartao(padding = PaddingValues(6.dp)) {
                    PwLinha(
                        e.nome,
                        subtitulo = listOfNotNull(
                            when (e.modalidade) { "por_km" -> "${moedaBr(e.valorKm)}/km"; "fixo" -> moedaBr(e.valorFixo); else -> "Por zona" },
                            e.documento?.let { if (e.tipo == "pf") formatarCpf(it) else formatarCnpj(it) }, if (e.podeExpress) "Express" else "só agendados",
                        ).joinToString(" · "),
                        icone = if (e.tipo == "pf") PwIcones.User else PwIcones.Building2, onClick = { onAbrir(e.id) },
                    )
                }
            }
        }
        PwBotaoFlutuante(PwIcones.Plus, "Novo cliente", { onAbrir(null) }, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 108.dp))
    }
}

private fun formatarCpf(s: String) = s.filter { it.isDigit() }.let { d -> if (d.length == 11) "${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}" else s }

/** Dígitos verificadores do CPF. */
private fun cpfValido(s: String): Boolean {
    val d = s.filter { it.isDigit() }.map { it - '0' }
    if (d.size != 11 || d.distinct().size == 1) return false
    fun dv(n: Int): Int { val soma = (0 until n).sumOf { d[it] * (n + 1 - it) }; val r = soma * 10 % 11; return if (r == 10) 0 else r }
    return dv(9) == d[9] && dv(10) == d[10]
}

private fun formatarCnpj(s: String) = s.filter { it.isDigit() }.let { d -> if (d.length == 14) "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}" else s }

@Composable
fun EmpresaG(id: String?, onVoltar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val e = id?.let { dados.empresa(it) }
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(e?.nome.orEmpty()) }
    var cnpj by remember { mutableStateOf(e?.cnpj?.let(::formatarCnpj).orEmpty()) }
    var razao by remember { mutableStateOf(e?.razaoSocial.orEmpty()) }
    var fantasia by remember { mutableStateOf(e?.nomeFantasia.orEmpty()) }
    var situacao by remember { mutableStateOf(e?.situacao.orEmpty()) }
    var telefone by remember { mutableStateOf(e?.telefone.orEmpty()) }
    var email by remember { mutableStateOf(e?.email.orEmpty()) }
    var endereco by remember { mutableStateOf(e?.endereco.orEmpty()) }
    var lat by remember { mutableStateOf(e?.lat) }
    var lng by remember { mutableStateOf(e?.lng) }
    var instrucoes by remember { mutableStateOf(e?.instrucoesColeta.orEmpty()) }
    var modalidade by remember { mutableStateOf(e?.modalidade ?: "por_km") }
    var valorKm by remember { mutableStateOf(e?.valorKm?.toString()?.replace('.', ',') ?: dados.config.numero("valor_km_padrao", 2.0).toString().replace('.', ',')) }
    var valorFixo by remember { mutableStateOf(e?.valorFixo?.toString()?.replace('.', ',').orEmpty()) }
    var aoVivo by remember { mutableStateOf(e?.clienteVeAoVivo ?: false) }
    var observacoes by remember { mutableStateOf(e?.observacoes.orEmpty()) }
    var tipo by remember { mutableStateOf(e?.tipo ?: "pj") }
    var cpf by remember { mutableStateOf(e?.cpf?.let(::formatarCpf).orEmpty()) }
    var podeExpress by remember { mutableStateOf(e?.podeExpress ?: false) }
    val metodos = remember { mutableStateListOf<String>().apply { addAll(e?.pagamentoMetodos ?: listOf("pix", "cartao")) } }
    var repasse by remember { mutableStateOf(e?.repassePct?.let { numeroBr(it) }.orEmpty()) }
    var devPct by remember { mutableStateOf(e?.devPct?.let { numeroBr(it) }.orEmpty()) }
    var criandoAcesso by remember { mutableStateOf(false) }
    val favoritos = remember { mutableStateListOf<String>().apply { addAll(dados.favoritos[id].orEmpty()) } }
    var msg by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf(false) }
    var marcarMapa by remember { mutableStateOf(false) }
    fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

    fun localizar() = escopo.launch {
        val r = Geo.buscarEnderecos(endereco).firstOrNull()
        if (r != null) { lat = r.lat; lng = r.lng; erro = null } else erro = "Não encontrei esse endereço no mapa. Ajuste o texto ou toque no mapa."
    }

    PwPagina {
        Cabecalho(if (e == null) "Novo cliente" else e.nome, onVoltar, subtitulo = "Dados, preço, pagamento e coleta padrão")
        PwSegmentado(listOf("Empresa (CNPJ)" to PwIcones.Building2, "Pessoa (CPF)" to PwIcones.User), if (tipo == "pf") 1 else 0, { tipo = if (it == 1) "pf" else "pj" })
        if (tipo == "pf") PwCartao {
            PwRotulo("Pessoa física")
            Espaco(8)
            PwCampo(cpf, { cpf = it }, "CPF", teclado = KeyboardType.Number)
            if (cpf.count { it.isDigit() } == 11 && !cpfValido(cpf)) { Espaco(4); PwErro("CPF inválido — confira os números.") }
        }
        if (tipo == "pj") PwCartao {
            PwRotulo("Receita Federal")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(cnpj, { cnpj = it }, "CNPJ", teclado = KeyboardType.Number, modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.Search, "Consultar na Receita", habilitado = !ocupado, tamanho = 52.dp, ativo = cnpj.count { it.isDigit() } == 14, onClick = {
                    val d = cnpj.filter { it.isDigit() }
                    if (d.length != 14) { erro = "Digite os 14 dígitos do CNPJ."; return@PwBotaoIcone }
                    ocupado = true
                    escopo.launch {
                        Gestao.consultarCnpj(d).onSuccess { r ->
                            if (nome.isBlank()) nome = r.nomeFantasia ?: r.razaoSocial.orEmpty()
                            razao = r.razaoSocial.orEmpty(); fantasia = r.nomeFantasia.orEmpty(); situacao = r.situacao.orEmpty()
                            if (telefone.isBlank()) telefone = r.telefone.orEmpty()
                            if (email.isBlank()) email = r.email.orEmpty()
                            if (endereco.isBlank() && r.endereco != null) { endereco = r.endereco; localizar() }
                            msg = "Dados da Receita carregados."; erro = null
                        }.onFailure { erro = it.message }
                        ocupado = false
                    }
                })
            }
            if (razao.isNotBlank()) { Espaco(6); TextoDica("$razao${if (situacao.isNotBlank()) " · $situacao" else ""}") }
            if (situacao.isNotBlank() && situacao != "ATIVA") PwErro("Situação cadastral: $situacao")
        }
        PwCartao {
            PwCampo(nome, { nome = it }, "Nome (como aparece nas corridas)")
            Espaco(8); PwCampo(telefone, { telefone = it }, "Telefone", teclado = KeyboardType.Phone)
            Espaco(8); PwCampo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
        }
        PwCartao {
            PwRotulo("Preço")
            Espaco(8)
            PwSelecao("", Precos.modalidades, modalidade, { modalidade = it })
            Espaco(8)
            when (modalidade) {
                "por_km" -> PwCampo(valorKm, { valorKm = it }, "Valor do km", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                "fixo" -> PwCampo(valorFixo, { valorFixo = it }, "Preço fixo", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                else -> TextoDica("O valor é informado em cada corrida.")
            }
        }
        PwCartao {
            PwRotulo("Pedidos e pagamento")
            PwInterruptor("Pode pedir Express", podeExpress, { podeExpress = it }, descricao = if (podeExpress) "Pede corridas na hora, além das agendadas." else "Cliente novo: só pedidos agendados até você liberar.", icone = PwIcones.Zap)
            Espaco(4)
            TextoDica("Formas de pagamento aceitas (o pedido só sai depois de pago):")
            PwInterruptor("PIX", "pix" in metodos, { if (it) metodos.add("pix") else if (metodos.size > 1) metodos.remove("pix") })
            PwInterruptor("Cartão", "cartao" in metodos, { if (it) metodos.add("cartao") else if (metodos.size > 1) metodos.remove("cartao") })
            Espaco(6)
            TextoDica("Divisão própria deste cliente (em branco = padrão das configurações):")
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(repasse, { repasse = it }, "Motoboy %", Modifier.weight(1f), dica = numeroBr(dados.config.numero("repasse_motoboy_pct", 67.5)), teclado = KeyboardType.Decimal)
                PwCampo(devPct, { devPct = it }, "Dev %", Modifier.weight(1f), dica = numeroBr(dados.config.numero("dev_pct", 5.0)), teclado = KeyboardType.Decimal)
            }
        }
        PwCartao {
            PwRotulo("Coleta padrão")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(endereco, { endereco = it; lat = null; lng = null }, "Endereço de coleta", modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.Search, "Localizar endereço", { localizar() }, tamanho = 52.dp)
            }
            Espaco(8)
            val formaMapa = RoundedCornerShape(18.dp)
            Box(Modifier.fillMaxWidth().height(180.dp).clip(formaMapa).border(1.dp, c.borda, formaMapa)) {
                PwMapa(
                    paradas = listOfNotNull(lat?.let { la -> lng?.let { lo -> PinoMapa(la, lo, true, true, false) } }), rota = null, minhaPosicao = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp), enquadrar = "$lat,$lng",
                )
                // a prévia não se mexe: o toque abre o mapa em tela cheia, que é fácil de arrastar
                Box(Modifier.fillMaxSize().clickable { marcarMapa = true })
                PwPilula(if (lat != null) "Ajustar no mapa" else "Marcar no mapa", PwIcones.MapPinned, { marcarMapa = true }, Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
            Detalhe(if (lat != null) PwIcones.CircleCheck else PwIcones.MapPin, if (lat != null) "Coleta marcada no mapa" else "Sem local: busque o endereço ou marque no mapa", if (lat != null) c.verde else null)
            Espaco(8)
            PwCampo(instrucoes, { instrucoes = it }, "Instruções de coleta", linhas = 2)
        }
        PwCartao {
            PwRotulo("Motoboys favoritos")
            TextoDica("Recebem as corridas deste cliente antes dos outros, na ordem marcada.")
            dados.equipe.filter { it.motoboy }.forEach { m ->
                PwInterruptor(m.nome + (if (m.id in favoritos) "  · ${favoritos.indexOf(m.id) + 1}º" else ""), m.id in favoritos, { lig -> if (lig) favoritos.add(m.id) else favoritos.remove(m.id) })
            }
            Espaco(6)
            PwInterruptor("Cliente vê a corrida ao vivo", aoVivo, { aoVivo = it }, descricao = "No painel do cliente, mostra o motoboy no mapa durante a corrida.")
            PwCampo(observacoes, { observacoes = it }, "Observações internas", linhas = 2)
        }
        PwErro(erro)
        msg?.let { Text(it, color = c.verde, fontWeight = FontWeight.SemiBold) }
        PwBotaoOuro(if (ocupado) "Salvando…" else "Salvar", habilitado = !ocupado, icone = PwIcones.Check, onClick = {
            if (nome.isBlank()) { erro = "Informe o nome."; return@PwBotaoOuro }
            val d = if (tipo == "pj") cnpj.filter { it.isDigit() } else ""
            val dCpf = if (tipo == "pf") cpf.filter { it.isDigit() } else ""
            if (d.isNotEmpty() && d.length != 14) { erro = "CNPJ deve ter 14 dígitos."; return@PwBotaoOuro }
            if (dCpf.isNotEmpty() && !cpfValido(dCpf)) { erro = "CPF inválido."; return@PwBotaoOuro }
            val pr = num(repasse); val pd = num(devPct)
            if ((pr ?: 0.0) + (pd ?: 0.0) > 100 || (pr ?: 0.0) < 0 || (pd ?: 0.0) < 0) { erro = "A soma de motoboy + Dev não pode passar de 100%."; return@PwBotaoOuro }
            val linha = buildJsonObject {
                put("tipo", tipo); put("cpf", dCpf.ifEmpty { null }); put("pode_express", podeExpress)
                put("pagamento_metodos", kotlinx.serialization.json.buildJsonArray { metodos.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                put("repasse_pct", pr); put("dev_pct", pd)
                put("nome", nome.trim()); put("cnpj", d.ifEmpty { null }); put("razao_social", razao.ifBlank { null }); put("nome_fantasia", fantasia.ifBlank { null })
                put("situacao", situacao.ifBlank { null }); put("telefone", telefone.ifBlank { null }); put("email", email.ifBlank { null })
                put("endereco", endereco.ifBlank { null }); put("lat", lat); put("lng", lng); put("instrucoes_coleta", instrucoes.ifBlank { null })
                put("modalidade_preco", modalidade); put("valor_km", num(valorKm) ?: 2.0); put("valor_fixo", num(valorFixo))
                put("cliente_ve_ao_vivo", aoVivo); put("observacoes", observacoes.ifBlank { null })
            }
            ocupado = true
            escopo.launch {
                Gestao.salvarEmpresa(id, linha, favoritos.toList()).onSuccess { Gestao.atualizar(); msg = "Salvo."; erro = null; if (id == null) onVoltar() }.onFailure { erro = it.message }
                ocupado = false
            }
        })
        if (e != null) {
            PwCartao {
                PwRotulo("Acesso do cliente")
                val acessos = dados.acessosCliente[e.id].orEmpty()
                TextoDica(if (acessos.isEmpty()) "Crie um login para o cliente pedir, pagar e acompanhar pelo site." else "Logins que pedem pelo site em nome deste cliente:")
                acessos.forEach { a -> Detalhe(PwIcones.User, "${a.nome} · ${a.email}") }
                Espaco(8)
                PwBotaoVidro("Criar acesso do cliente", onClick = { criandoAcesso = true }, icone = PwIcones.UserPlus, modifier = Modifier.fillMaxWidth())
            }
            PwBotaoPerigo("Excluir cliente", onClick = { excluir = true }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (criandoAcesso && e != null) NovaContaG(0.0, onFechar = { criandoAcesso = false }, onCriada = { criandoAcesso = false }, cliente = e)
    if (marcarMapa) {
        com.pwexpress.core.mapa.PwSeletorMapa(lat?.let { la -> lng?.let { lo -> la to lo } }, onFechar = { marcarMapa = false }, titulo = "Coleta padrão") { la, lo ->
            marcarMapa = false
            lat = la; lng = lo
            escopo.launch { if (endereco.isBlank()) endereco = Geo.enderecoDoPonto(la, lo) }
        }
    }
    if (excluir && e != null) {
        PwJanela("Excluir ${e.nome}?", onFechar = { excluir = false }) {
            Text("As corridas antigas ficam, mas sem vínculo com o cliente.", color = c.texto2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Voltar", onClick = { excluir = false }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Excluir", cheio = true, modifier = Modifier.weight(1f), onClick = {
                    excluir = false
                    escopo.launch { Gestao.excluirEmpresa(e.id).onSuccess { Gestao.atualizar(); onVoltar() }.onFailure { erro = it.message } }
                })
            }
        }
    }
}

// =====================================================================
// Equipe
// =====================================================================

@Composable
fun EquipeG(onAbrir: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var criando by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TituloTela("Equipe", if (dados.equipe.size == 1) "1 pessoa" else "${dados.equipe.size} pessoas", PwIcones.Users) }
            items(dados.equipe, key = { it.id }) { m ->
                val p = dados.posicao(m.id)
                val forma = RoundedCornerShape(20.dp)
                Row(
                    Modifier.fillMaxWidth().clip(forma).background(c.vidro).border(1.dp, c.borda, forma).clickable { onAbrir(m.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PwAvatar(m.nome, m.foto, tamanho = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (m.motoboy) { Spacer(Modifier.width(6.dp)); PwPonto(corMotoboy(p?.status, c), 9.dp) }
                        }
                        Text(m.email, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Selos(m, p?.status)
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (modulo("criar_contas")) PwBotaoFlutuante(PwIcones.UserPlus, "Nova conta", { criando = true }, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 108.dp))
    }
    if (criando) NovaContaG(dados.config.numero("repasse_motoboy_pct", 67.5), onFechar = { criando = false }, onCriada = { criando = false; it?.let(onAbrir) })
}

/** Papéis e situação da pessoa, em fichas que quebram de linha sem cortar o texto. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Selos(m: com.pwexpress.feature.gestor.data.MembroG, status: String?) {
    val c = Pw.cores
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (m.dev) PwSelo("Dev", c.dev, icone = PwIcones.CodeXml)
        if (m.gestor && !m.dev) PwSelo("Gestor", c.ouro, icone = PwIcones.ShieldCheck)
        if (m.motoboy) PwSelo(rotuloMotoboy[status] ?: "Offline", corMotoboy(status, c), icone = PwIcones.Bike)
        if (m.modoTeste) PwSelo("Teste", c.violeta, icone = PwIcones.FlaskConical)
        if (m.bloqueado) PwSelo("Bloqueado", c.vermelho, icone = PwIcones.Lock)
    }
}

private fun gerarSenha(): String {
    val l = "abcdefghjkmnpqrstuvwxyz"; val n = "23456789"; val r = java.security.SecureRandom()
    fun p() = (1..4).map { l[r.nextInt(l.length)] }.joinToString("")
    return "${p()}-${p()}-${n[r.nextInt(n.length)]}${n[r.nextInt(n.length)]}"
}

private fun copiar(ctx: Context, texto: String) {
    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Acesso PW Express", texto))
}

@Composable
private fun NovaContaG(repassePadrao: Double, onFechar: () -> Unit, onCriada: (String?) -> Unit, cliente: com.pwexpress.feature.gestor.data.EmpresaG? = null) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(cliente?.nome.orEmpty()) }
    var email by remember { mutableStateOf(cliente?.email.orEmpty()) }
    var senha by remember { mutableStateOf(gerarSenha()) }
    var motoboy by remember { mutableStateOf(true) }
    var gestor by remember { mutableStateOf(false) }
    var repasse by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var criada by remember { mutableStateOf<String?>(null) }
    val acesso = "PW Express\nE-mail: ${email.trim().lowercase()}\nSenha: $senha"
    if (criada != null) {
        PwJanela("Conta criada", onFechar = { onCriada(criada) }) {
            Text("Passe estes dados para ${nome.trim().substringBefore(" ")}. A senha não aparece de novo.", color = c.texto2)
            Text(acesso, fontFamily = FontFamily.Monospace, color = c.texto, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.campo).border(1.dp, c.bordaForte, RoundedCornerShape(14.dp)).padding(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Copiar acesso", onClick = { copiar(ctx, acesso) }, icone = PwIcones.Copy, modifier = Modifier.weight(1f))
                PwBotaoOuro("Pronto", onClick = { onCriada(criada) }, modifier = Modifier.weight(1f))
            }
        }
        return
    }
    PwJanela(if (cliente != null) "Acesso de ${cliente.nome}" else "Nova conta", onFechar = onFechar, fecharFora = !enviando) {
        TextoDica(if (cliente != null) "O cliente entra no site da PW Express para pedir, pagar e acompanhar." else "A conta já nasce confirmada e pode entrar no app ou no painel na hora.")
        PwCampo(nome, { nome = it }, "Nome")
        PwCampo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwCampo(senha, { senha = it }, "Senha inicial", modifier = Modifier.weight(1f))
            PwBotaoIcone(PwIcones.RefreshCw, "Gerar outra", { senha = gerarSenha() })
        }
        if (cliente == null) PwInterruptor("Motoboy", motoboy, { motoboy = it }, descricao = "Usa o app e recebe corridas", icone = PwIcones.Bike)
        if (cliente == null) PwInterruptor("Gestor", gestor, { gestor = it }, descricao = "Acessa o painel", icone = PwIcones.ShieldCheck)
        if (motoboy && cliente == null) PwCampo(repasse, { repasse = it }, "Repasse individual (%)", dica = "padrão: ${repassePadrao.toInt()}%", teclado = KeyboardType.Decimal)
        PwErro(erro)
        PwBotaoOuro(if (enviando) "Criando…" else "Criar conta", icone = PwIcones.UserPlus, habilitado = !enviando, onClick = {
            enviando = true
            escopo.launch {
                Gestao.criarConta(
                    nome.trim(), email.trim(), senha,
                    if (cliente != null) listOf("cliente") else listOfNotNull("motoboy".takeIf { motoboy }, "gestor".takeIf { gestor }),
                    repasse.replace(',', '.').toDoubleOrNull().takeIf { cliente == null }, cliente?.id,
                )
                    .onSuccess { criada = it; Gestao.atualizar() }.onFailure { erro = it.message }
                enviando = false
            }
        })
    }
}

@Composable
fun MembroG(id: String, meuId: String?, onVoltar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val m = dados.membro(id)
    var aba by remember { mutableIntStateOf(0) }
    PwPagina {
        Cabecalho(m?.nome ?: "Conta", onVoltar, subtitulo = m?.email)
        if (m == null) { if (!dados.carregado) PwCarregando() else PwErro("Essa conta não existe mais."); return@PwPagina }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwAvatar(m.nome, m.foto, tamanho = 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Selos(m, dados.posicao(m.id)?.status)
                m.ultimoAcesso?.let { TextoDica("Último acesso: ${dataHora(it)}") }
            }
        }
        val comPercursos = m.motoboy && modulo("trilhas_mapa")
        PwSegmentado(listOfNotNull("Registro" to PwIcones.History, ("Percursos" to PwIcones.Route).takeIf { comPercursos }, "Editar" to PwIcones.Pencil), aba, { aba = it })
        val souDev = com.pwexpress.core.perfil.Perfis.perfil.collectAsState().value?.dev == true
        val abaEditar = if (comPercursos) 2 else 1
        if (aba == 0) RegistroG(m.id)
        else if (aba == 1 && comPercursos) PercursosMembroG(m.id, m.modoTeste)
        else if (aba != abaEditar) Unit
        else if (m.dev && !souDev) TextoDica("Conta Dev: só o Dev altera esta conta.")
        else EditarMembroG(m, eu = m.id == meuId, onExcluida = onVoltar, souDev = souDev)
    }
}

/** Percursos das últimas 12 h do motoboy, separados em registros (procurando / corrida). */
@Composable
private fun PercursosMembroG(id: String, teste: Boolean) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val regs = registrosDe(dados, id)
    var destaque by remember { mutableStateOf<Int?>(null) }
    var chave by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { chave = Gestao.chavesPix()[id] }
    val forma = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().height(300.dp).clip(forma).border(1.dp, c.borda, forma)) {
        PwMapa(
            paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(300.dp),
            percursos = regs.mapIndexed { i, r -> com.pwexpress.core.mapa.PercursoMapa(r.pontos.map { it.lat to it.lng }, destaque = destaque == i, apagado = destaque != null && destaque != i) },
            percursoTeste = teste, enquadrar = "$id-${regs.size}-$destaque",
            marcadores = dados.posicao(id)?.takeIf { coordenadaValida(it.lat, it.lng) }?.let { p -> listOf(MarcadorMapa(id, p.lat!!, p.lng!!, String.format("#%06X", 0xFFFFFF and corMotoboy(p.status, c).toArgb()), dados.membro(id)?.nome?.substringBefore(" ").orEmpty(), foto = dados.membro(id)?.foto)) }.orEmpty(),
        )
    }
    LegendaPercurso(teste)
    PwCartao {
        PwRotulo("Registros · últimas 12 h")
        Espaco(6)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { ListaRegistros(regs, destaque, { destaque = if (destaque == it) null else it }, dados) }
        Espaco(8)
        TextoDica("Total: ${String.format(java.util.Locale("pt", "BR"), "%.1f km", regs.sumOf { it.metros } / 1000)} · ${regs.count { it.corrida }} em corrida · ${regs.count { !it.corrida }} procurando")
    }
    PwCartao { PwRotulo("Chave PIX"); Espaco(4); TextoDica(chave ?: "Ainda não cadastrou a chave PIX no MotoApp.") }
}

@Composable
private fun RegistroG(id: String) {
    val c = Pw.cores
    val teste by TemaApp.modoTeste.collectAsState()
    var dados by remember { mutableStateOf<Pair<JsonObject, List<ItemRegistro>>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var filtro by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) { runCatching { Gestao.registro(id) }.onSuccess { dados = it }.onFailure { erro = it.message } }
    PwErro(erro)
    val d = dados ?: run { if (erro == null) PwCarregando(); return }
    val r = d.first
    fun n(k: String) = (r.int(k) ?: 0).toString()
    PwNumero(if (teste) "Ganhos de teste" else "Ganhos", moedaBr(r.dbl(if (teste) "ganhos_teste" else "ganhos") ?: 0.0), Modifier.fillMaxWidth(), icone = PwIcones.Wallet, destaque = true)
    val numeros = listOf(
        Triple("Km rodados", "%.1f".format(BR, r.dbl("km_rodados") ?: 0.0), PwIcones.Ruler),
        Triple("Concluídas", n(if (teste) "concluidas_teste" else "concluidas"), PwIcones.CircleCheck),
        Triple("Criadas", n(if (teste) "criadas_teste" else "criadas"), PwIcones.Plus),
        Triple("Aceitas", n("aceitas"), PwIcones.Check),
        Triple("Recusadas", n("recusadas"), PwIcones.Ban),
        Triple("Expiradas", n("expiradas"), PwIcones.Timer),
        Triple("Canceladas", ((r.int("cancelou") ?: 0) + (r.int("canceladas_com_ele") ?: 0)).toString(), PwIcones.CircleX),
        Triple("Apagadas", n("excluiu"), PwIcones.Trash2),
    )
    numeros.chunked(2).forEach { par ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            par.forEach { (rot, v, ic) -> PwNumero(rot, v, Modifier.weight(1f), icone = ic) }
        }
    }
    val grupos = mapOf(
        "criada" to 1, "reagendada" to 1, "concluida" to 2, "parada_concluida" to 2, "chegou" to 2, "cancelada" to 3,
        "oferta_enviada" to 4, "oferta_recusada" to 4, "oferta_expirada" to 4, "aceita" to 4, "redespachada" to 4, "reservada" to 4, "reserva_cancelada" to 4, "planejada_atrasada" to 4,
        "excluida" to 5,
    )
    PwFiltros(listOf("Tudo" to 0, "Criadas" to 0, "Concluídas" to 0, "Canceladas" to 0, "Recebidas" to 0, "Apagadas" to 0), filtro, { filtro = it })
    val itens = d.second.filter { (teste || !it.teste) && (filtro == 0 || grupos[it.tipo] == filtro) }
    if (itens.isEmpty()) PwVazio("Nada por aqui ainda.", icone = PwIcones.History)
    var diaAnterior = ""
    itens.forEach { i ->
        val dia = dataDia(i.quando)
        if (dia != diaAnterior) { diaAnterior = dia; Espaco(4); PwRotulo(dia) }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(descricaoRegistro(i), color = c.texto, fontSize = 14.sp)
                extraRegistro(i)?.let { TextoDica(it) }
            }
            Spacer(Modifier.width(8.dp))
            Text(hora(i.quando), color = c.texto3, fontSize = 12.sp)
        }
    }
}

private fun dataDia(ms: Long): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val hoje = java.time.LocalDate.now()
    return when (d) { hoje -> "Hoje"; hoje.minusDays(1) -> "Ontem"; else -> d.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd 'de' MMM", BR)) }
}

private fun descricaoRegistro(i: ItemRegistro): String {
    val n = i.numero?.let { if (i.teste) "T$it" else "#$it" }.orEmpty()
    val pelo = (i.dados["pelo_gestor"] as? JsonPrimitive)?.content == "true"
    return when (i.tipo) {
        "criada" -> "Criou a corrida $n"
        "cancelada" -> if (i.fez) "Cancelou a corrida $n" else "A corrida $n foi cancelada pelo gestor"
        "redespachada" -> if (i.fez) "Relançou a corrida $n" else "Recebeu a corrida $n direto do gestor"
        "oferta_enviada" -> "Recebeu a oferta da corrida $n"
        "oferta_recusada" -> "Recusou a corrida $n"
        "oferta_expirada" -> "Deixou a oferta da corrida $n expirar"
        "aceita" -> "Aceitou a corrida $n"
        "chegou" -> "Chegou no local (corrida $n)"
        "parada_concluida" -> "Concluiu uma parada (corrida $n)"
        "concluida" -> if (i.fez) "Concluiu a corrida $n" else "Corrida $n concluída"
        "excluida" -> "Apagou a corrida $n"
        "reservada" -> if (i.fez) (if (pelo) "Reservou a corrida planejada $n para um motoboy" else "Reservou a corrida planejada $n") else "Recebeu a reserva da corrida $n (pelo gestor)"
        "reserva_cancelada" -> if (i.fez && !pelo) "Desistiu da corrida planejada $n" else if (i.fez) "Liberou a reserva da corrida $n" else "A reserva da corrida $n foi liberada pelo gestor"
        "reagendada" -> "Mudou o horário da corrida $n"
        "planejada_atrasada" -> "Não iniciou a corrida planejada $n no horário"
        else -> "${i.tipo.replace('_', ' ')} $n"
    }
}

private fun extraRegistro(i: ItemRegistro): String? {
    val motivo = (i.dados["motivo"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
    val dist = (i.dados["distancia_m"] as? JsonPrimitive)?.content?.toDoubleOrNull()
    return listOfNotNull(
        i.cliente, motivo?.let { "Motivo: $it" }, dist?.let { "${km(it.toInt())} até a coleta" },
        i.ganho?.takeIf { i.tipo == "concluida" }?.let { "Ganhou ${moedaBr(it)}" },
        i.valor?.takeIf { i.tipo == "criada" }?.let { moedaBr(it) },
        "teste".takeIf { i.teste },
    ).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun EditarMembroG(m: com.pwexpress.feature.gestor.data.MembroG, eu: Boolean, onExcluida: () -> Unit, souDev: Boolean = false) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val dados by Gestao.dados.collectAsState()
    var nome by remember(m.id) { mutableStateOf(m.nome) }
    var gestor by remember(m.id) { mutableStateOf(m.gestor) }
    var motoboy by remember(m.id) { mutableStateOf(m.motoboy) }
    var devPapel by remember(m.id) { mutableStateOf(m.dev) }
    var repasse by remember(m.id) { mutableStateOf(m.repassePct?.toString().orEmpty()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var senhaNova by remember { mutableStateOf<String?>(null) }
    var senhaFeita by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf(false) }

    PwCartao {
        PwCampo(nome, { nome = it }, "Nome de exibição")
        Espaco(8)
        PwRotulo("Papéis")
        if (souDev) PwInterruptor("Dev", devPapel, { devPapel = it }, descricao = "Administra o sistema inteiro", icone = PwIcones.CodeXml, cor = c.dev)
        PwInterruptor("Gestor", gestor, { gestor = it }, descricao = "Acessa o painel (site e app)", icone = PwIcones.ShieldCheck)
        PwInterruptor("Motoboy", motoboy, { motoboy = it }, descricao = "Usa o app e recebe corridas", icone = PwIcones.Bike)
        if (motoboy) PwCampo(repasse, { repasse = it }, "Repasse individual (%)", dica = "padrão: ${dados.config.numero("repasse_motoboy_pct", 70.0).toInt()}%", teclado = KeyboardType.Decimal)
        PwErro(erro)
        msg?.let { Text(it, color = c.verde, fontWeight = FontWeight.SemiBold) }
        Espaco(6)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoVidro("Nova senha", onClick = { senhaNova = gerarSenha(); senhaFeita = false }, icone = PwIcones.KeyRound, modifier = Modifier.weight(1f))
            PwBotaoOuro("Salvar", icone = PwIcones.Check, modifier = Modifier.weight(1f), onClick = {
                escopo.launch {
                    Gestao.editarMembro(m.id, nome, listOfNotNull("dev".takeIf { devPapel }, "gestor".takeIf { gestor }, "motoboy".takeIf { motoboy }), repasse.replace(',', '.').toDoubleOrNull())
                        .onSuccess { msg = null; erro = null; com.pwexpress.core.designsystem.PwToast.mostrar("${nome.ifBlank { m.email }} atualizado.", titulo = "Salvo"); Gestao.atualizar() }.onFailure { erro = it.message; msg = null }
                }
            })
        }
    }

    if (eu) { TextoDica("Esta é a sua conta: nome e foto você muda no seu perfil."); return }
    PwCartao(cor = c.vermelho.copy(alpha = 0.45f)) {
        PwRotulo("Zona de perigo")
        Espaco(6)
        PwLinha(if (m.bloqueado) "Acesso bloqueado" else "Bloquear acesso", subtitulo = if (m.bloqueado) "Não entra no app nem no painel. O histórico continua." else "Impede de entrar, sem apagar nada. Dá para liberar depois.",
            icone = if (m.bloqueado) PwIcones.LockOpen else PwIcones.Lock, corIcone = c.vermelho)
        PwBotaoVidro(if (m.bloqueado) "Liberar acesso" else "Bloquear", icone = if (m.bloqueado) PwIcones.LockOpen else PwIcones.Lock, modifier = Modifier.fillMaxWidth(), onClick = {
            escopo.launch { Gestao.bloquear(m.id, !m.bloqueado).onSuccess { Gestao.atualizar(); erro = null }.onFailure { erro = it.message } }
        })
        Espaco(10)
        PwLinha("Excluir conta", subtitulo = "Apaga de vez. As corridas continuam no histórico com o nome da pessoa.", icone = PwIcones.Trash2, corIcone = c.vermelho)
        PwBotaoPerigo("Excluir", onClick = { excluindo = true }, modifier = Modifier.fillMaxWidth())
    }

    senhaNova?.let { s ->
        val acesso = "PW Express\nE-mail: ${m.email}\nSenha: $s"
        PwJanela(if (senhaFeita) "Senha trocada" else "Nova senha", onFechar = { senhaNova = null }) {
            if (!senhaFeita) {
                TextoDica("Para quando ${m.nome} esquecer a senha. Quem estiver conectado continua até sair.")
                Text(s, fontFamily = FontFamily.Monospace, color = c.texto, fontSize = 18.sp)
                PwBotaoOuro("Trocar senha", icone = PwIcones.KeyRound, onClick = {
                    escopo.launch { Gestao.redefinirSenha(m.id, s).onSuccess { senhaFeita = true }.onFailure { erro = it.message; senhaNova = null } }
                })
            } else {
                Text(acesso, fontFamily = FontFamily.Monospace, color = c.texto)
                PwBotaoVidro("Copiar acesso", onClick = { copiar(ctx, acesso) }, icone = PwIcones.Copy, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (excluindo) {
        var senha by remember { mutableStateOf("") }
        var motivo by remember { mutableStateOf("") }
        var entendi by remember { mutableStateOf(false) }
        var errExc by remember { mutableStateOf<String?>(null) }
        var enviando by remember { mutableStateOf(false) }
        PwJanela("Excluir ${m.nome}?", onFechar = { excluindo = false }, fecharFora = !enviando) {
            PwErro("${m.email} deixa de existir: não entra mais no app nem no painel. Não dá para desfazer. As corridas continuam no histórico com o nome guardado.")
            PwCampo(motivo, { motivo = it }, "Motivo (opcional)")
            PwInterruptor("Entendo que é definitivo", entendi, { entendi = it }, cor = c.vermelho)
            PwCampo(senha, { senha = it }, "Sua senha", senha = true, teclado = KeyboardType.Password)
            PwErro(errExc)
            PwBotaoPerigo(if (enviando) "Excluindo…" else "Excluir de vez", cheio = true, habilitado = entendi && senha.isNotEmpty() && !enviando, modifier = Modifier.fillMaxWidth(), onClick = {
                enviando = true
                escopo.launch {
                    Gestao.excluirConta(m.id, senha, motivo.trim().ifEmpty { null }).onSuccess { excluindo = false; Gestao.atualizar(); onExcluida() }.onFailure { errExc = it.message; senha = "" }
                    enviando = false
                }
            })
        }
    }
}

// =====================================================================
// Carteira
// =====================================================================

@Composable
fun FinanceiroG(onVoltar: (() -> Unit)?) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by remember { mutableIntStateOf(2) }
    var linhas by remember { mutableStateOf<List<CorridaG>?>(null) }
    LaunchedEffect(periodo, teste) {
        linhas = null
        val cal = java.util.Calendar.getInstance()
        when (periodo) {
            0 -> { cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0) }
            1 -> cal.add(java.util.Calendar.DAY_OF_MONTH, -7)
            2 -> { cal.set(java.util.Calendar.DAY_OF_MONTH, 1); cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0) }
            else -> cal.add(java.util.Calendar.DAY_OF_MONTH, -30)
        }
        linhas = Gestao.concluidas(cal.timeInMillis, teste)
    }
    PwPagina {
        val sub = if (teste) "Ganhos das corridas de teste" else "Corridas concluídas"
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = sub) else TituloTela("Carteira", sub, PwIcones.Wallet)
        PwFiltros(listOf("Hoje" to 0, "7 dias" to 0, "Este mês" to 0, "30 dias" to 0), periodo, { periodo = it })
        val l = linhas ?: run { PwCarregando(); return@PwPagina }
        val total = l.sumOf { it.valor ?: 0.0 }; val moto = l.sumOf { it.valorMotoboy ?: 0.0 }; val gest = l.sumOf { it.valorGestao ?: 0.0 }; val dev = l.sumOf { it.valorDev ?: 0.0 }
        var chaves by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        LaunchedEffect(Unit) { chaves = Gestao.chavesPix() }
        val ctxC = LocalContext.current
        PwNumero("Faturado · ${l.size} corrida${if (l.size == 1) "" else "s"}", moedaBr(total), Modifier.fillMaxWidth(), icone = PwIcones.TrendingUp, destaque = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwNumero("Gestão", moedaBr(gest), Modifier.weight(1f), icone = PwIcones.Landmark)
            PwNumero("Motoboys", moedaBr(moto), Modifier.weight(1f), icone = PwIcones.Bike)
        }
        PwNumero("Dev", moedaBr(dev), Modifier.fillMaxWidth(), icone = PwIcones.CodeXml)
        if (l.isEmpty()) { PwVazio("Nenhuma corrida concluída neste período.", icone = PwIcones.Wallet); return@PwPagina }
        PwCartao {
            PwRotulo("Por motoboy")
            l.groupBy { it.motoboyId }.entries.sortedByDescending { e -> e.value.sumOf { it.valor ?: 0.0 } }.forEach { (mid, cs) ->
                Espaco(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dados.membro(mid)?.nome ?: cs.first().motoboyNomeArquivado ?: "—", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${cs.size} corrida${if (cs.size == 1) "" else "s"} · faturou ${moedaBr(cs.sumOf { it.valor ?: 0.0 })} · gestão ${moedaBr(cs.sumOf { it.valorGestao ?: 0.0 })}")
                        TextoDica(chaves[mid]?.let { "PIX: $it" } ?: "Sem chave PIX cadastrada")
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(moedaBr(cs.sumOf { it.valorMotoboy ?: 0.0 }), color = c.texto, fontWeight = FontWeight.Bold)
                        TextoDica("a repassar")
                    }
                    chaves[mid]?.let { ch -> PwBotaoIcone(PwIcones.Copy, "Copiar chave PIX", { copiar(ctxC, ch.substringBefore(" ·")); com.pwexpress.core.designsystem.PwToast.mostrar("Chave PIX copiada.") }, tamanho = 38.dp) }
                }
            }
        }
        PwCartao {
            PwRotulo("Por cliente")
            l.groupBy { it.empresaId ?: it.clienteNome?.let { n -> "avulso:$n" } ?: "—" }.entries.sortedByDescending { e -> e.value.sumOf { it.valor ?: 0.0 } }.forEach { (k, cs) ->
                Espaco(8)
                val nome = when { k == "—" -> "Avulso (sem nome)"; k.startsWith("avulso:") -> "${k.removePrefix("avulso:")} (avulso)"; else -> dados.empresa(k)?.nome ?: "—" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(nome, color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); TextoDica("${cs.size} corrida${if (cs.size == 1) "" else "s"} · gestão ${moedaBr(cs.sumOf { it.valorGestao ?: 0.0 })} · Dev ${moedaBr(cs.sumOf { it.valorDev ?: 0.0 })}") }
                    Spacer(Modifier.width(8.dp))
                    Text(moedaBr(cs.sumOf { it.valor ?: 0.0 }), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
        }
        PwCartao {
            PwRotulo("Extrato")
            l.take(150).forEach { k ->
                Espaco(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${k.numeroExibicao} · ${dados.cliente(k)}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${k.concluidaEm?.let { dataHora(it) } ?: ""} · ${dados.nomeMotoboy(k) ?: "—"}")
                        TextoDica("Motoboy ${moedaBr(k.valorMotoboy)} · gestão ${moedaBr(k.valorGestao)} · Dev ${moedaBr(k.valorDev)}")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(moedaBr(k.valor), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
            if (l.size > 150) { Espaco(6); TextoDica("Mostrando as 150 mais recentes. O extrato completo (com planilha) está no site.") }
        }
    }
}

// =====================================================================
// Configurações
// =====================================================================

@Composable
fun ConfiguracoesG(onVoltar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var grupo by remember { mutableIntStateOf(0) }
    var busca by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf<Pair<String, String>?>(null) }
    val escopo = rememberCoroutineScope()
    fun sem(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    fun salvar(p: Param, v: kotlinx.serialization.json.JsonElement) {
        estado = p.chave to "salvando"
        escopo.launch { Gestao.salvarConfig(p.chave, v).onSuccess { estado = p.chave to "salvo" }.onFailure { estado = p.chave to (it.message ?: "erro") } }
    }

    PwPagina {
        Cabecalho("Configurações", onVoltar, subtitulo = "Valem para todo o sistema · salvam sozinhas")
        PwCampo(busca, { busca = it }, "Buscar ajuste…")
        val termo = sem(busca.trim())
        if (termo.isEmpty()) PwFiltros(GRUPOS_CONFIG.map { it.titulo to 0 }, grupo, { grupo = it })
        val g = GRUPOS_CONFIG.getOrNull(grupo)
        val lista = if (termo.isNotEmpty()) PARAMETROS.filter { sem("${it.rotulo} ${it.descricao}").contains(termo) } else PARAMETROS.filter { it.grupo == g?.chave }
        if (termo.isEmpty() && g != null) TextoDica(g.descricao)
        if (g?.chave == "distancia" && termo.isEmpty()) {
            val cfg = dados.config
            fun faixa(kmv: Double, cada: Double, f: Double) = if (cada > 0) Math.floor(kmv / cada) * f else 0.0
            val fc = faixa(250.0, cfg.numero("acrescimo_coleta_cada_km", 100.0), cfg.numero("acrescimo_coleta_fator", 0.5))
            val fe = faixa(450.0, cfg.numero("acrescimo_entrega_cada_km", 200.0), cfg.numero("acrescimo_entrega_fator", 0.25))
            PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
                PwRotulo("Exemplo")
                TextoDica("Corrida de R$ 100,00, motoboy a 250 km da coleta e 450 km de percurso:")
                Text(if (cfg.logico("acrescimo_distancia", true)) "100 × (1 + ${com.pwexpress.core.designsystem.numeroBr(fc)} + ${com.pwexpress.core.designsystem.numeroBr(fe)}) = ${moedaBr(100 * (1 + fc + fe))}" else "Acréscimo desligado: fica R$ 100,00.", color = c.texto, fontWeight = FontWeight.Bold)
            }
        }
        lista.forEach { p ->
            val v = dados.config[p.chave]
            val st = estado?.takeIf { it.first == p.chave }?.second
            PwCartao(padding = PaddingValues(14.dp)) {
                if (p.tipo == "booleano") {
                    val lig = (v as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: p.padraoBool
                    PwInterruptor(p.rotulo, lig, { salvar(p, JsonPrimitive(it)) }, descricao = (if (lig) p.descricao else p.descricaoDesligado ?: p.descricao).ifEmpty { null })
                } else {
                    Text(p.rotulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (p.descricao.isNotEmpty()) TextoDica(p.descricao)
                    Espaco(8)
                    val atual = (v as? JsonPrimitive)?.content?.toDoubleOrNull() ?: p.padraoNum
                    if (p.tipo == "opcoes") {
                        PwSegmentado(p.opcoes.map { it.second to null }, p.opcoes.indexOfFirst { it.first == atual }.coerceAtLeast(0), { i -> salvar(p, JsonPrimitive(p.opcoes[i].first)) })
                    } else {
                        PwPasso(atual, { salvar(p, JsonPrimitive(it)) }, Modifier.fillMaxWidth(), passo = p.passo, min = p.min ?: 0.0, max = p.max ?: 1e9, unidade = p.unidade)
                    }
                }
                when (st) {
                    null, "salvando" -> {}
                    "salvo" -> Text("✓ salvo", color = c.verde, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    else -> PwErro(st)
                }
            }
        }
        if (lista.isEmpty()) PwVazio("Nenhum ajuste encontrado.", icone = PwIcones.Search)
    }
}

@Suppress("unused") private val usado = JsonNull
