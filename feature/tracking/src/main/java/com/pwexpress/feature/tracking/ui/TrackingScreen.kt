package com.pwexpress.feature.tracking.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCores
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

@Composable
fun TrackingScreen(email: String, onSair: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val info by Tracking.info.collectAsState()
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // relógio para os "há X s"
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

    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        perms = Permissoes.ler(ctx)
    }
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
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------------------------------------------------------- topo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PwLogo(tamanho = 26)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onSair) { Text("Sair", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // ---------------------------------------------------------- estado atual
        PwCartao(destaque = info.modo != Modo.OFFLINE) {
            PwRotulo("Seu estado")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwPonto(corDo(info.modo), 14.dp)
                Spacer(Modifier.width(10.dp))
                Text(info.modo.rotulo, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                descricaoDo(info.modo, info.economia),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Modo.entries.forEach { m ->
                    BotaoModo(m, selecionado = info.modo == m, modifier = Modifier.weight(1f)) { escolher(m) }
                }
            }
            if (info.modo == Modo.EM_CORRIDA) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Piloto: \"Em corrida\" é ligado manualmente. Na próxima fase, ele liga sozinho ao aceitar uma corrida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            aviso?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        // ---------------------------------------------------------- economia
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Economia de bateria", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Em corrida: posição a cada ${Intervalos.corridaMs(info.economia) / 1000} s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = info.economia,
                    onCheckedChange = { Tracking.definirEconomia(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PwCores.Preto, checkedTrackColor = PwCores.Ouro),
                )
            }
        }

        // ---------------------------------------------------------- telemetria
        PwCartao {
            PwRotulo("Envio de localização")
            Spacer(Modifier.height(10.dp))
            Linha("Última leitura GPS", info.ultimaLeitura?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
            Linha("Precisão", info.ultimaPrecisao?.let { "± ${it.toInt()} m" } ?: "—")
            Linha("Último envio", info.ultimoEnvio?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
            Linha(
                "Na fila do aparelho",
                if (info.enviando) "${info.naFila} (enviando…)" else info.naFila.toString(),
                destaque = info.naFila > 0,
            )
            info.erro?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (info.naFila > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { Tracking.sincronizarAgora() }) { Text("Enviar agora") }
            }
        }

        // ---------------------------------------------------------- preparação do aparelho
        PwCartao(destaque = !perms.tudoOk) {
            PwRotulo(if (perms.tudoOk) "Aparelho pronto" else "Preparar o aparelho")
            Spacer(Modifier.height(10.dp))
            ItemPermissao("Localização precisa", perms.localizacao) { pedirBasicas.launch(Permissoes.basicas()) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ItemPermissao("Notificações", perms.notificacoes) { pedirBasicas.launch(Permissoes.basicas()) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ItemPermissao("Localização \"o tempo todo\"", perms.segundoPlano, habilitado = perms.localizacao) {
                    pedirSegundoPlano.launch(Permissoes.SEGUNDO_PLANO)
                }
            }
            ItemPermissao("Sem restrição de bateria", perms.semRestricaoBateria) {
                Permissoes.pedirSemRestricaoBateria(ctx)
            }
            if (!perms.tudoOk) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sem esses ajustes o Android pode pausar o GPS com a tela apagada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BotaoModo(m: Modo, selecionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    if (selecionado) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = forma,
            colors = ButtonDefaults.buttonColors(containerColor = corDo(m), contentColor = PwCores.Preto),
        ) { Text(m.rotulo, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = forma,
            border = BorderStroke(1.dp, PwCores.Borda),
        ) { Text(m.rotulo, color = PwCores.Branco, maxLines = 1) }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String, destaque: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(rotulo, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.SemiBold, color = if (destaque) PwCores.Ouro else PwCores.Branco)
    }
}

@Composable
private fun ItemPermissao(nome: String, ok: Boolean, habilitado: Boolean = true, onLiberar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        PwPonto(if (ok) PwCores.Verde else PwCores.Vermelho, 10.dp)
        Spacer(Modifier.width(10.dp))
        Text(nome, Modifier.weight(1f))
        if (!ok) {
            TextButton(onClick = onLiberar, enabled = habilitado) { Text("Liberar", color = PwCores.Ouro) }
        } else {
            Text("OK", color = PwCores.Verde, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
        }
    }
}

private fun corDo(m: Modo): Color = when (m) {
    Modo.OFFLINE -> PwCores.Cinza
    Modo.DISPONIVEL -> PwCores.Verde
    Modo.EM_CORRIDA -> PwCores.Ouro
}

private fun descricaoDo(m: Modo, economia: Boolean): String = when (m) {
    Modo.OFFLINE -> "Fora de serviço. Nenhuma localização é enviada."
    Modo.DISPONIVEL -> "Aguardando corridas. Sinal leve a cada 3 min ou a cada 300 m."
    Modo.EM_CORRIDA -> "Rastreamento ao vivo a cada ${Intervalos.corridaMs(economia) / 1000} s."
}

private val formatoHora = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR"))
private fun hora(t: Long): String = formatoHora.format(Date(t))

private fun ha(agora: Long, t: Long): String {
    val s = ((agora - t) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "há ${s}s"
        s < 3600 -> "há ${s / 60} min"
        else -> "há ${s / 3600} h"
    }
}
