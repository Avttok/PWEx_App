package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoFlutuante
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLinha
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import kotlinx.coroutines.launch

// =====================================================================
// Mapa ao vivo
// =====================================================================

@Composable
fun MapaG(onCorrida: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val erro by Gestao.erro.collectAsState()
    val agora = System.currentTimeMillis()
    val motoboys = dados.equipe.filter { it.motoboy }
    val emServico = motoboys.count { dados.posicao(it.id)?.status?.let { s -> s != "offline" } == true }
    val marcadores = motoboys.mapNotNull { m ->
        val p = dados.posicao(m.id) ?: return@mapNotNull null
        if (p.lat == null || p.lng == null) return@mapNotNull null
        MarcadorMapa(m.id, p.lat, p.lng, String.format("#%06X", 0xFFFFFF and corMotoboy(p.status, c).toArgb()), m.nome.substringBefore(" "))
    }
    val pinos = dados.corridas.filter { it.ativa }.flatMap { cor -> cor.paradas.filter { it.status != "concluida" }.map { PinoMapa(it.lat, it.lng, it.coleta, false, false) } }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(0.5f).padding(horizontal = 12.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, c.borda, RoundedCornerShape(24.dp))) {
            PwMapa(paradas = pinos, rota = null, minhaPosicao = null, marcadores = marcadores, modifier = Modifier.fillMaxSize(), enquadrar = marcadores.size)
        }
        LazyColumn(Modifier.weight(0.5f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TituloTela("Mapa", "$emServico em serviço · ${motoboys.size} motoboy${if (motoboys.size == 1) "" else "s"}", PwIcones.Mapa)
                PwErro(erro)
                if (!dados.carregado) PwCarregando()
            }
            items(motoboys.sortedBy { if (dados.posicao(it.id)?.status == "offline") 1 else 0 }, key = { it.id }) { m ->
                val p = dados.posicao(m.id)
                val corrida = dados.corridas.firstOrNull { it.motoboyId == m.id && it.ativa }
                val forma = RoundedCornerShape(18.dp)
                Column(
                    Modifier.fillMaxWidth().clip(forma).background(c.vidro).border(1.dp, c.borda, forma)
                        .clickable(enabled = corrida != null) { corrida?.let { onCorrida(it.id) } }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PwAvatar(m.nome, m.foto, tamanho = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (m.modoTeste) { PwSelo("teste", c.violeta, icone = PwIcones.FlaskConical); Spacer(Modifier.width(6.dp)) }
                        PwSelo(rotuloMotoboy[p?.status] ?: "Offline", corMotoboy(p?.status, c))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Detalhe(PwIcones.Timer, haQuanto(p?.posicaoEm, agora))
                        p?.precisaoM?.let { Detalhe(PwIcones.Crosshair, "± ${it.toInt()} m") }
                        p?.bateria?.let { Detalhe(PwIcones.BatteryMedium, "$it%") }
                    }
                    corrida?.let { Detalhe(PwIcones.Route, "Corrida ${it.numeroExibicao} · ${Status.rotulo[it.status]} · toque para abrir") }
                }
            }
        }
    }
}

// =====================================================================
// Corridas
// =====================================================================

@Composable
fun CorridasG(onAbrir: (String) -> Unit, onNova: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val erro by Gestao.erro.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var selecionando by remember { mutableStateOf(false) }
    val marcadas = remember { mutableStateListOf<String>() }
    var excluindo by remember { mutableStateOf(false) }

    val todas = dados.corridas.filter { it.teste == teste }
    val planejadas = todas.filter { it.planejada }.sortedBy { it.agendadaPara ?: Long.MAX_VALUE }
    val listas = listOf(
        todas.filter { it.ativa }, planejadas, todas.filter { it.status == Status.CONCLUIDA }, todas.filter { it.status == Status.CANCELADA }, todas,
    )
    val lista = listas[filtro]
    val ativas = listas[0].size

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TituloTela("Corridas", "${if (ativas == 0) "Nenhuma ativa" else if (ativas == 1) "1 ativa" else "$ativas ativas"} agora${if (teste) " · modo teste" else ""}", PwIcones.Route)
                Espaco(8)
                PwFiltros(listOf("Ativas" to listas[0].size, "Planejadas" to planejadas.size, "Concluídas" to 0, "Canceladas" to 0, "Todas" to 0), filtro, { filtro = it })
                Espaco(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextoDica(if (selecionando) "${marcadas.size} selecionada${if (marcadas.size == 1) "" else "s"}" else "${lista.size} corrida${if (lista.size == 1) "" else "s"} · últimos 30 dias")
                    Spacer(Modifier.weight(1f))
                    PwBotaoVidro(if (selecionando) "Cancelar" else "Selecionar", onClick = { selecionando = !selecionando; marcadas.clear() }, icone = if (selecionando) PwIcones.X else PwIcones.CircleCheck)
                }
                PwErro(erro)
                if (!dados.carregado) PwCarregando()
                else if (lista.isEmpty()) PwVazio(
                    if (filtro == 1) "Nenhuma corrida planejada. Em “Nova corrida”, escolha “Planejar” para marcar data e hora." else "Nenhuma corrida aqui.",
                    icone = if (filtro == 1) PwIcones.CalendarClock else PwIcones.Route,
                )
            }
            items(lista, key = { it.id }) { cor ->
                CartaoCorridaG(cor, dados, marcado = if (selecionando) cor.id in marcadas else null, onClick = {
                    if (selecionando) { if (cor.id in marcadas) marcadas.remove(cor.id) else marcadas.add(cor.id) } else onAbrir(cor.id)
                })
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 108.dp), horizontalAlignment = Alignment.End) {
            if (selecionando) {
                com.pwexpress.core.designsystem.PwBotaoPerigo(
                    "Apagar ${marcadas.size}", onClick = { excluindo = true }, habilitado = marcadas.isNotEmpty(), cheio = true,
                )
            } else PwBotaoFlutuante(PwIcones.Plus, "Nova corrida", onNova)
        }
    }
    if (excluindo) {
        DialogoExcluirCorridas(dados.corridas.filter { it.id in marcadas }, onFechar = { excluindo = false }, onExcluidas = { excluindo = false; selecionando = false; marcadas.clear() })
    }
}

// =====================================================================
// Mais (financeiro, configurações e conta)
// =====================================================================

@Composable
fun MaisG(onFinanceiro: () -> Unit, onConfig: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    PwPagina {
        TituloTela("Mais", "Financeiro, configurações e a sua conta", PwIcones.Menu)
        PwCartao(padding = PaddingValues(8.dp)) {
            PwLinha("Financeiro", subtitulo = "Faturado, repasse e gestão por período", icone = PwIcones.Wallet, onClick = onFinanceiro)
            PwLinha("Configurações", subtitulo = "Despacho, preço, planejadas, GPS e mais", icone = PwIcones.Settings2, onClick = onConfig)
        }
        conteudo()
    }
}
