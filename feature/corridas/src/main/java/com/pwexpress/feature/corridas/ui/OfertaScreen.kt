package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLogo
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.Oferta
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.corridas.push.Alarme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Oferta de corrida com contador. Usada dentro do app e na tela cheia. */
@Composable
fun OfertaScreen(oferta: Oferta, onFim: (aceitou: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val scope = rememberCoroutineScope()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val total = remember(oferta.id) { (oferta.prazoMillis - System.currentTimeMillis()).coerceAtLeast(1).toFloat() }
    val restante = (oferta.prazoMillis - agora).coerceAtLeast(0)

    LaunchedEffect(oferta.id) {
        while (System.currentTimeMillis() < oferta.prazoMillis) {
            agora = System.currentTimeMillis()
            delay(200)
        }
        agora = System.currentTimeMillis()
        Alarme.pararOferta(ctx)
        Corridas.descartarOferta(oferta.id)
        onFim(false)
    }

    fun responder(aceitar: Boolean) {
        if (enviando) return
        enviando = true
        erro = null
        Alarme.pararOferta(ctx)
        scope.launch {
            Corridas.responder(aceitar)
                .onSuccess { onFim(aceitar) }
                .onFailure { erro = it.message; enviando = false }
        }
    }

    val corrida = oferta.corrida
    Column(
        modifier
            .fillMaxSize()
            // qualquer toque silencia o alarme (a oferta continua na tela)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); Alarme.pararOferta(ctx) } }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwLogo(tamanho = 52, soSelo = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NOVA CORRIDA", style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.5.sp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${corrida.numero}" + (corrida.empresaNome?.let { " · $it" } ?: ""), color = c.texto2, fontFamily = Archivo)
                    if (corrida.teste) {
                        Spacer(Modifier.width(8.dp))
                        PwSelo("TESTE", c.violeta, icone = PwIcones.FlaskConical)
                    }
                }
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { restante / total },
                    modifier = Modifier.size(68.dp),
                    color = if (restante < 10_000) c.vermelho else c.ouro,
                    trackColor = c.borda,
                    strokeWidth = 6.dp,
                )
                Text("${(restante + 999) / 1000}", fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = ArchivoLargo, color = c.texto)
            }
        }

        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PwCartao(destaque = true) {
                PwRotulo("Você recebe")
                PwValor(moeda(corrida.valorMotoboy), tamanhoMax = 46)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Info("Até a coleta", km(oferta.distanciaM))
                    Info("Percurso", km(corrida.distanciaM))
                    Info("Tempo", minutos(corrida.duracaoS))
                }
            }
            PwCartao {
                PwRotulo("Trajeto")
                Spacer(Modifier.height(8.dp))
                corrida.paradas.forEach { p ->
                    val coleta = p.tipo == TipoParada.COLETA
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            if (coleta) PwIcones.ArrowUpFromLine else PwIcones.ArrowDownToLine, null,
                            tint = if (coleta) c.ouro else c.texto, modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (coleta) "COLETA" else "ENTREGA", color = if (coleta) c.ouro else c.texto2, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = Archivo)
                            Text(p.endereco, style = MaterialTheme.typography.bodyMedium, color = c.texto)
                        }
                    }
                }
            }
            val exigencias = listOfNotNull(
                "Nome".takeIf { corrida.exigeNome }, "Foto".takeIf { corrida.exigeFoto },
                "Assinatura".takeIf { corrida.exigeAssinatura }, "Código".takeIf { corrida.exigeCodigo },
            )
            if (exigencias.isNotEmpty() || corrida.observacoes != null || corrida.exigirNoLocal) {
                PwCartao {
                    if (exigencias.isNotEmpty()) Linha(PwIcones.PackageCheck, "Comprovante: ${exigencias.joinToString(", ")}")
                    if (corrida.exigirNoLocal) Linha(PwIcones.MapPinned, "Etapas só no local (até ${corrida.raioChegadaM} m)")
                    corrida.observacoes?.let { Linha(PwIcones.Pencil, it) }
                }
            }
        }

        erro?.let { Text(it, color = c.vermelho, modifier = Modifier.padding(vertical = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PwBotaoVidro("Recusar", onClick = { responder(false) }, habilitado = !enviando, icone = PwIcones.X, modifier = Modifier.weight(1f).height(64.dp))
            PwBotaoOuro(
                if (enviando) "Enviando…" else "ACEITAR",
                onClick = { responder(true) },
                habilitado = !enviando,
                icone = PwIcones.Check,
                altura = 64.dp,
                tamanhoTexto = 20,
                modifier = Modifier.weight(1.7f),
            )
        }
    }
}

@Composable
private fun Info(rotulo: String, valor: String) {
    Column {
        PwRotulo(rotulo)
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = ArchivoLargo, color = Pw.cores.texto)
    }
}

@Composable
private fun Linha(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = Pw.cores.ouro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, color = Pw.cores.texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun CarregandoOferta() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        PwLogo(tamanho = 120, soSelo = true)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(color = Pw.cores.ouro)
        Spacer(Modifier.height(16.dp))
        Text("Carregando a oferta…", color = Pw.cores.texto2)
    }
}
