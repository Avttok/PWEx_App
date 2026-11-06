package com.pwexpress.feature.tracking.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.BotaoTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwLogo
import com.pwexpress.core.designsystem.PwPonto
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.feature.tracking.Intervalos
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela inicial do motoboy.
 * [acoesTopo] e [extras] recebem conteúdo de outros módulos (avatar, ganhos, ofertas) pelo :app.
 */
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    saudacao: String = "",
    acoesTopo: @Composable RowScope.() -> Unit = {},
    extras: @Composable () -> Unit = {},
) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    var aviso by remember { mutableStateOf<String?>(null) }

    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            agora = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LifecycleResumeEffect(Unit) {
        perms = Permissoes.ler(ctx)
        onPauseOrDispose { }
    }

    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms = Permissoes.ler(ctx) }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        perms = Permissoes.ler(ctx)
        if (!ok) Permissoes.abrirConfiguracoesDoApp(ctx)
    }

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
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------------------------------------------------------- topo
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwLogo(tamanho = 22, modifier = Modifier.weight(1f))
            BotaoTema()
            Spacer(Modifier.width(8.dp))
            acoesTopo()
        }
        if (saudacao.isNotBlank()) {
            Text("Olá, $saudacao", style = MaterialTheme.typography.headlineMedium, color = c.texto)
        }

        // ---------------------------------------------------------- estado
        PwCartao(destaque = info.modo != Modo.OFFLINE) {
            PwRotulo("Seu estado")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwPonto(corDo(info.modo), 14.dp)
                Spacer(Modifier.width(10.dp))
                Text(info.modo.rotulo, style = MaterialTheme.typography.displaySmall, color = c.texto)
            }
            Spacer(Modifier.height(4.dp))
            Text(descricaoDo(info.modo, info.economia), style = MaterialTheme.typography.bodyMedium, color = c.texto2)
            Spacer(Modifier.height(16.dp))
            if (info.modo == Modo.EM_CORRIDA) {
                Text("Conclua a corrida para mudar de estado.", style = MaterialTheme.typography.bodyMedium, color = c.ouro)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotaoModo("Offline", PwIcones.Power, info.modo == Modo.OFFLINE, c.texto3, Modifier.weight(1f)) { escolher(Modo.OFFLINE) }
                    BotaoModo("Disponível", PwIcones.Radio, info.modo == Modo.DISPONIVEL, c.verde, Modifier.weight(1f)) { escolher(Modo.DISPONIVEL) }
                }
            }
            aviso?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = c.vermelho)
            }
        }

        // ---------------------------------------------------------- outros módulos (ganhos, ofertas)
        extras()

        // ---------------------------------------------------------- economia
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.BatteryLow, null, tint = c.ouro, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Economia de bateria", style = MaterialTheme.typography.titleMedium, color = c.texto)
                    Text("Em corrida: posição a cada ${Intervalos.corridaMs(info.economia) / 1000} s", style = MaterialTheme.typography.bodyMedium, color = c.texto2)
                }
                Switch(
                    checked = info.economia,
                    onCheckedChange = { Tracking.definirEconomia(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = c.ouro),
                )
            }
        }

        // ---------------------------------------------------------- telemetria
        PwCartao {
            PwRotulo("Envio de localização")
            Spacer(Modifier.height(6.dp))
            PwLinhaInfo(PwIcones.Crosshair, "Última leitura GPS", info.ultimaLeitura?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
            PwLinhaInfo(PwIcones.LocateFixed, "Precisão", info.ultimaPrecisao?.let { "± ${it.toInt()} m" } ?: "—")
            PwLinhaInfo(PwIcones.Send, "Último envio", info.ultimoEnvio?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
            PwLinhaInfo(PwIcones.Clock, "Na fila do aparelho", if (info.enviando) "${info.naFila} (enviando…)" else info.naFila.toString(), destaque = info.naFila > 0)
            info.erro?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = c.vermelho, style = MaterialTheme.typography.bodyMedium)
            }
            if (info.naFila > 0) {
                Spacer(Modifier.height(8.dp))
                PwBotaoVidro("Enviar agora", onClick = { Tracking.sincronizarAgora() }, icone = PwIcones.RefreshCw)
            }
        }

        // ---------------------------------------------------------- preparação do aparelho
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
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BotaoModo(rotulo: String, icone: ImageVector, selecionado: Boolean, cor: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(56.dp)
            .clip(forma)
            .background(if (selecionado) Brush.linearGradient(listOf(cor.copy(alpha = 0.32f), cor.copy(alpha = 0.10f))) else Brush.linearGradient(listOf(c.vidroSutil, c.vidroSutil)))
            .border(1.dp, if (selecionado) cor else c.borda, forma)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = if (selecionado) cor else c.texto2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(rotulo, color = if (selecionado) c.texto else c.texto2, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Archivo)
    }
}

@Composable
private fun ItemPermissao(nome: String, ok: Boolean, habilitado: Boolean = true, onLiberar: () -> Unit) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) PwIcones.CircleCheck else PwIcones.Alerta, null, tint = if (ok) c.verde else c.vermelho, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(nome, Modifier.weight(1f), color = c.texto)
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

@Composable
private fun corDo(m: Modo): Color = when (m) {
    Modo.OFFLINE -> Pw.cores.texto3
    Modo.DISPONIVEL -> Pw.cores.verde
    Modo.EM_CORRIDA -> Pw.cores.ouro
}

private fun descricaoDo(m: Modo, economia: Boolean): String = when (m) {
    Modo.OFFLINE -> "Fora de serviço. Nenhuma localização é enviada."
    Modo.DISPONIVEL -> "Aguardando corridas. Elas chegam com som e tela cheia."
    Modo.EM_CORRIDA -> "Rastreamento ao vivo a cada ${Intervalos.corridaMs(economia) / 1000} s."
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
