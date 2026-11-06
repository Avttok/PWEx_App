package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.TipoParada
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")

/** "hoje às 14:00", "amanhã às 08:30", "sex, 07/11 às 14:00". */
fun quandoTexto(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val hoje = LocalDate.now()
    val dia = when (z.toLocalDate()) {
        hoje -> "hoje"
        hoje.plusDays(1) -> "amanhã"
        else -> z.format(DateTimeFormatter.ofPattern("EEE, dd/MM", br))
    }
    return "$dia às ${z.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

/** "em 2 h 10 min", "em 3 d", "há 5 min". */
fun relativoTexto(ms: Long, agora: Long = System.currentTimeMillis()): String {
    val min = ((ms - agora) / 60_000).toInt()
    val abs = kotlin.math.abs(min)
    val t = when {
        abs < 60 -> "$abs min"
        abs < 1440 -> "${abs / 60} h" + if (abs % 60 > 0) " ${abs % 60} min" else ""
        else -> "${abs / 1440} d" + if ((abs % 1440) / 60 > 0) " ${(abs % 1440) / 60} h" else ""
    }
    return if (min >= 0) "em $t" else "há $t"
}

/** Corridas com hora marcada: as livres para reservar e as minhas reservas. */
@Composable
fun PlanejadasScreen(modifier: Modifier = Modifier, onIniciada: () -> Unit = {}) {
    val c = Pw.cores
    val lista by Corridas.planejadas.collectAsState()
    var aba by remember { mutableIntStateOf(0) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var ocupado by remember { mutableStateOf<String?>(null) }
    var desistindo by remember { mutableStateOf<Corrida?>(null) }
    val escopo = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            Corridas.carregarPlanejadas().onFailure { erro = it.message }.onSuccess { erro = null }
            carregando = false
            repeat(6) { delay(5_000); agora = System.currentTimeMillis() }
        }
    }

    val livres = lista.filter { !it.minha && it.status == "agendada" }
    val minhas = lista.filter { it.minha }
    val mostrar = if (aba == 0) livres else minhas

    fun acao(id: String, bloco: suspend () -> Result<Unit>, aoTerminar: () -> Unit = {}) {
        ocupado = id
        escopo.launch {
            bloco().onFailure { erro = it.message }.onSuccess { erro = null; aoTerminar() }
            ocupado = null
        }
    }

    PwPagina(modifier) {
        PwCabeca(PwIcones.CalendarClock, "Planejadas", "Corridas com hora marcada. Reserve e inicie no horário.")
        PwSegmentado(
            listOf("Livres (${livres.size})" to PwIcones.CalendarClock, "Minhas reservas (${minhas.size})" to PwIcones.CalendarCheck),
            aba, { aba = it },
        )
        PwErro(erro)
        when {
            carregando && lista.isEmpty() -> PwCarregando(texto = "Buscando corridas planejadas…")
            mostrar.isEmpty() -> PwVazio(
                if (aba == 0) "Nenhuma corrida planejada livre agora. Quando o gestor planejar uma, você recebe um aviso."
                else "Você não tem reservas. Reserve uma corrida na aba Livres.",
                icone = PwIcones.CalendarClock,
            )
            else -> mostrar.forEach { corrida ->
                CartaoPlanejada(
                    corrida, agora, ocupado == corrida.id,
                    onReservar = { acao(corrida.id, { Corridas.reservar(corrida.id) }) { aba = 1 } },
                    onIniciar = { acao(corrida.id, { Corridas.iniciarPlanejada(corrida.id) }, onIniciada) },
                    onDesistir = { desistindo = corrida },
                )
            }
        }
    }

    desistindo?.let { cor ->
        var motivo by remember(cor.id) { mutableStateOf("") }
        PwJanela("Desistir da corrida ${cor.numeroExibicao}?", onFechar = { desistindo = null }) {
            Text("Ela volta a ficar livre e os outros motoboys são avisados.", color = c.texto2)
            PwCampo(motivo, { motivo = it }, "Motivo (opcional)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Manter", onClick = { desistindo = null }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Desistir", onClick = {
                    desistindo = null
                    acao(cor.id, { Corridas.cancelarReserva(cor.id, motivo.trim().ifEmpty { null }) })
                }, modifier = Modifier.weight(1f), icone = PwIcones.CalendarX, cheio = true)
            }
        }
    }
}

@Composable
private fun CartaoPlanejada(
    corrida: Corrida, agora: Long, ocupado: Boolean,
    onReservar: () -> Unit, onIniciar: () -> Unit, onDesistir: () -> Unit,
) {
    val c = Pw.cores
    val quando = corrida.agendadaPara ?: 0L
    val podeIniciar = corrida.minha && (corrida.iniciarAPartirDe ?: quando) <= agora
    val podeDesistir = corrida.minha && (corrida.cancelarReservaAte ?: 0L) > agora
    val coleta = corrida.paradas.firstOrNull { it.tipo == TipoParada.COLETA }
    val entregas = corrida.paradas.filter { it.tipo == TipoParada.ENTREGA }

    PwCartao(destaque = corrida.minha, cor = if (corrida.minha) c.azul.copy(alpha = 0.7f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(corrida.numeroExibicao, color = c.texto2, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    if (corrida.minha) PwSelo("reservada para você", c.azul, icone = PwIcones.CalendarCheck)
                    if (corrida.teste) { Spacer(Modifier.width(6.dp)); PwSelo("TESTE", c.violeta, icone = PwIcones.FlaskConical) }
                }
                Spacer(Modifier.height(4.dp))
                Text(quandoTexto(quando).replaceFirstChar { it.uppercase() }, style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 20.sp), color = c.texto)
                Text(relativoTexto(quando, agora), color = c.azul, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                PwValor(moedaBr(corrida.valorMotoboy), tamanhoMax = 26)
                corrida.distanciaM?.let { Text(if (it < 1000) "$it m" else "%.1f km".format(br, it / 1000.0), color = c.texto3, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        val forma = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            coleta?.let { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowUpFromLine, null, tint = c.ouro, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column { Text(p.endereco, color = c.texto, fontSize = 14.sp); ReferenciasParada(p.referencias) }
                }
            }
            entregas.lastOrNull()?.let { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowDownToLine, null, tint = c.texto, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(p.endereco + if (entregas.size > 1) "  (+${entregas.size - 1})" else "", color = c.texto, fontSize = 14.sp)
                        ReferenciasParada(p.referencias)
                    }
                }
            }
            corrida.empresaNome?.let { Text("Cliente: $it", color = c.texto3, fontSize = 12.5.sp) }
        }
        Spacer(Modifier.height(12.dp))
        when {
            !corrida.minha -> PwBotaoOuro(if (ocupado) "Reservando…" else "Reservar", onClick = onReservar, icone = PwIcones.CalendarCheck, habilitado = !ocupado)
            podeIniciar -> PwBotaoOuro(if (ocupado) "Iniciando…" else "Iniciar corrida", onClick = onIniciar, icone = PwIcones.ChevronsRight, habilitado = !ocupado, altura = 60.dp)
            else -> Text(
                "Você pode iniciar ${corrida.iniciarAPartirDe?.let { quandoTexto(it) } ?: "perto do horário"}.",
                color = c.texto2, fontSize = 13.sp,
            )
        }
        if (corrida.minha) {
            Spacer(Modifier.height(8.dp))
            if (podeDesistir) {
                PwBotaoVidro(
                    "Desistir (até ${corrida.cancelarReservaAte?.let { quandoTexto(it).substringAfter(" às ") } ?: "—"})",
                    onClick = onDesistir, icone = PwIcones.CalendarX, habilitado = !ocupado, modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("O prazo para desistir pelo app já passou. Se não puder ir, fale com o gestor.", color = c.texto3, fontSize = 12.5.sp)
            }
        }
    }
}
