package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TextoAjustado
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.corridas.data.CorridaFeita
import com.pwexpress.feature.corridas.data.Corridas
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")
private val zona = ZoneId.systemDefault()
private fun dia(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()
internal fun kmTexto(m: Int?): String = when { m == null -> "—"; m < 1000 -> "$m m"; else -> "%.1f km".format(br, m / 1000.0) }
internal fun dataCurta(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(zona).format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")) } ?: "—"

/**
 * Carteira do motoboy: quanto ele ganhou, por período, com gráfico e extrato.
 * Mostra só o valor que é dele (nunca percentuais nem o que o cliente pagou).
 */
@Composable
fun CarteiraScreen(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val ganhos by Perfis.ganhos.collectAsState()
    val historico by Corridas.historico.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by rememberSaveable { mutableIntStateOf(1) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(teste) {
        Perfis.carregar()
        Corridas.carregarHistorico().onFailure { erro = it.message }.onSuccess { erro = null }
    }

    val hoje = LocalDate.now()
    val inicio = when (periodo) {
        0 -> hoje
        1 -> hoje.minusDays((hoje.dayOfWeek.value - 1).toLong())
        2 -> hoje.withDayOfMonth(1)
        else -> hoje.minusDays(29)
    }
    val feitas = historico.orEmpty().filter { it.concluida && it.concluidaEm != null }
    val noPeriodo = feitas.filter { !dia(it.concluidaEm!!).isBefore(inicio) }
    val total = noPeriodo.sumOf { it.valor ?: 0.0 }
    val km = noPeriodo.sumOf { it.distanciaM ?: 0 }
    val canceladas = historico.orEmpty().count { !it.concluida && it.quando != null && !dia(it.quando).isBefore(inicio) }

    PwPagina(modifier) {
        PwCabeca(PwIcones.Wallet, "Carteira", if (teste) "Ganhos das corridas de teste" else "Seus ganhos")
        PwErro(erro)

        // ---------------------------------------------------- destaque do período
        PwCartao(destaque = true) {
            PwFiltros(listOf("Hoje" to 0, "Semana" to 0, "Mês" to 0, "30 dias" to 0), periodo, { periodo = it })
            Spacer(Modifier.height(14.dp))
            PwRotulo(listOf("Hoje", "Nesta semana", "Neste mês", "Últimos 30 dias")[periodo])
            PwValor(moedaBr(total), Modifier.fillMaxWidth(), tamanhoMax = 44)
            Text(
                "${noPeriodo.size} corrida${if (noPeriodo.size == 1) "" else "s"}" + (if (canceladas > 0) " · $canceladas cancelada${if (canceladas == 1) "" else "s"}" else ""),
                color = c.texto2, fontFamily = Archivo, fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Média", if (noPeriodo.isEmpty()) "—" else moedaBr(total / noPeriodo.size), Modifier.weight(1f), icone = PwIcones.TrendingUp)
                PwNumero("Rodados", kmTexto(km), Modifier.weight(1f), icone = PwIcones.Ruler)
            }
        }

        // ---------------------------------------------------- gráfico dos últimos 7 dias
        PwCartao {
            PwRotulo("Últimos 7 dias")
            Spacer(Modifier.height(10.dp))
            val dias = (6 downTo 0).map { hoje.minusDays(it.toLong()) }
            val valores = dias.map { d -> feitas.filter { dia(it.concluidaEm!!) == d }.sumOf { it.valor ?: 0.0 } }
            GraficoBarras(valores, dias.map { it.format(DateTimeFormatter.ofPattern("EEE", br)).take(3).replaceFirstChar { ch -> ch.uppercase() } }, destaque = 6)
        }

        // ---------------------------------------------------- resumo (do servidor)
        ganhos?.let { g ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Semana · ${g.corridasSemana}", moedaBr(g.semana), Modifier.weight(1f), icone = PwIcones.CalendarClock)
                PwNumero("Mês · ${g.corridasMes}", moedaBr(g.mes), Modifier.weight(1f), icone = PwIcones.CalendarCheck)
            }
        }

        // ---------------------------------------------------- extrato
        PwRotulo("Extrato")
        when {
            historico == null && erro == null -> PwCarregando()
            noPeriodo.isEmpty() -> PwVazio("Nenhuma corrida concluída neste período.", icone = PwIcones.Wallet)
            else -> noPeriodo.forEach { LinhaExtrato(it) }
        }
        Text(
            "Recebimentos direto na sua chave PIX chegam em breve.",
            color = c.texto3, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun LinhaExtrato(f: CorridaFeita) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.verde.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(PwIcones.ArrowDownToLine, null, tint = c.verde, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Corrida ${f.numeroExibicao}" + (f.empresa?.let { " · $it" } ?: ""), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${dataCurta(f.concluidaEm)} · ${kmTexto(f.distanciaM)}", color = c.texto3, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        TextoAjustado("+ ${moedaBr(f.valor)}", TextStyle(color = c.verde, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold), 15, Modifier.width(110.dp), minimo = 11)
    }
}

/** Barras simples (valor por dia), a de [destaque] na cor da marca. */
@Composable
private fun GraficoBarras(valores: List<Double>, rotulos: List<String>, destaque: Int) {
    val c = Pw.cores
    val max = (valores.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = valores.size
            val passo = size.width / n
            val larg = passo * 0.56f
            valores.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * (size.height - 6f)
                val x = i * passo + (passo - larg) / 2f
                drawRoundRect(c.borda, Offset(x, 0f), Size(larg, size.height), CornerRadius(10f, 10f))
                if (h > 0f) drawRoundRect(if (i == destaque) c.ouro else c.ouroEscuro.copy(alpha = 0.8f), Offset(x, size.height - h), Size(larg, h), CornerRadius(10f, 10f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            rotulos.forEachIndexed { i, r ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(r, color = if (i == destaque) c.ouro else c.texto3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(if (valores[i] > 0) "%.0f".format(br, valores[i]) else "—", color = c.texto2, fontSize = 10.5.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Cartão de uma corrida do histórico (aba Corridas → Histórico). */
@Composable
internal fun CartaoFeita(f: CorridaFeita) {
    val c = Pw.cores
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Corrida ${f.numeroExibicao}", color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            com.pwexpress.core.designsystem.PwSelo(if (f.concluida) "Concluída" else "Cancelada", if (f.concluida) c.verde else c.texto3)
            Spacer(Modifier.weight(1f))
            if (f.concluida) TextoAjustado(moedaBr(f.valor), TextStyle(color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold), 15, Modifier.width(110.dp), minimo = 11)
        }
        Spacer(Modifier.height(6.dp))
        f.coleta?.let { Endereco(PwIcones.ArrowUpFromLine, it, c.ouro) }
        f.entrega?.let { Endereco(PwIcones.ArrowDownToLine, it + if (f.paradas > 2) "  (+${f.paradas - 2})" else "", c.texto) }
        Spacer(Modifier.height(4.dp))
        Text(listOfNotNull(dataCurta(f.quando), kmTexto(f.distanciaM), f.empresa).joinToString(" · "), color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Endereco(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String, cor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = Pw.cores.texto, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
