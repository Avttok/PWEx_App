package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPaleta
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val BR: Locale = Locale.forLanguageTag("pt-BR")

internal fun corStatus(s: String, c: PwPaleta): Color = when (s) {
    Status.AGENDADA, Status.RESERVADA -> c.azul
    Status.AGUARDANDO -> c.ouroClaro
    Status.SEM_MOTOBOY -> c.vermelho
    Status.ACEITA, Status.EM_ANDAMENTO -> c.ouro
    Status.CONCLUIDA -> c.verde
    else -> c.texto3
}

internal fun corMotoboy(s: String?, c: PwPaleta): Color = when (s) {
    "disponivel" -> c.verde
    "em_corrida" -> c.ouro
    else -> c.texto3
}

internal val rotuloMotoboy = mapOf("disponivel" to "Disponível", "em_corrida" to "Em corrida", "offline" to "Offline")

internal fun hora(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
internal fun dataHora(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) } ?: "—"
internal fun km(m: Int?): String = when { m == null -> "—"; m < 1000 -> "$m m"; else -> "%.1f km".format(BR, m / 1000.0) }
internal fun duracao(s: Int?): String = when { s == null -> "—"; s < 3600 -> "${s / 60} min"; else -> "${s / 3600} h ${(s % 3600) / 60} min" }

internal fun quando(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val hoje = LocalDate.now()
    val dia = when (z.toLocalDate()) { hoje -> "hoje"; hoje.plusDays(1) -> "amanhã"; else -> z.format(DateTimeFormatter.ofPattern("EEE, dd/MM", BR)) }
    return "$dia às ${z.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

internal fun relativo(ms: Long, agora: Long = System.currentTimeMillis()): String {
    val min = ((ms - agora) / 60_000).toInt()
    val a = kotlin.math.abs(min)
    val t = when { a < 60 -> "$a min"; a < 1440 -> "${a / 60} h${if (a % 60 > 0) " ${a % 60} min" else ""}"; else -> "${a / 1440} d${if ((a % 1440) / 60 > 0) " ${(a % 1440) / 60} h" else ""}" }
    return if (min >= 0) "em $t" else "há $t"
}

internal fun haQuanto(ms: Long?, agora: Long = System.currentTimeMillis()): String {
    if (ms == null) return "—"
    val s = ((agora - ms) / 1000).coerceAtLeast(0)
    return when { s < 60 -> "agora"; s < 3600 -> "há ${s / 60} min"; s < 86400 -> "há ${s / 3600} h"; else -> "há ${s / 86400} d" }
}

/** Cartão de corrida das listas (mesmo desenho do site). */
@Composable
internal fun CartaoCorridaG(corrida: CorridaG, dados: Gestao.Dados, onClick: () -> Unit, marcado: Boolean? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(18.dp)
    val coleta = corrida.paradas.firstOrNull()
    val entregas = corrida.paradas.filter { !it.coleta }
    val oferta = dados.ofertas.firstOrNull { it.corridaId == corrida.id }
    Column(
        Modifier.fillMaxWidth().clip(forma).background(c.vidro)
            .border(1.dp, if (marcado == true) c.ouro else c.borda, forma)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (marcado != null) {
                Icon(if (marcado) PwIcones.CircleCheck else PwIcones.Package, null, tint = if (marcado) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(corrida.numeroExibicao, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 17.sp, color = c.texto)
            Spacer(Modifier.width(8.dp))
            PwSelo(Status.rotulo[corrida.status] ?: corrida.status, corStatus(corrida.status, c))
            if (corrida.teste) { Spacer(Modifier.width(6.dp)); PwSelo("teste", c.violeta, icone = PwIcones.FlaskConical) }
            Spacer(Modifier.weight(1f))
            Text(moedaBr(corrida.valor), fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold, color = c.texto, fontSize = 15.sp)
        }
        if (corrida.planejada && corrida.agendadaPara != null) {
            val f2 = RoundedCornerShape(10.dp)
            Row(
                Modifier.fillMaxWidth().clip(f2).background((if (corrida.atrasada) c.vermelho else c.azul).copy(alpha = 0.10f))
                    .border(1.dp, (if (corrida.atrasada) c.vermelho else c.azul).copy(alpha = 0.3f), f2).padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (corrida.atrasada) PwIcones.Alerta else PwIcones.CalendarClock, null, tint = if (corrida.atrasada) c.vermelho else c.azul, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(quando(corrida.agendadaPara), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("  " + if (corrida.atrasada) "atrasada" else relativo(corrida.agendadaPara), color = c.texto2, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    if (corrida.motoboyId != null) "Reservada · ${dados.membro(corrida.motoboyId)?.nome ?: "—"}" else "Livre",
                    color = if (corrida.motoboyId != null) c.texto else c.azul, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        coleta?.let { LinhaEndereco(PwIcones.ArrowUpFromLine, it.endereco, c.ouro) }
        entregas.lastOrNull()?.let { LinhaEndereco(PwIcones.ArrowDownToLine, it.endereco + if (entregas.size > 1) " (+${entregas.size - 1})" else "", c.texto) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Detalhe(PwIcones.Building2, dados.cliente(corrida))
            Detalhe(PwIcones.Clock, hora(corrida.criadaEm))
            Detalhe(PwIcones.Ruler, km(corrida.distanciaPlanejadaM))
        }
        if (!corrida.planejada) {
            dados.nomeMotoboy(corrida)?.let { Detalhe(PwIcones.Bike, it) }
            oferta?.let { Detalhe(PwIcones.BellRing, "Oferecida a ${dados.membro(it.motoboyId)?.nome ?: "motoboy"}") }
        }
    }
}

@Composable
internal fun LinhaEndereco(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String, cor: Color) {
    val c = Pw.cores
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = c.texto, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun Detalhe(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    val c = Pw.cores
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = c.texto3, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun TituloTela(titulo: String, subtitulo: String? = null, icone: androidx.compose.ui.graphics.vector.ImageVector) {
    com.pwexpress.core.designsystem.PwCabeca(icone, titulo, subtitulo)
}

@Composable
internal fun Espaco(h: Int = 4) = Spacer(Modifier.height(h.dp))

@Composable
internal fun TextoDica(texto: String) {
    Text(texto, color = Pw.cores.texto3, fontSize = 12.5.sp, lineHeight = 17.sp, fontFamily = Archivo)
}
