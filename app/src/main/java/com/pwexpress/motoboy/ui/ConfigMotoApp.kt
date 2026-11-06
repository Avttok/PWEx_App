package com.pwexpress.motoboy.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabecalho
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.PwSeletorMapa
import com.pwexpress.core.mapa.coordenadaValida
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.PainelAparelho
import com.pwexpress.feature.tracking.ui.PainelEnvio
import kotlinx.coroutines.launch

private const val RAIO_MAX = 100f

/** Configurações do MotoApp (menu da foto): raio de disponibilidade, aparelho, corridas novas e envio de localização. */
@Composable
fun ConfigMotoApp(onVoltar: () -> Unit, modifier: Modifier = Modifier) {
    PwPagina(modifier, padding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)) {
        PwCabecalho("Configurações", onVoltar, subtitulo = "Raio, aparelho e GPS")
        CartaoRaio()
        CartaoPix()
        PainelAparelho()
        CartaoOfertas()
        PainelEnvio()
    }
}

/** Até onde o motoboy aceita ir buscar uma corrida, desenhado no mapa. */
@Composable
private fun CartaoRaio() {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val info by Tracking.info.collectAsState()
    val escopo = rememberCoroutineScope()
    var km by remember(perfil?.raioKm) { mutableFloatStateOf((perfil?.raioKm ?: 0.0).toFloat()) }
    var fixo by remember(perfil?.raioModo) { mutableStateOf(perfil?.raioModo == "fixo") }
    var ponto by remember(perfil?.raioLat, perfil?.raioLng) {
        mutableStateOf(perfil?.raioLat?.let { la -> perfil?.raioLng?.let { lo -> la to lo } })
    }
    var escolhendo by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    val eu = info.ultimaPosicao?.takeIf { coordenadaValida(it.first, it.second) }
    val centro = if (fixo) ponto else eu
    val mudou = km.toDouble() != (perfil?.raioKm ?: 0.0) || fixo != (perfil?.raioModo == "fixo") ||
        (fixo && ponto != perfil?.raioLat?.let { la -> perfil?.raioLng?.let { lo -> la to lo } })

    PwCartao {
        PwRotulo("Raio de disponibilidade")
        Spacer(Modifier.height(4.dp))
        Text(
            if (km < 1f) "Sem limite" else "Até ${km.toInt()} km",
            style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 26.sp), color = c.texto,
        )
        Text(
            if (km < 1f) "Você recebe corridas de qualquer distância." else "Corridas com coleta mais longe que isso não chegam para você (a não ser as urgentes).",
            color = c.texto2, fontSize = 13.sp, fontFamily = Archivo,
        )
        Slider(
            value = km, onValueChange = { km = it.toInt().toFloat() }, valueRange = 0f..RAIO_MAX,
            colors = SliderDefaults.colors(thumbColor = c.ouro, activeTrackColor = c.ouro, inactiveTrackColor = c.bordaForte),
        )
        PwSegmentado(listOf("De onde eu estiver" to PwIcones.LocateFixed, "Ponto fixo" to PwIcones.MapPin), if (fixo) 1 else 0, { fixo = it == 1 })
        Spacer(Modifier.height(10.dp))
        val forma = RoundedCornerShape(18.dp)
        Box(Modifier.fillMaxWidth().height(220.dp).clip(forma).border(1.dp, c.borda, forma)) {
            PwMapa(
                paradas = emptyList(), rota = null, minhaPosicao = eu, modifier = Modifier.fillMaxWidth().height(220.dp),
                marcadores = listOfNotNull(if (fixo) ponto?.let { MarcadorMapa("base", it.first, it.second, "#FFFFFF", "Base", pino = false) } else null),
                circulo = centro?.takeIf { km >= 1f }?.let { it to km * 1000.0 },
                enquadrar = "${centro?.first},${centro?.second},${km.toInt()}",
            )
        }
        if (centro == null) {
            Spacer(Modifier.height(6.dp))
            Text(if (fixo) "Escolha o ponto no mapa." else "Fique disponível para o mapa saber onde você está.", color = c.texto3, fontSize = 12.5.sp)
        }
        if (fixo) {
            Spacer(Modifier.height(8.dp))
            PwBotaoVidro(if (ponto == null) "Escolher o ponto no mapa" else "Mudar o ponto", onClick = { escolhendo = true }, icone = PwIcones.MapPinned, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))
        PwBotaoOuro(
            if (salvando) "Salvando…" else "Salvar raio", icone = PwIcones.Check, altura = 52.dp, habilitado = mudou && !salvando && !(fixo && ponto == null),
            onClick = {
                salvando = true
                escopo.launch {
                    Perfis.definirRaio(km.toDouble().takeIf { it >= 1 }, if (fixo) "fixo" else "posicao", ponto?.first, ponto?.second)
                        .onSuccess { PwToast.mostrar(if (km < 1f) "Você recebe corridas de qualquer distância." else "Até ${km.toInt()} km ${if (fixo) "do ponto fixo" else "de onde você estiver"}.", titulo = "Raio salvo") }
                        .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
                    salvando = false
                }
            },
        )
    }
    if (escolhendo) {
        PwSeletorMapa(ponto ?: eu, onFechar = { escolhendo = false }, titulo = "Ponto fixo do raio") { la, lo ->
            escolhendo = false
            ponto = la to lo
        }
    }
}

private val TIPOS_PIX = listOf("cpf" to "CPF", "cnpj" to "CNPJ", "telefone" to "Celular", "email" to "E-mail", "aleatoria" to "Aleatória")

/** Chave PIX para onde os ganhos das corridas são repassados. */
@Composable
private fun CartaoPix() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var carregada by remember { mutableStateOf<com.pwexpress.core.perfil.ChavePix?>(null) }
    var tipo by remember { mutableStateOf("cpf") }
    var chave by remember { mutableStateOf("") }
    var titular by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        Perfis.minhaChavePix()?.let { carregada = it; tipo = it.tipo; chave = it.chave; titular = it.titular }
        if (titular.isBlank()) titular = Perfis.perfil.value?.nome.orEmpty()
    }
    val mudou = carregada?.let { it.tipo != tipo || it.chave != chave.trim() || it.titular != titular.trim() } ?: chave.isNotBlank()
    PwCartao {
        PwRotulo("Chave PIX para receber")
        Spacer(Modifier.height(4.dp))
        Text(
            if (carregada == null) "Cadastre a chave para onde seus ganhos vão ser repassados quando a corrida terminar." else "Seus ganhos são repassados para esta chave.",
            color = c.texto2, fontSize = 13.sp, fontFamily = Archivo,
        )
        Spacer(Modifier.height(10.dp))
        com.pwexpress.core.designsystem.PwFiltros(TIPOS_PIX.map { it.second to 0 }, TIPOS_PIX.indexOfFirst { it.first == tipo }.coerceAtLeast(0), { tipo = TIPOS_PIX[it].first })
        Spacer(Modifier.height(10.dp))
        com.pwexpress.core.designsystem.PwCampo(chave, { chave = it }, "Chave", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        com.pwexpress.core.designsystem.PwCampo(titular, { titular = it }, "Nome do titular", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        PwBotaoOuro(
            if (salvando) "Salvando…" else "Salvar chave PIX", icone = PwIcones.Check, altura = 52.dp, habilitado = mudou && !salvando && chave.isNotBlank(),
            onClick = {
                salvando = true
                escopo.launch {
                    Perfis.definirChavePix(tipo, chave.trim(), titular.trim())
                        .onSuccess { carregada = com.pwexpress.core.perfil.ChavePix(tipo, chave.trim(), titular.trim()); PwToast.mostrar("Seus repasses vão para esta chave.", titulo = "Chave PIX salva") }
                        .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
                    salvando = false
                }
            },
        )
    }
}

/** Texto curto do raio para o atalho do Início. */
fun textoRaio(km: Double?, modo: String): String = if (km == null || km < 1) "Sem raio" else "Raio ${km.toInt()} km" + if (modo == "fixo") " · base" else ""
