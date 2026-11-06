package com.pwexpress.feature.tracking.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.feature.tracking.Intervalos
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Início do MotoApp: saudação, o botão redondo de disponível/offline e os cartões que
 * vêm de outros módulos pelo :app (ganhos…). Detalhes técnicos ficam em Configurações.
 * [chips] mostra atalhos curtos (raio…); [onConfiguracoes] abre a tela de ajustes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    saudacao: String = "",
    extras: @Composable () -> Unit = {},
    chips: @Composable () -> Unit = {},
    onConfiguracoes: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var confirmarOffline by remember { mutableStateOf(false) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { agora = System.currentTimeMillis(); delay(1_000) } }
    LifecycleResumeEffect(Unit) {
        perms = Permissoes.ler(ctx)
        onPauseOrDispose { }
    }
    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms = Permissoes.ler(ctx) }

    fun escolher(m: Modo) {
        if (m != Modo.OFFLINE && !perms.basicasOk) {
            aviso = "Libere a localização precisa e as notificações para ficar disponível."
            pedirBasicas.launch(Permissoes.basicas())
            return
        }
        aviso = null
        Tracking.definirModo(m)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (saudacao.isNotBlank()) {
            Column {
                Text("Olá, $saudacao", style = MaterialTheme.typography.headlineMedium, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (info.modo) {
                        Modo.OFFLINE -> "Fora de serviço: você não recebe corridas."
                        Modo.DISPONIVEL -> "Disponível: as corridas chegam aqui."
                        Modo.EM_CORRIDA -> "Corrida em andamento."
                    },
                    color = c.texto2, fontSize = 14.sp, fontFamily = Archivo,
                )
            }
        }

        // ---------------------------------------------------------- o botão
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            BotaoStatus(info.modo, agora - (info.ultimaLeitura ?: agora), info.economia) {
                when (info.modo) {
                    Modo.OFFLINE -> escolher(Modo.DISPONIVEL)
                    Modo.DISPONIVEL -> confirmarOffline = true
                    Modo.EM_CORRIDA -> aviso = "Conclua a corrida para mudar o status."
                }
            }
        }

        // atalhos (GPS, raio, economia, aparelho)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (info.modo != Modo.OFFLINE) {
                Chip(PwIcones.Crosshair, info.ultimaPrecisao?.let { "GPS ± ${it.toInt()} m" } ?: "Buscando GPS…", if (info.ultimaPrecisao != null) c.verde else c.texto3)
            }
            if (info.economia) Chip(PwIcones.BatteryLow, "Economia", c.ouro)
            chips()
            if (!perms.tudoOk) Chip(PwIcones.Alerta, "Preparar o aparelho", c.vermelho, onConfiguracoes)
            if (info.naFila > 0) Chip(PwIcones.Clock, "${info.naFila} na fila", c.ouro, onConfiguracoes)
        }
        aviso?.let { Text(it, color = c.vermelho, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }

        extras()
    }

    if (confirmarOffline) {
        PwJanela("Ficar offline?", onFechar = { confirmarOffline = false }) {
            Text("Você para de receber corridas até ficar disponível de novo.", color = c.texto2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Continuar", onClick = { confirmarOffline = false }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Ficar offline", onClick = { confirmarOffline = false; escolher(Modo.OFFLINE) }, icone = PwIcones.Power, cheio = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Botão grande e redondo: cinza (offline), verde pulsando (disponível) ou na cor da marca (em corrida). */
@Composable
private fun BotaoStatus(modo: Modo, semLeituraMs: Long, economia: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    val cor by animateColorAsState(
        when (modo) { Modo.OFFLINE -> c.texto3; Modo.DISPONIVEL -> c.verde; Modo.EM_CORRIDA -> c.ouro }, label = "cor-status",
    )
    val pulso = rememberInfiniteTransition(label = "pulso")
    val onda by pulso.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "onda")
    val toque = remember { MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val tamanho = 212.dp
    Box(Modifier.size(tamanho + 56.dp), contentAlignment = Alignment.Center) {
        // ondas enquanto está ligado
        if (modo != Modo.OFFLINE) {
            Box(Modifier.size(tamanho + 56.dp).drawBehind {
                listOf(onda, (onda + 0.5f) % 1f).forEach { t ->
                    drawCircle(cor.copy(alpha = (1f - t) * 0.35f), radius = size.minDimension / 2f * (0.78f + 0.22f * t), style = Stroke(width = 3.dp.toPx()))
                }
            })
        }
        Box(
            Modifier.size(tamanho).scale(if (apertado) 0.96f else 1f).clip(CircleShape)
                .background(
                    if (modo == Modo.OFFLINE) Brush.radialGradient(listOf(c.vidroForte, c.fundo2))
                    else Brush.radialGradient(listOf(cor.copy(alpha = 0.42f), cor.copy(alpha = 0.10f))),
                )
                .border(3.dp, if (modo == Modo.OFFLINE) c.bordaForte else cor, CircleShape)
                .clickable(interactionSource = toque, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    when (modo) { Modo.OFFLINE -> PwIcones.Power; Modo.DISPONIVEL -> PwIcones.Radio; Modo.EM_CORRIDA -> PwIcones.Route },
                    null, tint = if (modo == Modo.OFFLINE) c.texto2 else cor, modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    when (modo) { Modo.OFFLINE -> "OFFLINE"; Modo.DISPONIVEL -> "DISPONÍVEL"; Modo.EM_CORRIDA -> "EM CORRIDA" },
                    style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp),
                    color = c.texto, maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when (modo) {
                        Modo.OFFLINE -> "Toque para ficar disponível"
                        Modo.DISPONIVEL -> if (semLeituraMs > 5 * 60_000) "Sem GPS há ${semLeituraMs / 60_000} min" else "Toque para ficar offline"
                        Modo.EM_CORRIDA -> "Ao vivo a cada ${Intervalos.corridaMs(economia) / 1000} s"
                    },
                    color = c.texto2, fontSize = 12.5.sp, fontFamily = Archivo, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Chip(icone: ImageVector, texto: String, cor: Color, onClick: (() -> Unit)? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Row(
        Modifier.clip(forma).background(cor.copy(alpha = 0.12f)).border(1.dp, cor.copy(alpha = 0.35f), forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1, softWrap = false)
    }
}

/** Atalho do Início em forma de ficha (o :app usa para o raio). */
@Composable
fun ChipInicio(icone: ImageVector, texto: String, onClick: (() -> Unit)? = null) = Chip(icone, texto, Pw.cores.ouro, onClick)

// =====================================================================
// Painéis da tela Configurações do MotoApp
// =====================================================================

/** Envio de localização: últimas leituras, fila e erro. */
@Composable
fun PainelEnvio() {
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { agora = System.currentTimeMillis(); delay(1_000) } }
    PwCartao {
        PwRotulo("Envio de localização")
        Spacer(Modifier.height(6.dp))
        PwLinhaInfo(PwIcones.Radio, "Status", info.modo.rotulo)
        PwLinhaInfo(PwIcones.Crosshair, "Última leitura GPS", info.ultimaLeitura?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
        PwLinhaInfo(PwIcones.LocateFixed, "Precisão", info.ultimaPrecisao?.let { "± ${it.toInt()} m" } ?: "—")
        PwLinhaInfo(PwIcones.Send, "Último envio", info.ultimoEnvio?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
        PwLinhaInfo(PwIcones.Clock, "Na fila do aparelho", if (info.enviando) "${info.naFila} (enviando…)" else info.naFila.toString(), destaque = info.naFila > 0)
        PwLinhaInfo(PwIcones.Timer, "Intervalo em corrida", "${Intervalos.corridaMs(info.economia) / 1000} s")
        info.erro?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = c.vermelho, style = MaterialTheme.typography.bodyMedium)
        }
        if (info.naFila > 0) {
            Spacer(Modifier.height(8.dp))
            PwBotaoVidro("Enviar agora", onClick = { Tracking.sincronizarAgora() }, icone = PwIcones.RefreshCw)
        }
    }
}

/** Aparelho pronto: permissões que o GPS precisa para não ser pausado. */
@Composable
fun PainelAparelho() {
    val ctx = LocalContext.current
    val c = Pw.cores
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    LifecycleResumeEffect(Unit) {
        perms = Permissoes.ler(ctx)
        onPauseOrDispose { }
    }
    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms = Permissoes.ler(ctx) }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        perms = Permissoes.ler(ctx)
        if (!ok) Permissoes.abrirConfiguracoesDoApp(ctx)
    }
    PwCartao(destaque = !perms.tudoOk) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.ShieldCheck, null, tint = if (perms.tudoOk) c.verde else c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            PwRotulo(if (perms.tudoOk) "Aparelho pronto" else "Preparar o aparelho")
        }
        Spacer(Modifier.height(8.dp))
        ItemPermissao("Localização precisa", perms.localizacao) { pedirBasicas.launch(Permissoes.basicas()) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ItemPermissao("Notificações", perms.notificacoes) { pedirBasicas.launch(Permissoes.basicas()) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ItemPermissao("Localização \"o tempo todo\"", perms.segundoPlano, habilitado = perms.localizacao) {
                pedirSegundoPlano.launch(Permissoes.SEGUNDO_PLANO)
            }
        }
        ItemPermissao("Sem restrição de bateria", perms.semRestricaoBateria) { Permissoes.pedirSemRestricaoBateria(ctx) }
        if (!perms.tudoOk) {
            Spacer(Modifier.height(6.dp))
            Text("Sem esses ajustes o Android pode pausar o GPS com a tela apagada.", style = MaterialTheme.typography.bodyMedium, color = c.texto2)
        }
    }
}

@Composable
private fun ItemPermissao(nome: String, ok: Boolean, habilitado: Boolean = true, onLiberar: () -> Unit) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) PwIcones.CircleCheck else PwIcones.Alerta, null, tint = if (ok) c.verde else c.vermelho, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(nome, Modifier.weight(1f), color = c.texto, maxLines = 2)
        if (!ok) {
            Text(
                "Liberar",
                color = if (habilitado) c.ouro else c.texto3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = habilitado, onClick = onLiberar).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("pt-BR"))
private fun hora(t: Long): String = formatoHora.format(Date(t))

private fun ha(agora: Long, t: Long): String {
    val s = ((agora - t) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "há ${s}s"
        s < 3600 -> "há ${s / 60} min"
        else -> "há ${s / 3600} h"
    }
}
