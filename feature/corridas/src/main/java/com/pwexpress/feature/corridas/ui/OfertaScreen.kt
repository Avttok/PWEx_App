package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCores
import com.pwexpress.core.designsystem.PwRotulo
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
    val scope = rememberCoroutineScope()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val total = 30_000f
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

    val c = oferta.corrida
    Column(
        modifier
            .fillMaxSize()
            // qualquer toque silencia o alarme (a oferta continua na tela)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); Alarme.pararOferta(ctx) } }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NOVA CORRIDA", color = PwCores.Ouro, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
                Text("#${c.numero}" + (c.empresaNome?.let { " · $it" } ?: ""), color = PwCores.Cinza)
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { restante / total },
                    modifier = Modifier.size(64.dp),
                    color = if (restante < 10_000) PwCores.Vermelho else PwCores.Ouro,
                    trackColor = PwCores.Borda,
                    strokeWidth = 5.dp,
                )
                Text("${(restante + 999) / 1000}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PwCartao(destaque = true) {
                Text(moeda(c.valor), fontSize = 40.sp, fontWeight = FontWeight.Black, color = PwCores.Ouro)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Info("Até a coleta", km(oferta.distanciaM))
                    Info("Percurso", km(c.distanciaM))
                    Info("Tempo", minutos(c.duracaoS))
                }
            }
            PwCartao {
                PwRotulo("Trajeto")
                Spacer(Modifier.height(8.dp))
                c.paradas.forEach { p ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            if (p.tipo == TipoParada.COLETA) "COLETA" else "ENTREGA",
                            color = if (p.tipo == TipoParada.COLETA) PwCores.Ouro else PwCores.Branco,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(76.dp),
                        )
                        Text(p.endereco, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            val exigencias = listOfNotNull(
                "Nome".takeIf { c.exigeNome }, "Foto".takeIf { c.exigeFoto },
                "Assinatura".takeIf { c.exigeAssinatura }, "Código".takeIf { c.exigeCodigo },
            )
            if (exigencias.isNotEmpty() || c.observacoes != null) {
                PwCartao {
                    if (exigencias.isNotEmpty()) Text("Comprovante: ${exigencias.joinToString(", ")}")
                    c.observacoes?.let { Text(it, color = PwCores.Cinza) }
                }
            }
        }

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { responder(false) },
                enabled = !enviando,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Recusar", color = PwCores.Branco, fontSize = 18.sp) }
            Button(
                onClick = { responder(true) },
                enabled = !enviando,
                modifier = Modifier.weight(1.6f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PwCores.Ouro, contentColor = PwCores.Preto),
            ) { Text(if (enviando) "Enviando…" else "ACEITAR", fontWeight = FontWeight.Black, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun Info(rotulo: String, valor: String) {
    Column {
        PwRotulo(rotulo)
        Text(valor, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
internal fun CarregandoOferta() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = PwCores.Ouro)
        Spacer(Modifier.height(16.dp))
        Text("Carregando a oferta…", color = PwCores.Cinza)
    }
}

@Composable
internal fun Rodape(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth())
}
