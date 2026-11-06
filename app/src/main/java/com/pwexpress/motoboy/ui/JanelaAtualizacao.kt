package com.pwexpress.motoboy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwSeloLogo
import com.pwexpress.core.update.Atualizador
import com.pwexpress.core.update.EstadoAtualizacao

/** Janela de atualização: versão nova, notas, download com progresso e instalação. */
@Composable
fun JanelaAtualizacao() {
    val aberta by Atualizador.janela.collectAsState()
    val estado by Atualizador.estado.collectAsState()
    if (!aberta) return
    val c = Pw.cores
    val ctx = LocalContext.current
    val baixando = estado is EstadoAtualizacao.Baixando || estado is EstadoAtualizacao.Instalando
    Dialog(onDismissRequest = { if (!baixando) Atualizador.fecharJanela() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = !baixando)) {
        val forma = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(18.dp).fillMaxWidth().clip(forma).background(c.fundo2).border(1.dp, c.bordaForte, forma).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val fracao = (estado as? EstadoAtualizacao.Baixando)?.fracao ?: if (estado is EstadoAtualizacao.Instalando) 1f else 0f
            val anim by animateFloatAsState(fracao, label = "progresso")
            Box(contentAlignment = Alignment.Center) {
                if (baixando) CircularProgressIndicator(progress = { anim }, color = c.ouro, trackColor = c.borda, strokeWidth = 5.dp, modifier = Modifier.size(132.dp))
                PwSeloLogo(if (baixando) 104.dp else 120.dp)
            }
            when (val e = estado) {
                is EstadoAtualizacao.Disponivel -> {
                    Titulo("Nova versão disponível")
                    Text("Build ${e.versao.build} · você tem a ${Atualizador.versaoInstalada}", color = c.texto2, fontFamily = Archivo)
                    Notas(e.versao.notas)
                    Instalar(ctx)
                    PwBotaoOuro("Atualizar agora", onClick = { Atualizador.baixar() }, icone = PwIcones.Download, altura = 58.dp)
                    PwBotaoVidro("Depois", onClick = { Atualizador.fecharJanela() }, modifier = Modifier.fillMaxWidth())
                }
                is EstadoAtualizacao.Baixando -> {
                    Titulo("Baixando…")
                    val mb = { b: Long -> "%.1f".format(b / 1_048_576.0).replace('.', ',') }
                    Text("${mb(e.baixado)} de ${mb(e.total)} MB · ${(e.fracao * 100).toInt()}%", color = c.texto2, fontFamily = Archivo)
                    Barra(anim)
                    Text("Pode deixar o app aberto. O Android vai pedir para confirmar a instalação.", color = c.texto3, fontSize = 13.sp, textAlign = TextAlign.Center)
                    PwBotaoVidro("Cancelar", onClick = { Atualizador.cancelarDownload() }, modifier = Modifier.fillMaxWidth())
                }
                is EstadoAtualizacao.Instalando -> {
                    Titulo("Instalando…")
                    Text("Confirme no aviso do Android. O app reinicia sozinho na versão ${e.versao.build}.", color = c.texto2, textAlign = TextAlign.Center)
                    Barra(1f)
                }
                is EstadoAtualizacao.Erro -> {
                    Titulo("Não deu certo")
                    PwErro(e.mensagem)
                    Instalar(ctx)
                    if (e.versao != null) PwBotaoOuro("Tentar de novo", onClick = { Atualizador.baixar() }, icone = PwIcones.RefreshCw)
                    PwBotaoVidro("Fechar", onClick = { Atualizador.fecharJanela() }, modifier = Modifier.fillMaxWidth())
                }
                EstadoAtualizacao.Verificando -> { Titulo("Procurando atualização…"); Barra(null) }
                EstadoAtualizacao.EmDia -> {
                    Titulo("Você está em dia")
                    Text("Esta é a versão mais nova (build ${Atualizador.versaoInstalada}).", color = c.texto2, textAlign = TextAlign.Center)
                    PwBotaoOuro("Ótimo", onClick = { Atualizador.fecharJanela() }, icone = PwIcones.Check)
                }
                EstadoAtualizacao.Ocioso -> PwBotaoVidro("Fechar", onClick = { Atualizador.fecharJanela() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Titulo(t: String) {
    Text(t, style = TextStyle(brush = Pw.cores.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 24.sp), textAlign = TextAlign.Center)
}

@Composable
private fun Notas(notas: String) {
    if (notas.isBlank()) return
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text("O que há de novo", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(notas, color = c.texto, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun Barra(fracao: Float?) {
    val c = Pw.cores
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.borda)) {
        if (fracao != null) Box(Modifier.fillMaxWidth(fracao.coerceIn(0.02f, 1f)).height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.gradOuro))
    }
}

/** Sem a permissão de instalar, mostra o atalho para liberar. */
@Composable
private fun Instalar(ctx: android.content.Context) {
    if (Atualizador.podeInstalar(ctx)) return
    val c = Pw.cores
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(PwIcones.ShieldCheck, null, tint = c.ouro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Libere “Instalar apps desconhecidos” para o PW Express.", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
    PwBotaoVidro("Abrir permissão", onClick = { Atualizador.abrirPermissaoInstalar(ctx) }, icone = PwIcones.Settings2, modifier = Modifier.fillMaxWidth())
}
